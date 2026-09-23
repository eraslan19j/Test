package com.redhawk.code.llm.provider

import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.LlmProvider
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.ToolCall
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class KiloGatewayProvider(
    override val id: String,
    override val displayName: String = "Kilo Gateway"
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val baseUrl = "https://api.kilo.ai/api/gateway"

    override fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<LlmEvent> = callbackFlow {
        val body = buildBody(messages, tools, model, temperature, maxTokens)
        val url = "$baseUrl/chat/completions"

        val reqB = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer nil")

        val listener = object : EventSourceListener() {
            var toolId: String? = null
            var toolName: String? = null
            val toolArgs = StringBuilder()
            var sawToolCall = false
            var gotFirst = false
            var thinkOpen = false
            var thinkClosed = false
            var inputTokens = 0
            var outputTokens = 0

            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") { trySend(LlmEvent.Done); close(); return }
                gotFirst = true
                parseChunk(data)?.let { ev ->
                    when (ev) {
                        is LlmEvent.TextDelta -> trySend(ev)
                        is LlmEvent.ToolCallRequested -> trySend(ev)
                        is LlmEvent.Usage -> {
                            inputTokens = ev.inputTokens
                            outputTokens = ev.outputTokens
                            trySend(ev)
                        }
                        is LlmEvent.Error -> trySend(ev)
                        LlmEvent.Done -> trySend(LlmEvent.Done)
                    }
                }
            }

            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code ?: 0
                val bodySnippet = try { response?.body?.string()?.take(200) } catch (_: Throwable) { null }
                val msg = when {
                    code == 400 -> "İstek geçersiz. Model ID: $model"
                    code == 401 -> "Kimlik doğrulama hatası."
                    code == 402 -> "Kredi yetersiz."
                    code == 403 -> "Bu modele erişim yok."
                    code == 404 -> "Model '$model' bulunamadı."
                    code == 429 -> "Çok fazla istek. Lütfen bekleyin."
                    code in 500..599 -> "Sunucu hatası ($code)."
                    code > 0 -> "HTTP $code: ${bodySnippet ?: "hata"}"
                    t is java.net.UnknownHostException -> "İnternet bağlantısı yok."
                    t is java.net.SocketTimeoutException -> "Bağlantı zaman aşımı."
                    else -> t?.message ?: "Bir hata oluştu."
                }
                val quota = code == 429 || code == 402 || code == 403
                trySend(LlmEvent.Error(msg, t, quota))
                trySend(LlmEvent.Done)
                close()
            }

            override fun onClosed(es: EventSource) {
                if (!gotFirst) {
                    trySend(LlmEvent.Error("Sağlayıcı yanıt vermedi. Tekrar deneyin."))
                    trySend(LlmEvent.Done)
                }
                close()
            }

            private fun parseChunk(data: String): LlmEvent? {
                return try {
                    val obj = json.parseToJsonElement(data).jsonObject
                    obj["error"]?.let { e ->
                        val emsg = when (e) {
                            is JsonObject -> e["message"]?.jsonPrimitive?.contentOrNull
                                ?: e.toString().take(200)
                            is JsonPrimitive -> e.contentOrNull ?: e.toString()
                            else -> e.toString().take(200)
                        }.ifBlank { "API hatası" }
                        val low = emsg.lowercase()
                        val quota = low.contains("rate") || low.contains("quota") ||
                            low.contains("429") || low.contains("credit") ||
                            low.contains("insufficient") || low.contains("limit") ||
                            low.contains("balance") || low.contains("payment")
                        return LlmEvent.Error(emsg.take(300), null, quota)
                    }

                    val usage = obj["usage"]?.jsonObject
                    if (usage != null) {
                        val promptTokens = usage["prompt_tokens"]?.jsonPrimitive?.intOrNull ?: 0
                        val completionTokens = usage["completion_tokens"]?.jsonPrimitive?.intOrNull ?: 0
                        if (promptTokens > 0 || completionTokens > 0) {
                            return LlmEvent.Usage(promptTokens, completionTokens)
                        }
                    }

                    obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
                    val choice = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
                    val finish = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
                    val delta = choice["delta"]?.jsonObject

                    val tc = delta?.get("tool_calls")?.jsonArray?.firstOrNull()?.jsonObject
                    if (tc != null) {
                        sawToolCall = true
                        tc["id"]?.jsonPrimitive?.contentOrNull
                            ?.takeIf { it.isNotBlank() }?.let { toolId = it }
                        val fn = tc["function"]?.jsonObject
                        fn?.get("name")?.jsonPrimitive?.contentOrNull
                            ?.takeIf { it.isNotBlank() }?.let { toolName = it }
                        fn?.get("arguments")?.jsonPrimitive?.contentOrNull
                            ?.let { toolArgs.append(it) }
                    }

                    if (finish == "tool_calls" && sawToolCall) {
                        sawToolCall = false
                        return LlmEvent.ToolCallRequested(
                            ToolCall(
                                id = toolId ?: "call_${System.currentTimeMillis()}",
                                name = toolName ?: "",
                                argumentsJson = toolArgs.toString().ifBlank { "{}" }
                            )
                        )
                    }

                    val reasoning = delta?.get("reasoning_content")
                        ?.jsonPrimitive?.contentOrNull
                        ?: delta?.get("reasoning")?.jsonPrimitive?.contentOrNull
                    if (!reasoning.isNullOrEmpty()) {
                        if (!thinkOpen) {
                            thinkOpen = true
                            trySend(LlmEvent.TextDelta(""))
                        }
                        return LlmEvent.TextDelta(reasoning)
                    }

                    val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                    if (!content.isNullOrEmpty()) {
                        if (thinkOpen && !thinkClosed) {
                            thinkClosed = true
                            trySend(LlmEvent.TextDelta(""))
                        }
                        return LlmEvent.TextDelta(content)
                    } else {
                        return null
                    }
                } catch (_: Exception) { null }
            }
        }

        val es = EventSources.createFactory(client).newEventSource(reqB.build(), listener)

        val watchdog = launch {
            delay(60_000)
            if (!listener.gotFirst) {
                trySend(LlmEvent.Error("60 saniyede yanıt alınamadı."))
                trySend(LlmEvent.Done)
                close()
            }
        }
        awaitClose { watchdog.cancel(); es.cancel() }
    }

    private fun buildBody(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        val root = kotlinx.serialization.json.buildJsonObject {
            put("model", model)
            put("stream", true)
            put("stream_options", kotlinx.serialization.json.buildJsonObject {
                put("include_usage", true)
            })
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        put("role", m.role.name.lowercase())
                        put("content", m.content)
                        if (m.toolCallId != null) put("tool_call_id", m.toolCallId)
                        if (m.toolCalls.isNotEmpty()) {
                            putJsonArray("tool_calls") {
                                m.toolCalls.forEach { tc ->
                                    addJsonObject {
                                        put("id", tc.id)
                                        put("type", "function")
                                        putJsonObject("function") {
                                            put("name", tc.name)
                                            put("arguments", tc.argumentsJson)
                                        }
                                    }
                                }
                            }
                        }
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
                                put("parameters", kotlinx.serialization.json.Json.parseToJsonElement(t.parametersJsonSchema))
                            }
                        }
                    }
                }
            }
        }
        return root.toString()
    }
}
