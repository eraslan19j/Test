package com.redhawk.code.llm.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

@Serializable
data class ChatMessage(
    val role: Role,
    val content: String = "",
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolCalls: List<ToolCall> = emptyList()
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)
