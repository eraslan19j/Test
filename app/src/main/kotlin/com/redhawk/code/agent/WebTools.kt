package com.redhawk.code.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Ajanın internet erişimi — %100 ücretsiz, API anahtarı gerekmez.
 *  - search: DuckDuckGo (kayıtsız HTML ucu)
 *  - fetch: herhangi bir http(s) sayfasını metne çevirir
 */
object WebTools {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    /** DuckDuckGo ile web araması, ilk 5 sonuç */
    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext "HATA: aranacak metin boş."
        try {
            val url = "https://html.duckduckgo.com/html/?q=" +
                java.net.URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext "HATA: arama başarısız (HTTP ${resp.code})."
                resp.body?.string()?.take(400_000) ?: return@withContext "HATA: boş yanıt."
            }
            val results = parseDuckResults(html).take(5)
            if (results.isEmpty()) return@withContext "'$query' için sonuç bulunamadı."
            buildString {
                append("'$query' arama sonuçları:\n")
                results.forEachIndexed { i, (title, link) ->
                    append("${i + 1}. $title\n   $link\n")
                }
                append("\nDetay için fetch_url aracıyla sayfayı okuyabilirsin.")
            }
        } catch (t: Throwable) {
            "HATA: arama yapılamadı: ${t.message}"
        }
    }

    /** Sayfayı indirip düz metne çevirir (max ~8000 karakter) */
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            return@withContext "HATA: geçersiz URL (http/https olmalı)."
        }
        try {
            val req = Request.Builder().url(clean).header("User-Agent", UA).build()
            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext "HATA: sayfa açılamadı (HTTP ${resp.code})."
                val type = resp.header("Content-Type") ?: ""
                if (!type.contains("text") && !type.contains("html") && !type.contains("json") && !type.contains("xml")) {
                    return@withContext "HATA: bu içerik türü okunamıyor ($type)."
                }
                resp.body?.string()?.take(150_000) ?: return@withContext "HATA: boş sayfa."
            }
            val text = htmlToText(html).take(8000)
            if (text.length < 50) return@withContext "HATA: sayfadan anlamlı metin çıkmadı."
            "Sayfa içeriği ($clean):\n$text"
        } catch (t: Throwable) {
            "HATA: sayfa okunamadı: ${t.message}"
        }
    }

    // ---- ayrıştırıcılar (harici bağımlılık yok) ----

    private val RESULT_RE = Regex(
        """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
        RegexOption.DOT_MATCHES_ALL
    )

    private fun parseDuckResults(html: String): List<Pair<String, String>> {
        return RESULT_RE.findAll(html).mapNotNull { m ->
            val href = m.groupValues[1]
            val rawTitle = m.groupValues[2]
            val title = stripTags(rawTitle).trim().take(140)
            val link = extractDuckLink(href) ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null
            title to link
        }.toList()
    }

    /** //duckduckgo.com/l/?uddg=<şifreli> → gerçek adres */
    private fun extractDuckLink(href: String): String? {
        return try {
            if (href.contains("uddg=")) {
                val enc = href.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(enc, "UTF-8")
            } else if (href.startsWith("http")) {
                href
            } else null
        } catch (_: Throwable) { null }
    }

    private fun stripTags(s: String): String =
        s.replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&").replace("&quot;", "\"")
            .replace("&#x27;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")

    private fun htmlToText(html: String): String {
        var t = html
        t = t.replace(Regex("(?s)<script.*?</script>"), " ")
        t = t.replace(Regex("(?s)<style.*?</style>"), " ")
        t = t.replace(Regex("(?s)<!--.*?-->"), " ")
        t = t.replace(Regex("</(p|div|br|h[1-6]|li|tr)[^>]*>"), "\n")
        t = stripTags(t)
        return t.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n")
    }
}
