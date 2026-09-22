package com.redhawk.code.llm

import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.flow.Flow

/**
 * Tüm LLM sağlayıcıları (Gemini, OpenAI-uyumlu, Local llama.cpp) bu arayüzü uygular.
 */
interface LlmProvider {
    val id: String
    val displayName: String

    /**
     * Sohbeti çalıştırır. Streaming parçaları Flow üzerinden döner.
     * Tool call destekleniyorsa ToolCallEvent de yayınlanır.
     */
    fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec> = emptyList(),
        model: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<LlmEvent>
}

sealed class LlmEvent {
    data class TextDelta(val text: String) : LlmEvent()
    data class ToolCallRequested(val call: com.redhawk.code.llm.model.ToolCall) : LlmEvent()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        /** true = kota/hız sınırı → otomatik sağlayıcı geçişi denenebilir */
        val quotaExceeded: Boolean = false
    ) : LlmEvent()
    object Done : LlmEvent()
}
