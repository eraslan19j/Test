package com.redhawk.code.ui.chat

data class UiMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val createdAt: Long,
    val thinking: String = "",
    val thinkingMs: Long = 0L,
    val totalMs: Long = 0L,
    val streaming: Boolean = false,
    val thinkingStreaming: Boolean = false,
    val inThinking: Boolean = false,
    val toolLabel: String? = null,
    val error: String? = null
)
