package com.redhawk.code.llm.provider

import com.redhawk.code.llm.LlmEvent
import com.redhawk.code.llm.LlmProvider
import com.redhawk.code.llm.model.ChatMessage
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

/**
 * DuckDuckGo AI Chat — anahtarsız, ücretsiz (GPT-4o mini vb).
 *
 * RESMİ OLMAYAN uç (deneysel): web uygulamasının kullandığı protokol.
 * DuckDuckGo değiştirirse bozulabilir; hata verirse başka sağlayıcıya geç.
 * Araç (tool) desteği YOKTUR — ajan modu bu sağlayıcıda düz sohbet olur.
 */
class DuckAiProvider(
    override val id: String,
    override val displayName: String
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<LlmEvent> = callbackFlow {
        // 1) Oturum + anonim token
        val token = try {
            fetchToken()
        } catch (t: Throwable) {
            trySend(LlmEvent.Error("Duck.ai bağlantısı kurulamadı: ${t.message}", t))
            trySend(LlmEvent.Done); close(); return@callbackFlow
        }
        if (token.isNullOrBlank()) {
            trySend(LlmEvent.Error("Duck.ai oturum anahtarı alınamadı. Biraz sonra tekrar dene."))
            trySend(LlmEvent.Done); close(); return@callbackFlow
        }

        // 2) Sohbet isteği (araçlar gönderilmez — destek yok)
        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        val role = m.role.name.lowercase()
                        put("role", if (role == "tool") "user" else role)
                        put("content", m.content.ifBlank { " " })
                    }
                }
            }
        }.toString()

        val req = Request.Builder()
            .url("https://duckduckgo.com/duckchat/v1/chat")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", UA)
            .header("Accept", "text/event-stream")
            .header("x-vqd-4", token)
            .build()

        val listener = object : EventSourceListener() {
            var prevLen = 0
            var gotFirst = false

            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") { trySend(LlmEvent.Done); close(); return }
                gotFirst = true
                parseMessage(data)?.let { full ->
                    // Duck.ai kümülatif gönderir → sadece yeni kuyruğu yayınla
                    if (full.length >= prevLen) {
                        val suffix = full.substring(prevLen)
                        prevLen = full.length
                        if (suffix.isNotEmpty()) trySend(LlmEvent.TextDelta(suffix))
                    } else {
                        prevLen = full.length
                        if (full.isNotEmpty()) trySend(LlmEvent.TextDelta(full))
                    }
                }
            }

            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code ?: 0
                // 429/418 = hız sınırı → otomatik sağlayıcı geçişini tetikler
                val quota = code == 429 || code == 418
                val msg = when {
                    quota -> "Duck.ai hız sınırı. Otomatik geçiş deneniyor…"
                    code in 500..599 -> "Duck.ai sunucu hatası ($code)."
                    code == 404 -> "Duck.ai modeli '$model' artık yok. Başka model seç."
                    code > 0 -> "Duck.ai HTTP $code."
                    t is java.net.UnknownHostException -> "İnternet yok."
                    t is java.net.SocketTimeoutException -> "Bağlantı zaman aşımı."
                    else -> t?.message ?: "Duck.ai bağlantı hatası."
                }
                trySend(LlmEvent.Error(msg, t, quota))
                trySend(LlmEvent.Done)
                close()
            }

            override fun onClosed(es: EventSource) { trySend(LlmEvent.Done); close() }

            private fun parseMessage(data: String): String? = try {
                json.parseToJsonElement(data).jsonObject["message"]
                    ?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) { null }
        }

        val es = EventSources.createFactory(client).newEventSource(req, listener)

        // İlk token bekçisi: 60 sn sessizlik = zaman aşımı (takılan "…" bitsin)
        val watchdog = launch {
            delay(60_000)
            if (!listener.gotFirst) {
                trySend(LlmEvent.Error("Duck.ai 60 sn içinde yanıt vermedi. Tekrar dene."))
                trySend(LlmEvent.Done)
                close()
            }
        }
        awaitClose { watchdog.cancel(); es.cancel() }
    }

    private suspend fun fetchToken(): String? = withContext(Dispatchers.IO) {
        // Adım 0: ön sayfa (oturum için)
        val warm = Request.Builder()
            .url("https://duckduckgo.com/?q=DuckDuckGo+AI+Chat&ia=chat&duckai=1")
            .header("User-Agent", UA)
            .build()
        runCatching { client.newCall(warm).execute().close() }

        // Adım 1: anonim token
        val status = Request.Builder()
            .url("https://duckduckgo.com/duckchat/v1/status")
            .header("User-Agent", UA)
            .header("X-Vqd-Accept", "1")
            .build()
        client.newCall(status).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            resp.header("x-vqd-4")
        }
    }

    companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
