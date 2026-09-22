package com.redhawk.code.ui.chat

/**
 * Model çıktısından <think>...</think> bloğunu ayırır.
 * Desteklenen formatlar:
 *   - <think>...</think>            (DeepSeek R1, Qwen3)
 *   -  thinking...                 (alternatif)
 *   - <|think|>...</|think|>       (bazı modeller)
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

        var closeIdx = -1; var closeTag = ""
        if (openIdx != -1) {
            val from = openIdx + openTag.length
            for (t in CLOSE) {
                val i = raw.indexOf(t, from)
                if (i != -1 && (closeIdx == -1 || i < closeIdx)) { closeIdx = i; closeTag = t }
            }
        }

        return when {
            openIdx == -1 && closeIdx == -1 -> Parsed("", raw.trimStart('\n'), false)
            openIdx != -1 && closeIdx == -1 -> {
                val t = raw.substring(openIdx + openTag.length).trimStart('\n')
                Parsed(t, "", true)   // hâlâ düşünüyor
            }
            openIdx != -1 && closeIdx != -1 -> {
                val t = raw.substring(openIdx + openTag.length, closeIdx).trim('\n')
                val r = raw.substring(closeIdx + closeTag.length).trimStart('\n')
                Parsed(t, r, false)
            }
            else -> Parsed("", raw.trimStart('\n'), false)
        }
    }

    fun parseStored(raw: String): Parsed {
        val p = parseStreaming(raw)
        return Parsed(p.thinking, cleanResponse(p.response), false)
    }

    private fun cleanResponse(s: String): String {
        var r = s
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
