package com.redhawk.code.ui.chat

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redhawk.code.agent.AgentTools
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
    private var stopRequested = false
    private var approvalGate: CompletableDeferred<Boolean>? = null

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
        return UiMessage(
            id = id, isUser = isUser, content = parsed.response, createdAt = createdAt,
            thinking = parsed.thinking, error = error
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

    private suspend fun runAgentTool(chatId: String, tc: ToolCall): String {
        val app = getApplication<Application>()
        val projectUri = runCatching { repo.getChat(chatId)?.projectUri }.getOrNull()
        if (AgentTools.needsApproval(tc.name)) {
            val gate = CompletableDeferred<Boolean>()
            approvalGate = gate
            _state.update {
                it.copy(pendingApproval = ToolApproval(tc, AgentTools.preview(tc.name, tc.argumentsJson)))
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
                    val sysPrompt = buildSystemPrompt(sysPromptBase, agentOn)
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

                    // ---- Ajan döngüsü: model araç çağırdıkça çalıştır, sonucu geri ver ----
                    val convo = all.toMutableList()
                    val tools = if (agentOn) AgentTools.specs else emptyList()
                    var rounds = 0
                    var aborted = false
                    var pendingTool: ToolCall? = null
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
                        val tc = pendingTool
                        if (tc != null && !aborted && !stopRequested && rounds < MAX_AGENT_ROUNDS) {
                            rounds++
                            val roundText = streamingRaw.substring(roundStart)
                            val roundResp = ThinkingParser.parseStreaming(roundText).response
                            convo.add(ChatMessage(Role.ASSISTANT, content = roundResp, toolCalls = listOf(tc)))
                            updateToolLabel("⚙ ${tc.name} çalışıyor…")
                            val result = runAgentTool(chatId, tc)
                            updateToolLabel(null)
                            val clipped = if (result.length > 8000) result.take(8000) + "\n…(kesildi)"
                            else result
                            convo.add(ChatMessage(Role.TOOL, content = clipped, toolCallId = tc.id, toolName = tc.name))
                        }
                    } while (pendingTool != null && !aborted && !stopRequested && rounds < MAX_AGENT_ROUNDS)
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
        lastParsedThinking = parsed.thinking
        lastParsedResponse = parsed.response

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
                    content = parsed.response,
                    thinking = parsed.thinking,
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

    private fun buildSystemPrompt(base: String, agent: Boolean): String {
        var prompt = base
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
            "AJAN MODU: Araçların var: list_files, read_file, write_file (dosyalar), " +
            "web_search ve fetch_url (internet). " +
            "Kullanıcı kod/proje işi isterse önce list_files ile klasöre bak, " +
            "gerekirse read_file ile oku, sonucu write_file ile yaz. " +
            "Güncel bilgi, kütüphane dokümantasyonu veya hata çözümü gerekiyorsa " +
            "web_search ile ara, gerekirse fetch_url ile sayfayı oku. " +
            "write_file öncesi kullanıcıdan onay istenir, reddedilirse ısrar etme. " +
            "Yollar çalışma köküne göredir (örn: 'Main.kt', 'src/app.py'). " +
            "Türkçe konuş, açıklamaları kısa tut, yaptığın işlemleri maddelerle özetle."
    }
}
