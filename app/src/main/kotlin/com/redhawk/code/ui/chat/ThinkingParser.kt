package com.redhawk.code.ui.chat

/**
 * Model çıktısından düşünme bloğunu ayırır.
 * Desteklenen formatlar:
 *   - <think>...</think>            (DeepSeek R1, Qwen3)
 *   -  thinking...                 (alternatif)
 *   - <|think|>...</|think|>       (bazı modeller)
 *   - kapanış tek başına gelse bile (açılışsız) ayrıştırılır (küçük modeller)
 */
object ThinkingParser {

    private val OPEN = listOf(" thinking\n", " thinking", "<think>\n", "<think>", "<|think|>\n", "<|think|>")
    private val CLOSE = listOf("", "</think>", "<|/think|>", "<|endthink|>")

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
                Parsed("", stripTags(raw.trimStart('\n')), false)
            openIdx != -1 && closeIdx == -1 -> {
                val t = raw.substring(openIdx + openTag.length).trimStart('\n')
                Parsed(t, "", true)
            }
            closeIdx != -1 -> {
                val tStart = if (openIdx != -1) openIdx + openTag.length else 0
                val t = raw.substring(tStart, closeIdx).trim('\n').trim()
                val r = raw.substring(closeIdx + closeTag.length).trimStart('\n')
                Parsed(t, stripTags(r), false)
            }
            else -> Parsed("", stripTags(raw.trimStart('\n')), false)
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

    private fun cleanResponse(s: String): String {
        var r = stripTags(s)
        r = r.replace("<|im_end|>", "")
        r = r.replace("<|im_start|>", "")
        r = r.replace("<|endoftext|>", "")
        r = r.replace("<|end|>", "")
        return r.trim()
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
