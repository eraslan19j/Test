package com.redhawk.code.llm.provider

import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.LlmProvider
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

class OpenAiCompatProvider(
    override val id: String,
    override val displayName: String,
    private val baseUrl: String,
    private val apiKey: String = ""
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String
    ): Flow<LlmEvent> = callbackFlow {
        val body = buildBody(messages, tools, model)
        val url = baseUrl.trimEnd('/') + "/chat/completions"

        val reqB = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")

        if (apiKey.isNotBlank()) reqB.header("Authorization", "Bearer $apiKey")
        if (baseUrl.contains("openrouter.ai")) {
            reqB.header("HTTP-Referer", "https://redhawk.code")
            reqB.header("X-Title", "ReDHawK Code")
        }

        val listener = object : EventSourceListener() {
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") { trySend(LlmEvent.Done); close(); return }
                parseChunk(data)?.let { trySend(it) }
            }

            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code ?: 0
                val bodySnippet = try { response?.body?.string()?.take(200) } catch (_: Throwable) { null }
                val msg = when {
                    code == 400 -> "İstek geçersiz. Model ID: $model"
                    code == 401 -> "API anahtarı geçersiz."
                    code == 402 -> "Kredi yetersiz. Farklı sağlayıcı dene."
                    code == 403 -> "Bu modele izin yok."
                    code == 404 -> "Model '$model' bulunamadı. Model listesinden başka seç."
                    code == 429 -> "Çok fazla istek. 10 sn bekle."
                    code in 500..599 -> "Sunucu hatası ($code)."
                    code > 0 -> "HTTP $code: ${bodySnippet ?: "hata"}"
                    t is java.net.UnknownHostException -> "İnternet yok."
                    t is java.net.SocketTimeoutException -> "Bağlantı zaman aşımı."
                    else -> t?.message ?: "Bağlantı hatası."
                }
                trySend(LlmEvent.Error(msg, t))
                trySend(LlmEvent.Done)
                close()
            }

            override fun onClosed(es: EventSource) { trySend(LlmEvent.Done); close() }
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
        if (!content.isNullOrEmpty()) LlmEvent.TextDelta(content) else null
    } catch (_: Exception) { null }
}
