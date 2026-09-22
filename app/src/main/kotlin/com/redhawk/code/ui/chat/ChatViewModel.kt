package com.redhawk.code.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.Role
import com.redhawk.code.llm.provider.OpenAiCompatProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(val role: Role, val content: String)

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : ViewModel() {

    private val provider = OpenAiCompatProvider(baseUrl = baseUrl, apiKey = apiKey)

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun onInputChange(s: String) = _state.update { it.copy(input = s) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isStreaming) return

        val user = UiMessage(Role.USER, text)
        val afterUser = _state.value.messages + user
        val assistantIdx = afterUser.size
        _state.update {
            it.copy(
                messages = afterUser + UiMessage(Role.ASSISTANT, ""),
                input = "",
                isStreaming = true,
                error = null
            )
        }

        viewModelScope.launch {
            val history = afterUser.map { ChatMessage(role = it.role, content = it.content) }
            try {
                provider.chat(history, emptyList(), model).collect { ev ->
                    when (ev) {
                        is LlmEvent.TextDelta -> append(assistantIdx, ev.text)
                        is LlmEvent.Error -> _state.update { it.copy(error = ev.message) }
                        LlmEvent.Done -> _state.update { it.copy(isStreaming = false) }
                        else -> {}
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(error = t.message, isStreaming = false) }
            }
            _state.update { it.copy(isStreaming = false) }
        }
    }

    private fun append(idx: Int, delta: String) = _state.update { st ->
        val list = st.messages.toMutableList()
        if (idx in list.indices) list[idx] = list[idx].copy(content = list[idx].content + delta)
        st.copy(messages = list)
    }
}
