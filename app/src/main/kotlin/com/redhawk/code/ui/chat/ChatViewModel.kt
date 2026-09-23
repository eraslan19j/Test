package com.redhawk.code.ui.chat

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redhawk.code.agent.AgentTools
import com.redhawk.code.agent.ToolIntentParser
import com.redhawk.code.agent.PermissionCatalog
import com.redhawk.code.agent.PermissionProfile
import com.redhawk.code.agent.ProjectFiles
import com.redhawk.code.data.db.MessageEntity
import com.redhawk.code.data.db.ProviderEntity
import com.redhawk.code.data.prefs.PrefsStore
import com.redhawk.code.data.repo.ChatRepository
import com.redhawk.code.data.repo.ProviderRepository
import com.redhawk.code.data.skills.SkillCatalog
import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.LlmProvider
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.Role
import com.redhawk.code.llm.model.ToolCall
import com.redhawk.code.llm.provider.ProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Yazma öncesi kullanıcı onayı bekleyen araç çağrısı */
data class ToolApproval(val call: ToolCall, val preview: String)

data class ChatUiState(
    val chatId: String? = null,
    val chatTitle: String = "",
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val modelLabel: String = "sağlayıcı seç",
    val modelReady: Boolean = false,
    val currentProvider: ProviderEntity? = null,
    val enabledSkills: Set<String> = emptySet(),
    val agentMode: Boolean = false,
    val pendingApproval: ToolApproval? = null,
    val projectLabel: String = ""
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "ChatVM"
    private val prefs = PrefsStore(app)
    private val repo = ChatRepository(app)
    private val providerRepo = ProviderRepository(app)

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var provider: LlmProvider? = null
    private var generationJob: Job? = null
    private var messageCollector: Job? = null
    private var persistJob: Job? = null
    private var tickJob: Job? = null
    private var stopRequested = false
    private var approvalGate: CompletableDeferred<Boolean>? = null
    // Yanıt dili Türkçe ise İngilizce cümleler balondan panele taşınır (garanti)
    private var turkishOnly = true

    // Streaming tamponu: ham metin birikir, her UI güncellemesinde tamamı parse edilir.
    // (Parça-parça tag avı yerine full-reparse: bölünmüş tag bug'larını öldürür.)
    private var streamingRaw = StringBuilder()
    private var lastParsedThinking = ""
    private var lastParsedResponse = ""
    private var streamingMsgId: String? = null
    private var streamingStartedAt = 0L
    private var thinkingEndMs = 0L
    private var wasThinking = false

    private val uiTick = Channel<Unit>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // Ajan izinleri (Ajan izinleri ekranı okur/yazar)
    val agentProfile: StateFlow<PermissionProfile> =
        prefs.agentProfile.map { s ->
            runCatching { PermissionProfile.valueOf(s) }
                .getOrDefault(PermissionProfile.STANDARD)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PermissionProfile.STANDARD)
    val agentPerms: StateFlow<Set<String>> = prefs.agentPerms
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            setOf("read", "write", "move", "terminal"))
    val agentChmod: StateFlow<String> = prefs.agentChmod
        .stateIn(viewModelScope, SharingStarted.Eagerly, "755")

    fun saveAgentPerms(p: PermissionProfile, e: Set<String>, c: String) =
        viewModelScope.launch {
            prefs.setAgentProfile(p.name)
            prefs.setAgentPerms(e)
            prefs.setAgentChmod(c.ifBlank { "755" })
        }

    val chats = repo.observeChats()

    init {
        viewModelScope.launch {
            try {
                for (u in uiTick) { delay(60); runCatching { flushStreamingToUi() } }
            } catch (t: Throwable) { Log.e(TAG, "tick err", t) }
        }
        viewModelScope.launch {
            runCatching {
                val id = prefs.selectedProviderId.first()
                if (id != null) {
                    val p = providerRepo.get(id)
                    if (p != null) switchProvider(p)
                }
            }
        }
    }

    fun switchProvider(p: ProviderEntity) {
        provider = ProviderFactory.create(p)
        _state.update {
            it.copy(
                currentProvider = p,
                modelLabel = "${p.displayName} · ${p.model}",
                modelReady = true,
                error = null
            )
        }
        viewModelScope.launch { prefs.setSelectedProvider(p.id) }
    }

    /** Sadece model ID'sini değiştir, sağlayıcıyı yeniden kullan */
    fun updateModel(newModel: String) = viewModelScope.launch {
        val cur = _state.value.currentProvider ?: return@launch
        runCatching {
            providerRepo.updateModel(cur.id, newModel)
            val updated = providerRepo.get(cur.id) ?: return@launch
            provider = ProviderFactory.create(updated)
            _state.update {
                it.copy(
                    currentProvider = updated,
                    modelLabel = "${updated.displayName} · ${updated.model}"
                )
            }
        }
    }

    fun toggleSkill(id: String) = _state.update { st ->
        val s = st.enabledSkills.toMutableSet()
        if (!s.add(id)) s.remove(id)
        st.copy(enabledSkills = s)
    }

    fun ensureActiveChat() = viewModelScope.launch {
        runCatching {
            if (_state.value.chatId == null) {
                val c = repo.createChat("cloud", "Yeni sohbet")
                attachChat(c.id)
            }
        }
    }

    fun openChat(chatId: String) = viewModelScope.launch { runCatching { attachChat(chatId) } }

    private fun attachChat(chatId: String) {
        messageCollector?.cancel()
        _state.update { it.copy(chatId = chatId, messages = emptyList(), error = null, chatTitle = "") }
        messageCollector = viewModelScope.launch {
            runCatching {
                val chat = repo.getChat(chatId)
                if (chat != null) _state.update {
                    it.copy(
                        chatTitle = chat.title,
                        projectLabel = ProjectFiles.displayName(getApplication(), chat.projectUri)
                    )
                }
                repo.observeMessages(chatId).collect { list ->
                    if (_state.value.isStreaming && streamingMsgId != null) return@collect
                    _state.update { it.copy(messages = list.map { m -> m.toUi() }) }
                }
            }
        }
    }

    private fun MessageEntity.toUi(): UiMessage {
        val parsed = ThinkingParser.parseStored(content)
        // Güvenlik ağı: guardsız yazılmış eski/kayıtlı mesajları da temizle
        val think: String
        val resp: String
        if (!isUser && turkishOnly) {
            val g = TurkishGuard.enforce(parsed.thinking, parsed.response)
            think = g.thinking
            resp = g.response
        } else {
            think = parsed.thinking
            resp = parsed.response
        }
        return UiMessage(
            id = id, isUser = isUser, content = resp, createdAt = createdAt,
            thinking = think, error = error
        )
    }

    fun newChat() = viewModelScope.launch {
        runCatching {
            val c = repo.createChat("cloud", "Yeni sohbet")
            attachChat(c.id)
        }
    }
    fun renameChat(id: String, title: String) = viewModelScope.launch {
        runCatching { repo.renameChat(id, title)
            if (_state.value.chatId == id) _state.update { it.copy(chatTitle = title) } }
    }
    fun deleteChat(id: String) = viewModelScope.launch {
        runCatching { repo.deleteChat(id)
            if (_state.value.chatId == id) _state.update { it.copy(chatId = null, messages = emptyList()) } }
    }
    fun pinChat(id: String, p: Boolean) = viewModelScope.launch { runCatching { repo.setPinned(id, p) } }
    fun starChat(id: String, s: Boolean) = viewModelScope.launch { runCatching { repo.setStarred(id, s) } }
    fun archiveChat(id: String, a: Boolean) = viewModelScope.launch { runCatching { repo.setArchived(id, a) } }

    fun onInputChange(s: String) = _state.update { it.copy(input = s) }
    fun setInput(s: String) = _state.update { it.copy(input = s) }

    // ---------- Ajan modu ----------

    fun toggleAgent() = _state.update { it.copy(agentMode = !it.agentMode) }

    /** Onay diyaloğundan cevap geldi */
    fun approveTool(allow: Boolean) {
        approvalGate?.complete(allow)
        approvalGate = null
    }

    /** SAF ile seçilen proje klasörünü bu sohbete bağla */
    fun setProjectFolder(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        var id = _state.value.chatId
        if (id == null) {
            val c = repo.createChat("cloud", "Yeni sohbet")
            attachChat(c.id)
            delay(150)
            id = c.id
        }
        val chatId = id ?: return@launch
        runCatching { repo.setProject(chatId, uri.toString()) }
        _state.update { it.copy(projectLabel = ProjectFiles.displayName(app, uri.toString())) }
    }

    /** Dosya ekranı için aktif sohbetin proje yolu (null = uygulama deposu) */
    suspend fun activeProjectUri(): String? {
        val id = _state.value.chatId ?: return null
        return runCatching { repo.getChat(id)?.projectUri }.getOrNull()
    }

    private suspend fun runAgentTool(chatId: String, tc: ToolCall): String {
        val app = getApplication<Application>()
        val projectUri = runCatching { repo.getChat(chatId)?.projectUri }.getOrNull()
        // İzin denetimi (Ajan izinleri ekranı — gerçekten çalışır)
        val need = PermissionCatalog.permissionFor(tc.name)
        if (need != null) {
            val allowed = runCatching { prefs.agentPerms.first() }
                .getOrDefault(setOf("read", "write", "move", "terminal"))
            if (need !in allowed) {
                return "Kullanıcı bu araca izin vermedi ('$need' kapalı). " +
                    "Ajan izinleri ekranından açılabilir. Israr etme, alternatif öner."
            }
        }
        // 777 = soru sormadan otomatik devam
        val noAsk = runCatching { prefs.agentChmod.first() }.getOrDefault("755") ==
            PermissionCatalog.FULL_AUTO_CHMOD
        if (AgentTools.needsApproval(tc.name) && !noAsk) {
            val previewText = AgentTools.preview(app, projectUri, tc.name, tc.argumentsJson)
            val gate = CompletableDeferred<Boolean>()
            approvalGate = gate
            _state.update {
                it.copy(pendingApproval = ToolApproval(tc, previewText))
            }
            val ok = try {
                gate.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                false
            }
            approvalGate = null
            _state.update { it.copy(pendingApproval = null) }
            if (!ok) return "Kullanıcı bu dosya işlemini REDDETTİ. Israr etme; alternatif öner veya vazgeç."
        }
        return withContext(Dispatchers.IO) {
            runCatching { AgentTools.execute(app, projectUri, tc.name, tc.argumentsJson) }
                .getOrElse { "HATA: araç çalışamadı: ${it.message}" }
        }
    }

    /** Akış sırasında geçen süreyi 100ms'de bir UI'a yazar (panel sayacı canlı kalır) */
    private fun tickStreamingTotal() {
        val id = streamingMsgId ?: return
        val now = System.currentTimeMillis()
        _state.update { st ->
            val list = st.messages.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(totalMs = now - streamingStartedAt)
            st.copy(messages = list)
        }
    }

    private fun updateToolLabel(label: String?) {
        val id = streamingMsgId ?: return
        _state.update { st ->
            val list = st.messages.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(toolLabel = label)
            st.copy(messages = list)
        }
    }

    // ---------- Kota bitince otomatik sağlayıcı geçişi ----------

    private var quotaFailed = false
    private var failoverDepth = 0

    /** Mevcut hariç ilk yedek sağlayıcı (ücretsizler öncelikli) */
    private suspend fun findFailoverProvider(): ProviderEntity? = runCatching {
        val all = providerRepo.observeAll().first()
        val cur = _state.value.currentProvider?.id
        (all.filter { it.isFree } + all.filter { !it.isFree }).firstOrNull { it.id != cur }
    }.getOrNull()

    /**
     * Üretimi GERÇEKTEN durdurur: job iptal edilir, SSE bağlantısı
     * awaitClose içinde kapatılır, yarım kalan metin DB'ye kaydedilir.
     */
    fun stop() {
        stopRequested = true
        generationJob?.cancel()
    }

    fun clearChat() = viewModelScope.launch {
        runCatching { _state.value.chatId?.let { repo.clearMessages(it) } }
    }

    fun send(): Job = viewModelScope.launch {
        try {
            val text = _state.value.input.trim()
            if (text.isEmpty() || _state.value.isStreaming) return@launch

            val p = provider ?: run {
                _state.update { it.copy(error = "Önce sağlayıcı seç") }
                return@launch
            }
            val modelId = _state.value.currentProvider?.model ?: "gpt-4o-mini"

            if (_state.value.chatId == null) {
                val c = repo.createChat(modelId, "Yeni sohbet")
                attachChat(c.id)
                delay(150)
            }
            val chatId = _state.value.chatId ?: return@launch

            val userMsg = MessageEntity(chatId = chatId, isUser = true, content = text)
            val assistantMsg = MessageEntity(chatId = chatId, isUser = false, content = "")
            val now = System.currentTimeMillis()

            streamingMsgId = assistantMsg.id
            streamingRaw = StringBuilder()
            lastParsedThinking = ""
            lastParsedResponse = ""
            streamingStartedAt = now
            thinkingEndMs = 0L
            wasThinking = false
            stopRequested = false
            quotaFailed = false
            if (failoverDepth >= 3) failoverDepth = 0

            _state.update { st ->
                st.copy(
                    input = "", isStreaming = true, error = null,
                    messages = st.messages +
                        UiMessage(userMsg.id, true, text, now) +
                        UiMessage(assistantMsg.id, false, "", now,
                            streaming = true, thinkingStreaming = false, inThinking = false)
                )
            }

            repo.addMessage(userMsg)
            repo.addMessage(assistantMsg)

            // Otomatik başlık (ilk mesajdan)
            runCatching {
                val t = repo.autoTitle(chatId)
                if (t != null) _state.update { it.copy(chatTitle = t) }
            }

            generationJob = viewModelScope.launch {
                try {
                    val agentOn = _state.value.agentMode
                    val sysPromptBase = prefs.systemPrompt.first()
                    val langPref = runCatching { prefs.responseLang.first() }
                        .getOrDefault("tr")
                    // "auto" GERÇEKTEN otomatik: kullanıcının mesajından dili anla.
                    // (Eskiden auto = direktifsiz + guardsız = ham İngilizce demekti.)
                    val effLang = if (langPref == "auto") {
                        if (looksTurkish(text)) "tr" else "en"
                    } else langPref
                    turkishOnly = effLang == "tr"
                    val sysPrompt = buildSystemPrompt(sysPromptBase, agentOn, effLang)
                    // Ayarlardan: sıcaklık + maksimum token (gerçekten uygulanır)
                    val temp = prefs.temperature.first()
                    val maxTok = prefs.maxTokens.first()

                    val history = _state.value.messages
                        .filter { it.id != userMsg.id && it.id != assistantMsg.id && !it.streaming }
                        .takeLast(8).filter { it.content.isNotBlank() }
                        .map { ChatMessage(
                            role = if (it.isUser) Role.USER else Role.ASSISTANT,
                            content = it.content) }

                    val all = buildList {
                        add(ChatMessage(Role.SYSTEM, sysPrompt))
                        addAll(history)
                        add(ChatMessage(Role.USER, text))
                    }

                    persistJob = viewModelScope.launch {
                        try {
                            while (true) {
                                delay(600)
                                val id = streamingMsgId ?: break
                                val combined = ThinkingParser.mergeForDb(
                                    lastParsedThinking, lastParsedResponse)
                                if (combined.isNotEmpty()) {
                                    runCatching { repo.updateMessageContent(id, combined) }
                                }
                                if (!_state.value.isStreaming) break
                            }
                        } catch (_: Throwable) {}
                    }

                    // Canlı süre sayacı: ilk olay gelmese bile panel 0'da takılmaz
                    tickJob = viewModelScope.launch {
                        try {
                            while (true) {
                                delay(100)
                                tickStreamingTotal()
                            }
                        } catch (_: Throwable) {}
                    }

                    // ---- Ajan döngüsü: model araç çağırdıkça çalıştır, sonucu geri ver ----
                    val convo = all.toMutableList()
                    val tools = if (agentOn) AgentTools.specs else emptyList()
                    var rounds = 0
                    var aborted = false
                    var pendingTool: ToolCall? = null
                    var hadCalls = false
                    do {
                        pendingTool = null
                        val roundStart = streamingRaw.length
                        p.chat(convo, tools, modelId, temp, maxTok).collect { ev ->
                            if (stopRequested) return@collect
                            when (ev) {
                                is LlmEvent.TextDelta -> {
                                    streamingRaw.append(ev.text)
                                    uiTick.trySend(Unit)
                                }
                                is LlmEvent.ToolCallRequested -> pendingTool = ev.call
                                is LlmEvent.Error -> {
                                    _state.update { it.copy(error = ev.message) }
                                    aborted = true
                                    if (ev.quotaExceeded) quotaFailed = true
                                }
                                LlmEvent.Done -> {}
                            }
                        }
                        // Metin-içi araç niyeti: function-calling bilmeyen küçük
                        // modeller <tool> / name({...}) yazar — onu da çalıştır.
                        val roundText = streamingRaw.substring(roundStart)
                        val textIntents = if (agentOn && pendingTool == null &&
                            !aborted && !stopRequested
                        ) ToolIntentParser.extract(roundText) else emptyList()
                        val calls: List<ToolCall> = pendingTool?.let { listOf(it) }
                            ?: textIntents.mapIndexed { i, t ->
                                ToolCall("txt_${now}_${rounds}_$i", t.name, t.argsJson)
                            }
                        hadCalls = false
                        if (calls.isNotEmpty() && !aborted && !stopRequested &&
                            rounds < MAX_AGENT_ROUNDS
                        ) {
                            hadCalls = true
                            rounds++
                            val roundResp = ToolIntentParser.stripToolBlocks(
                                ThinkingParser.parseStreaming(roundText).response)
                            convo.add(ChatMessage(
                                Role.ASSISTANT, content = roundResp, toolCalls = calls))
                            for (tc in calls) {
                                if (stopRequested) break
                                updateToolLabel("⚙ ${tc.name} çalışıyor…")
                                val result = runAgentTool(chatId, tc)
                                updateToolLabel(null)
                                val clipped = if (result.length > 8000)
                                    result.take(8000) + "\n…(kesildi)"
                                else result
                                convo.add(ChatMessage(Role.TOOL, content = clipped,
                                    toolCallId = tc.id, toolName = tc.name))
                            }
                        }
                    } while (hadCalls && !aborted && !stopRequested && rounds < MAX_AGENT_ROUNDS)
                } catch (e: CancellationException) {
                    // Durdur butonu: hata değil, sessizce finally'e düş
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "gen err", t)
                    withContext(NonCancellable) {
                        runCatching { repo.updateMessageError(assistantMsg.id, t.message) }
                    }
                    _state.update { it.copy(error = t.message) }
                } finally {
                    persistJob?.cancel()
                    tickJob?.cancel()
                    // Yarıda kesildiyse onay diyaloğunu da kapat
                    approvalGate?.cancel()
                    approvalGate = null
                    _state.update { it.copy(pendingApproval = null) }
                    // Yarım kalan metni bile kaydet (iptal sonrası NonCancellable şart)
                    withContext(NonCancellable) {
                        flushStreamingToUi()
                        val finalCombined = ThinkingParser.mergeForDb(
                            lastParsedThinking, lastParsedResponse)
                        runCatching { repo.updateMessageContent(assistantMsg.id, finalCombined) }
                        runCatching { repo.touch(chatId) }
                        // Akış bitti ama metin yoksa sessiz "…" yerine net uyarı ver
                        if (finalCombined.isBlank() && !stopRequested && !quotaFailed &&
                            _state.value.error == null
                        ) {
                            _state.update { it.copy(error = "Model boş yanıt döndü. Başka model dene.") }
                        }
                    }

                    val total = System.currentTimeMillis() - streamingStartedAt
                    val thinkMs = if (thinkingEndMs > 0) thinkingEndMs - streamingStartedAt else 0L
                    val sid = streamingMsgId
                    _state.update { st ->
                        val list = st.messages.toMutableList()
                        val idx = list.indexOfFirst { it.id == sid }
                        if (idx >= 0) list[idx] = list[idx].copy(
                            streaming = false, thinkingStreaming = false, inThinking = false,
                            totalMs = total, thinkingMs = thinkMs)
                        st.copy(messages = list)
                    }
                    streamingMsgId = null
                    _state.update { it.copy(isStreaming = false) }

                    // Kota bittiyse: yedek sağlayıcıya geçip otomatik tekrar dene (max 3)
                    if (!quotaFailed || stopRequested) {
                        failoverDepth = 0
                    } else if (failoverDepth < 3) {
                        val next = withContext(NonCancellable) { findFailoverProvider() }
                        if (next != null) {
                            failoverDepth++
                            quotaFailed = false
                            switchProvider(next)
                            withContext(NonCancellable) {
                                runCatching { repo.deleteMessage(assistantMsg.id) }
                                runCatching { repo.deleteMessage(userMsg.id) }
                            }
                            _state.update { st ->
                                st.copy(
                                    messages = st.messages.filter {
                                        it.id != userMsg.id && it.id != assistantMsg.id
                                    },
                                    input = text,
                                    error = "Kota bitti → ${next.displayName} ile devam ediliyor…"
                                )
                            }
                            viewModelScope.launch { send() }
                        } else {
                            failoverDepth = 0
                        }
                    } else {
                        failoverDepth = 0
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "send err", t)
            _state.update { it.copy(error = t.message, isStreaming = false) }
        }
    }

    /**
     * Ham tamponu baştan parse edip UI'a yansıtır.
     * <think> etiketi yoksa her şey response'a düşer (Gemini/GPT doğru çalışır).
     */
    private fun flushStreamingToUi() {
        val id = streamingMsgId ?: return
        val parsed = ThinkingParser.parseStreaming(streamingRaw.toString())
        // <tool> blokları ekrana/DB'ye sızmasın; İngilizce cümleler
        // (Türkçe kipinde) balondan düşünme paneline taşınsın.
        var think = parsed.thinking
        var resp = ToolIntentParser.stripToolBlocks(parsed.response)
        if (turkishOnly) {
            val g = TurkishGuard.enforce(think, resp)
            think = g.thinking
            resp = g.response
        }
        lastParsedThinking = think
        lastParsedResponse = resp

        if (parsed.stillThinking) {
            wasThinking = true
        } else if (wasThinking && thinkingEndMs == 0L) {
            thinkingEndMs = System.currentTimeMillis()
        }

        val now = System.currentTimeMillis()
        val thinkMs = when {
            thinkingEndMs > 0 -> thinkingEndMs - streamingStartedAt
            parsed.stillThinking -> now - streamingStartedAt
            else -> 0L
        }

        _state.update { st ->
            val list = st.messages.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    content = resp,
                    thinking = think,
                    totalMs = now - streamingStartedAt,
                    thinkingMs = thinkMs,
                    streaming = true,
                    thinkingStreaming = parsed.stillThinking,
                    inThinking = parsed.stillThinking
                )
            }
            st.copy(messages = list)
        }
    }

    /** "auto" dil kipi: kullanıcının mesajı Türkçe mi? */
    private fun looksTurkish(s: String): Boolean {
        if (s.any { it in "çÇğĞıİöÖşŞüÜ" }) return true
        val padded = " " + s.lowercase().replace(Regex("[^a-zçğıöşü ]"), " ") + " "
        val hints = setOf(
            "merhaba", "selam", "evet", "hayir", "tamam", "lutfen", "nasil",
            "neden", "nicin", "nerede", "hangi", "hangisi", "bana", "beni",
            "bize", "bizi", "sana", "seni", "size", "sizi", "icin", "veya",
            "gibi", "kadar", "sonra", "once", "degil", "olarak", "cunku",
            "acaba", "nedir", "kimdir", "musun", "misin", "mısın", "müsün",
            "mudur", "midir", "mıdır", "müdür", "bunlar", "sunlar", "onlar",
            "kimse", "birsey", "birşey", "sey", "şey", "soyle", "boyle",
            "sunu", "bunu", "onu", "yapar", "verir", "soylar", "soylar",
            "yazar", "anlat", "acikla", "listele", "goster", "duzelt",
            "olustur", "calistir", "guncelle", "kaydet", "klasor", "dosya",
            "hata", "komut", "yap", "bul", "sil", "oku", "yaz", "ekle", "kod"
        )
        return hints.any { padded.contains(" $it ") }
    }

    private fun buildSystemPrompt(base: String, agent: Boolean, lang: String): String {
        val directive = when (lang) {
            "en" -> "Always respond in English. Never write Turkish."
            "auto" -> ""
            else -> "Her zaman Türkçe cevap ver. Asla İngilizce yazma. " +
                "İngilizce TEK CÜMLE bile yazma — cevabının her satırı Türkçe olacak. " +
                "Düşünme metnin bile Türkçe olsun. " +
                "Cevabına İngilizce giriş cümlesi ekleme; 'The user asks' gibi " +
                "kendi kendine konuşma. Doğrudan Türkçe cevaba başla."
        }
        var prompt = if (directive.isNotBlank()) "$base\n\n$directive" else base
        val skills = _state.value.enabledSkills
        if (skills.isNotEmpty()) {
            val prompts = skills.mapNotNull { SkillCatalog.byId(it)?.prompt }
            prompt += "\n\nAKTİF YETENEKLER:\n" + prompts.joinToString("\n---\n")
        }
        if (agent) prompt += "\n\n" + AGENT_PROMPT
        return prompt
    }

    companion object {
        const val MAX_AGENT_ROUNDS = 5
        const val AGENT_PROMPT =
            "AJAN MODU: Araçların var: list_files, read_file, write_file, delete_file, " +
            "move_file, chmod_file (dosyalar), " +
            "web_search ve fetch_url (internet). " +
            "ARAÇ ÇAĞIRMA FORMATI (function-calling yoksa bu bloğu aynen yaz): " +
            "<tool name=\"list_files\">{\"path\": \"\"}</tool> " +
            "Örnekler: <tool name=\"read_file\">{\"path\": \"Main.kt\"}</tool> " +
            "<tool name=\"web_search\">{\"query\": \"konu\"}</tool> " +
            "<tool name=\"write_file\">{\"path\": \"a.txt\", \"content\": \"...\"}</tool> " +
            "Araç bloğu dışında araç adı yazma; sonucu bekle, sonra Türkçe özetle. " +
            "Kullanıcı klasör veya dosya sorarsa SORU SORMA: önce list_files ile " +
            "köke bak, sonucu görmeden 'yapabilirim' deme. " +
            "run_command ile komut çalıştırabilirsin (ls, cat, grep, find, git " +
            "status/log/diff; salt-okunur, uygulama deposunda). " +
            "Kullanıcı kod/proje işi isterse önce list_files ile klasöre bak, " +
            "gerekirse read_file ile oku, sonucu write_file ile yaz. " +
            "Güncel bilgi, kütüphane dokümantasyonu veya hata çözümü gerekiyorsa " +
            "web_search ile ara, gerekirse fetch_url ile sayfayı oku. " +
            "write_file öncesi kullanıcıdan onay istenir, reddedilirse ısrar etme. " +
            "Yollar çalışma köküne göredir (örn: 'Main.kt', 'src/app.py'). " +
            "Türkçe konuş, açıklamaları kısa tut, yaptığın işlemleri maddelerle özetle."
    }
}
