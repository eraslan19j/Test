package com.redhawk.code.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.redhawk.code.llm.provider.ProviderFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val enabledSkills: Set<String> = emptySet()
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

    private var streamingContent = StringBuilder()
    private var streamingThinking = StringBuilder()
    private var streamingMsgId: String? = null
    private var streamingStartedAt = 0L
    private var thinkingEndMs = 0L

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
                if (chat != null) _state.update { it.copy(chatTitle = chat.title) }
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
    fun stop() { stopRequested = true }
    fun clearChat() = viewModelScope.launch {
        runCatching { _state.value.chatId?.let { repo.clearMessages(it) } }
    }

    fun send() = viewModelScope.launch {
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

            if (_state.value.chatTitle == "Yeni sohbet") {
                val newTitle = text.take(40).replace("\n", " ")
                repo.renameChat(chatId, newTitle)
                _state.update { it.copy(chatTitle = newTitle) }
            }

            val userMsg = MessageEntity(chatId = chatId, isUser = true, content = text)
            val assistantMsg = MessageEntity(chatId = chatId, isUser = false, content = "")
            val now = System.currentTimeMillis()

            streamingMsgId = assistantMsg.id
            streamingContent = StringBuilder()
            streamingThinking = StringBuilder()
            streamingStartedAt = now
            thinkingEndMs = 0L
            stopRequested = false

            _state.update { st ->
                st.copy(
                    input = "", isStreaming = true, error = null,
                    messages = st.messages +
                        UiMessage(userMsg.id, true, text, now) +
                        UiMessage(assistantMsg.id, false, "", now,
                            streaming = true, thinkingStreaming = true, inThinking = true)
                )
            }

            repo.addMessage(userMsg)
            repo.addMessage(assistantMsg)

            generationJob = viewModelScope.launch {
                try {
                    val sysPromptBase = prefs.systemPrompt.first()
                    val sysPrompt = buildSystemPrompt(sysPromptBase)

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
                                    streamingThinking.toString(), streamingContent.toString())
                                if (combined.isNotEmpty()) {
                                    runCatching { repo.updateMessageContent(id, combined) }
                                }
                                if (!_state.value.isStreaming) break
                            }
                        } catch (_: Throwable) {}
                    }

                    p.chat(all, emptyList(), modelId).collect { ev ->
                        if (stopRequested) return@collect
                        when (ev) {
                            is LlmEvent.TextDelta -> {
                                processDelta(ev.text)
                                uiTick.trySend(Unit)
                            }
                            is LlmEvent.Error -> _state.update { it.copy(error = ev.message) }
                            LlmEvent.Done -> {}
                            else -> {}
                        }
                    }

                    persistJob?.cancel()
                    val finalCombined = ThinkingParser.mergeForDb(
                        streamingThinking.toString(), streamingContent.toString())
                    runCatching { repo.updateMessageContent(assistantMsg.id, finalCombined) }
                    runCatching { repo.touch(chatId) }

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
                } catch (t: Throwable) {
                    Log.e(TAG, "gen err", t)
                    runCatching { repo.updateMessageError(assistantMsg.id, t.message) }
                    _state.update { it.copy(error = t.message) }
                } finally {
                    streamingMsgId = null
                    persistJob?.cancel()
                    _state.update { it.copy(isStreaming = false) }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "send err", t)
            _state.update { it.copy(error = t.message, isStreaming = false) }
        }
    }

    private fun processDelta(piece: String) {
        val st = _state.value.messages.find { it.id == streamingMsgId }
        val inThink = st?.inThinking ?: true

        if (inThink) {
            val closeTag = detectCloseTag(piece)
            if (closeTag != null) {
                val (before, after) = piece.split(closeTag, limit = 2)
                streamingThinking.append(before)
                streamingContent.append(after)
                if (thinkingEndMs == 0L) thinkingEndMs = System.currentTimeMillis()
                updateInThinking(false)
            } else {
                val openTag = detectOpenTag(piece)
                if (openTag != null) {
                    val after = piece.substringAfter(openTag)
                    streamingThinking.append(after)
                } else {
                    streamingThinking.append(piece)
                }
            }
        } else {
            val openTag = detectOpenTag(piece)
            if (openTag != null && streamingThinking.isEmpty()) {
                val after = piece.substringAfter(openTag)
                streamingThinking.append(after)
                updateInThinking(true)
            } else {
                streamingContent.append(piece)
            }
        }
    }

    private fun updateInThinking(v: Boolean) {
        val id = streamingMsgId ?: return
        _state.update { st ->
            val list = st.messages.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(
                inThinking = v, thinkingStreaming = v)
            st.copy(messages = list)
        }
    }

    private fun detectCloseTag(s: String): String? {
        for (t in listOf("", "</think>", "<|/think|>")) if (s.contains(t)) return t
        return null
    }

    private fun detectOpenTag(s: String): String? {
        for (t in listOf(" thinking\n", " thinking", "<think>\n", "<think>", "<|think|>\n", "<|think|>")) {
            if (s.contains(t)) return t
        }
        return null
    }

    private fun flushStreamingToUi() {
        val id = streamingMsgId ?: return
        val think = streamingThinking.toString()
        val resp = streamingContent.toString()
        val now = System.currentTimeMillis()
        val inThink = resp.isEmpty() && think.isNotEmpty()

        _state.update { st ->
            val list = st.messages.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    content = resp,
                    thinking = think,
                    totalMs = now - streamingStartedAt,
                    thinkingMs = if (thinkingEndMs > 0) thinkingEndMs - streamingStartedAt else 0L,
                    streaming = true,
                    thinkingStreaming = inThink,
                    inThinking = inThink
                )
            }
            st.copy(messages = list)
        }
    }

    private fun buildSystemPrompt(base: String): String {
        val skills = _state.value.enabledSkills
        if (skills.isEmpty()) return base
        val prompts = skills.mapNotNull { SkillCatalog.byId(it)?.prompt }
        return base + "\n\nAKTİF YETENEKLER:\n" + prompts.joinToString("\n---\n")
    }
}
