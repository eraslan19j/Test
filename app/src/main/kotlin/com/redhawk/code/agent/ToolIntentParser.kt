package com.redhawk.code.agent

/**
 * METİN-İÇİ ARAÇ NİYETİ: function-calling bilmeyen küçük modeller
 * araç çağrısını düz metin olarak yazar ("Use list_files...").
 * Bu ayrıştırıcı o niyeti yakalayıp GERÇEK çağrıya çevirir —
 * ajan döngüsü böylece her modelde çalışır.
 *
 * Desteklenen yazımlar (öncelik sırasıyla):
 *  1. <tool name="list_files">{"path": ""}</tool>   (prompt'ta öğretilen)
 *  2. list_files({"path": ""}) / read_file("Main.kt") (fonksiyon tarzı)
 *  3. Use list_files with empty path / read_file "Main.kt" (doğal dil)
 */
object ToolIntentParser {

    data class Intent(val name: String, val argsJson: String)

    val TOOL_NAMES = listOf("list_files", "read_file", "write_file", "web_search", "fetch_url")

    private val TOOL_TAG = Regex(
        "<tool\\s+name=[\"']([a-z_]+)[\"']\\s*>(.*?)</tool>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val EMPTY_PATH = Regex(
        "\\buse\\s+(list_files)\\s+with\\s+(empty|blank|no)\\s+path",
        RegexOption.IGNORE_CASE
    )
    private val QUOTED_ARG = Regex(
        "\\b(list_files|read_file|web_search|fetch_url)\\b[^()\\n\"“]{0,40}[\"“]([^\"”\\n]{1,200})[\"”]"
    )

    /** Tur metninden en fazla 3 araç niyeti çıkarır (tekrarlar elenir). */
    fun extract(text: String): List<Intent> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<Intent>()
        TOOL_TAG.findAll(text).forEach { m ->
            val name = m.groupValues[1].lowercase()
            if (name in TOOL_NAMES) {
                out.add(Intent(name, m.groupValues[2].trim().ifBlank { "{}" }))
            }
        }
        out.addAll(extractFuncStyle(text))
        EMPTY_PATH.findAll(text).forEach { m ->
            out.add(Intent(m.groupValues[1].lowercase(), "{\"path\": \"\"}"))
        }
        QUOTED_ARG.findAll(text).forEach { m ->
            out.add(Intent(m.groupValues[1].lowercase(), singleArg(m.groupValues[1].lowercase(), m.groupValues[2])))
        }
        // write_file doğal dille gelmez (içeriksiz dosya yazılmasın)
        return out.filter { it.name in TOOL_NAMES }
            .distinctBy { it.name + "\n" + it.argsJson }
            .take(3)
    }

    /** Ekrana/DB'ye yazılmadan önce <tool> bloklarını temizler. */
    fun stripToolBlocks(text: String): String {
        if (!text.contains("<tool")) return text
        return TOOL_TAG.replace(text, "").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /** name({...}) veya name("tek argüman") veya name() tarzı */
    private fun extractFuncStyle(text: String): List<Intent> {
        val out = mutableListOf<Intent>()
        for (name in TOOL_NAMES) {
            var from = 0
            while (from < text.length) {
                val i = text.indexOf(name, from)
                if (i < 0) break
                val prevOk = i == 0 || (!text[i - 1].isLetterOrDigit() && text[i - 1] != '_')
                var j = i + name.length
                while (j < text.length && text[j].isWhitespace()) j++
                if (!prevOk || j >= text.length || text[j] != '(') {
                    from = i + name.length; continue
                }
                j++
                while (j < text.length && text[j].isWhitespace()) j++
                if (j >= text.length) break
                when (text[j]) {
                    '{' -> {
                        val end = balancedEnd(text, j)
                        if (end > j) {
                            out.add(Intent(name, text.substring(j, end + 1)))
                            from = end + 1
                        } else from = j + 1
                    }
                    '"', '\'' -> {
                        val q = text[j]
                        val k = text.indexOf(q, j + 1)
                        if (k > j) {
                            out.add(Intent(name, singleArg(name, text.substring(j + 1, k))))
                            from = k + 1
                        } else from = j + 1
                    }
                    ')' -> {
                        out.add(Intent(name, "{}")); from = j + 1
                    }
                    else -> from = j + 1
                }
            }
        }
        return out
    }

    /** Dengeli { } taraması (tırnak ve kaçışlara saygılı) */
    private fun balancedEnd(s: String, open: Int): Int {
        var d = 0
        var inStr = false
        var q = '"'
        var i = open
        while (i < s.length) {
            val c = s[i]
            if (inStr) {
                if (c == '\\') i++
                else if (c == q) inStr = false
            } else when (c) {
                '{' -> d++
                '}' -> { d--; if (d == 0) return i }
                '"', '\'' -> { inStr = true; q = c }
            }
            i++
        }
        return -1
    }

    private fun singleArg(name: String, v: String): String {
        val key = when (name) {
            "web_search" -> "query"
            "fetch_url" -> "url"
            else -> "path"
        }
        val esc = v.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "")
        return "{\"$key\": \"$esc\"}"
    }
}
