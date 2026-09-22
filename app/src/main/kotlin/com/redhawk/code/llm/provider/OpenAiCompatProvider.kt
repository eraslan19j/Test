package com.redhawk.code.llm.provider

import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.LlmProvider
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.ToolCall
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * OpenAI /v1/chat/completions uyumlu her sunucu için:
 * Ollama (http://host:11434/v1), LM Studio (1234), llama.cpp server (8080), OpenAI, vLLM ...
 */
class OpenAiCompatProvider(
    override val id: String = "openai_compat",
    override val displayName: String = "OpenAI-uyumlu",
    private val baseUrl: String,
    private val apiKey: String = ""
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // SSE: timeout yok
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String
    ): Flow<LlmEvent> = callbackFlow {
        val body = buildBody(messages, tools, model)
        val reqB = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "text/event-stream")
        if (apiKey.isNotBlank()) reqB.header("Authorization", "Bearer $apiKey")

        val listener = object : EventSourceListener() {
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") { trySend(LlmEvent.Done); close(); return }
                parseChunk(data)?.let { trySend(it) }
            }
            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                val msg = response?.let { "HTTP ${it.code} ${it.message}" }
                    ?: (t?.message ?: "network error")
                trySend(LlmEvent.Error(msg, t))
                close()
            }
            override fun onClosed(es: EventSource) { close() }
        }

        val es = EventSources.createFactory(client).newEventSource(reqB.build(), listener)
        awaitClose { es.cancel() }
    }

    private fun buildBody(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String
    ): String {
        val root = buildJsonObject {
            put("model", model)
            put("stream", true)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        put("role", m.role.name.lowercase())
                        put("content", m.content)
                        if (m.toolCallId != null) put("tool_call_id", m.toolCallId)
                        if (m.toolName != null && m.role.name == "TOOL") put("name", m.toolName)
                    }
                }
            }
            if (tools.isNotEmpty()) {
                putJsonArray("tools") {
                    tools.forEach { t ->
                        addJsonObject {
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", t.name)
                                put("description", t.description)
                                put("parameters", Json.parseToJsonElement(t.parametersJsonSchema))
                            }
                        }
                    }
                }
            }
        }
        return root.toString()
    }

    private fun parseChunk(data: String): LlmEvent? = try {
        val obj = json.parseToJsonElement(data).jsonObject
        val choice = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject
        val delta = choice?.get("delta")?.jsonObject

        val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
        if (!content.isNullOrEmpty()) {
            LlmEvent.TextDelta(content)
        } else {
            val toolCalls = delta?.get("tool_calls")?.jsonArray
            if (!toolCalls.isNullOrEmpty()) {
                val tc = toolCalls.first().jsonObject
                val id = tc["id"]?.jsonPrimitive?.contentOrNull ?: ""
                val fn = tc["function"]?.jsonObject
                val name = fn?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                val args = fn?.get("arguments")?.jsonPrimitive?.contentOrNull ?: "{}"
                if (id.isNotEmpty() || name.isNotEmpty())
                    LlmEvent.ToolCallRequested(ToolCall(id, name, args))
                else null
            } else null
        }
    } catch (_: Exception) { null }
}
