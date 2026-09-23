package com.redhawk.code.ui.chat

/**
 * Model çıktısından düşünme bloğunu ayırır.
 * Desteklenen formatlar:
 *   - <think>...</think>            (DeepSeek R1, Qwen3)
 *   -  thinking...                 (alternatif)
 *   - <|think|>...</|think|>       (bazı modeller)
 *   - kapanış tek başına gelse bile (açılışsız) ayrıştırılır (küçük modeller)
 *   - etiketsiz İngilizce giriş cümleleri ayrıca temizlenir (bkz. stripPreamble)
 */
object ThinkingParser {

    private val OPEN = listOf(" thinking\n", " thinking", "<think>\n", "<think>", "<|think|>\n", "<|think|>")
    private val CLOSE = listOf("", "</think>", "<|/think|>", "<|endthink|>")

    /** Küçük modellerin etiketsiz yazdığı kendi-kendine-konuşma girişleri */
    private val SELF_TALK_START = listOf(
        "the user ask", "the user says", "the user wants", "the user wrote",
        "they want", "as an ai", "as a language model",
        "we should", "we can", "we must", "we need to",
        "we don't have", "we do not have", "we cannot",
        "i should", "i must", "i need to", "i will respond",
        "let's", "let us", "follow", "according to policy",
        "the instructions say", "the developer says",
        "should be short", "provide ", "my response", "in this response",
        "according to", "the response shows", "so we can", "the output shows"
    )
    private val TR_CHARS = setOf('ç', 'Ç', 'ğ', 'Ğ', 'ı', 'İ', 'ö', 'Ö', 'ş', 'Ş', 'ü', 'Ü')
    private val SENT_SPLIT = Regex("(?<=[.!?…])\\s+|(?<=\\.)(?=[A-ZÇĞİÖŞÜ])")

    data class Parsed(val thinking: String, val response: String, val stillThinking: Boolean)

    fun parseStreaming(raw: String): Parsed {
        var openIdx = -1; var openTag = ""
        for (t in OPEN) {
            val i = raw.indexOf(t)
            if (i != -1 && (openIdx == -1 || i < openIdx)) { openIdx = i; openTag = t }
        }

        // Kapanış: açılış varsa ondan sonra, yoksa metnin başından ara.
        // (Bazı küçük modeller açılışı yutar, sadece kapanışı yazar —
        //  yakalanmazsa düşünme + etiket cevaba sızar.)
        val from = if (openIdx != -1) openIdx + openTag.length else 0
        var closeIdx = -1; var closeTag = ""
        for (t in CLOSE) {
            val i = raw.indexOf(t, from)
            if (i != -1 && (closeIdx == -1 || i < closeIdx)) { closeIdx = i; closeTag = t }
        }

        return when {
            openIdx == -1 && closeIdx == -1 ->
                Parsed("", stripPreamble(stripTags(raw.trimStart('\n'))), false)
            openIdx != -1 && closeIdx == -1 -> {
                val t = raw.substring(openIdx + openTag.length).trimStart('\n')
                Parsed(t, "", true)
            }
            closeIdx != -1 -> {
                val tStart = if (openIdx != -1) openIdx + openTag.length else 0
                val t = raw.substring(tStart, closeIdx).trim('\n').trim()
                val r = raw.substring(closeIdx + closeTag.length).trimStart('\n')
                Parsed(t, stripPreamble(stripTags(r)), false)
            }
            else -> Parsed("", stripPreamble(stripTags(raw.trimStart('\n'))), false)
        }
    }

    fun parseStored(raw: String): Parsed {
        val p = parseStreaming(raw)
        return Parsed(p.thinking, cleanResponse(p.response), false)
    }

    /** Yanıta sızmış etiket artıklarını temizle (güvenlik ağı) */
    private fun stripTags(s: String): String {
        var r = s
        (OPEN + CLOSE).forEach { r = r.replace(it, "") }
        return r.trimStart('\n').trimEnd()
    }

    /**
     * Baştaki etiketsiz İngilizce düşünme cümlelerini at
     * ("The user asks..." gibi girişler).
     * SADECE arkada Türkçe içerik varsa atılır — tam-İngilizce
     * cevaplara dokunulmaz. Biçim (paragraflar) korunur.
     */
    private fun stripPreamble(s: String): String {
        val matches = SENT_SPLIT.findAll(s).take(7).toList()
        if (matches.isEmpty()) return s
        val bounds = listOf(0) + matches.map { it.range.last + 1 }
        var dropEnd = 0
        var dropped = 0
        for (i in 0 until bounds.size - 1) {
            val sent = s.substring(bounds[i], bounds[i + 1]).trim()
            if (sent.isEmpty()) {
                dropEnd = bounds[i + 1]
                continue
            }
            // Tırnak içi kullanıcı alıntısını yoksay ("The user asks: "..."")
            val bare = sent.lowercase().replace(Regex("\"[^\"]*\""), "")
            val trLooking = bare.any { it in TR_CHARS }
            if (SELF_TALK_START.any { bare.startsWith(it) } && !trLooking) {
                dropEnd = bounds[i + 1]
                dropped++
                if (dropped >= 6) break
            } else break
        }
        if (dropped == 0) return s
        val rest = s.substring(dropEnd).trimStart()
        if (rest.length < 10) return s
        // Arkada Türkçe yoksa (tam-İngilizce cevap) dokunma
        if (!rest.any { it in TR_CHARS }) return s
        return rest
    }

    private fun cleanResponse(s: String): String {
        var r = stripTags(s)
        r = r.replace("<|im_end|>", "")
        r = r.replace("<|im_start|>", "")
        r = r.replace("<|endoftext|>", "")
        r = r.replace("<|end|>", "")
        return stripPreamble(r.trim())
    }

    fun mergeForDb(thinking: String, response: String): String {
        if (thinking.isBlank()) return cleanResponse(response)
        return " thinking\n$thinking\n\n${cleanResponse(response)}"
    }

    fun formatDuration(ms: Long): String = when {
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> String.format("%.1fs", ms / 1000.0)
        else -> "${ms / 60_000}dk ${(ms % 60_000) / 1000}s"
    }
}
