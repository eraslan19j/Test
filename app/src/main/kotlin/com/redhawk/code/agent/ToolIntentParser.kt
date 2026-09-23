package com.redhawk.code.agent

/**
 * METİN-İÇİ ARAÇ NİYETİ (v2): function-calling bilmeyen küçük modeller
 * araç çağrısını düz metin olarak yazar. Bu ayrıştırıcı niyeti yakalayıp
 * GERÇEK çağrıya çevirir — ajan döngüsü her modelde çalışır.
 *
 * Desteklenen yazımlar (öncelik sırasıyla):
 *  1. <tool name="list_files">{"path": ""}</tool>   (prompt'ta öğretilen)
 *  2. list_files({"path": ""}) / read_file("Main.kt") (fonksiyon tarzı)
 *  3. Use (tool) list_files with empty path          ("tool" opsiyonel!)
 *  4. I can list files / list files in "src"         (boşluklu doğal dil)
 *  5. read_file "Main.kt"                            (çıplak + tırnaklı)
 *
 * write_file SADECE 1-2 ile gelir (içeriksiz dosya yazılmasın).
 * Aynı metin parçası iki kez çalıştırılmaz (claimed aralıkları).
 */
object ToolIntentParser {

    data class Intent(val name: String, val argsJson: String)

    val TOOL_NAMES = listOf("list_files", "read_file", "write_file", "web_search", "fetch_url")

    private val TOOL_TAG = Regex(
        "<tool\\s+name=[\"']([a-z_]+)[\"']\\s*>(.*?)</tool>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    // "Use tool list_files ..." / "Use list_files ..." / "Use the list_files ..."
    private val USE_TOOL = Regex(
        "\\buse\\s+(?:the\\s+|a\\s+)?(?:tool\\s+)?(list_files|read_file|web_search|fetch_url)\\b",
        RegexOption.IGNORE_CASE
    )
    private val LIST_SP = Regex("\\blist\\s+files?\\b", RegexOption.IGNORE_CASE)
    private val READ_SP = Regex("\\bread\\s+(?:the\\s+)?files?\\b", RegexOption.IGNORE_CASE)
    private val BARE_QUOTED = Regex(
        "\\b(list_files|read_file|web_search|fetch_url)\\b[^()\\n\"“]{0,40}[\"“]([^\"”\\n]{1,200})[\"”]"
    )
    private val QUOTED = Regex("[\"“]([^\"”\\n]{1,200})[\"”]")
    // Boşluklu doğal dil, eylem niyetiyle birlikteyse sayılır
    // ("linux'ta list files komutu" açıklaması araç çalıştırmasın)
    private val ACTION_GATE = Regex(
        "\\b(use|call|run|execute|let me|i will|i'll|need to|should|must|going to|i can|we can|will now)\\b",
        RegexOption.IGNORE_CASE
    )

    /** Tur metninden en fazla 3 araç niyeti çıkarır (tekrarlar elenir). */
    fun extract(text: String): List<Intent> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<Intent>()
        val claimed = mutableListOf<IntRange>()

        TOOL_TAG.findAll(text).forEach { m ->
            val name = m.groupValues[1].lowercase()
            if (name in TOOL_NAMES) {
                out.add(Intent(name, m.groupValues[2].trim().ifBlank { "{}" }))
                claimed.add(m.range)
            }
        }

        val funcRanges = mutableListOf<IntRange>()
        out.addAll(extractFuncStyle(blankRanges(text, claimed), funcRanges))
        claimed.addAll(funcRanges)

        // Doğal dil taramaları: claimed dışı bölgelerde, argüman cümleden
        val scan = blankRanges(text, claimed)
        USE_TOOL.findAll(scan).forEach { m ->
            val name = m.groupValues[1].lowercase()
            out.add(Intent(name, sentenceArgs(name, text, m.range.first)))
        }
        LIST_SP.findAll(scan).forEach { m ->
            val sent = sentenceAt(text, m.range.first)
            if (ACTION_GATE.containsMatchIn(sent)) {
                out.add(Intent("list_files", sentenceArgs("list_files", text, m.range.first)))
            }
        }
        READ_SP.findAll(scan).forEach { m ->
            val sent = sentenceAt(text, m.range.first)
            if (ACTION_GATE.containsMatchIn(sent)) {
                out.add(Intent("read_file", sentenceArgs("read_file", text, m.range.first)))
            }
        }
        BARE_QUOTED.findAll(scan).forEach { m ->
            val name = m.groupValues[1].lowercase()
            out.add(Intent(name, singleArg(name, m.groupValues[2])))
        }

        return out.filter { it.name in TOOL_NAMES }
            .distinctBy { it.name + "\n" + it.argsJson }
            .take(3)
    }

    /** Ekrana/DB'ye yazılmadan önce <tool> bloklarını temizler. */
    fun stripToolBlocks(text: String): String {
        if (!text.contains("<tool")) return text
        return TOOL_TAG.replace(text, "").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /** Eşleşmenin içinde olduğu cümle: tırnaklı argüman buradan alınır */
    private fun sentenceAt(text: String, pos: Int): String {
        var s = pos.coerceIn(0, text.length)
        while (s > 0 && text[s - 1] != '.' && text[s - 1] != '!' &&
            text[s - 1] != '?' && text[s - 1] != '\n'
        ) s--
        var e = pos.coerceIn(0, text.length)
        while (e < text.length && text[e] != '.' && text[e] != '!' &&
            text[e] != '?' && text[e] != '\n'
        ) e++
        return text.substring(s, e)
    }

    private fun sentenceArgs(name: String, text: String, pos: Int): String {
        val sent = sentenceAt(text, pos)
        QUOTED.find(sent)?.let { return singleArg(name, it.groupValues[1]) }
        // list_files argümansız = kök listele (en zararsız varsayılan)
        if (name == "list_files") return "{\"path\": \"\"}"
        // Diğerleri: eksik argüman HATA döndürür, model düzeltir
        return "{}"
    }

    private fun blankRanges(text: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return text
        val sb = StringBuilder(text)
        for (r in ranges) {
            val a = r.first.coerceAtLeast(0)
            val b = r.last.coerceAtMost(sb.length - 1)
            for (i in a..b) sb[i] = ' '
        }
        return sb.toString()
    }

    /** name({...}) veya name("tek argüman") veya name() tarzı */
    private fun extractFuncStyle(text: String, ranges: MutableList<IntRange>): List<Intent> {
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
                            ranges.add(i..end)
                            from = end + 1
                        } else from = j + 1
                    }
                    '"', '\'' -> {
                        val q = text[j]
                        val k = text.indexOf(q, j + 1)
                        if (k > j) {
                            out.add(Intent(name, singleArg(name, text.substring(j + 1, k))))
                            ranges.add(i..k)
                            from = k + 1
                        } else from = j + 1
                    }
                    ')' -> {
                        out.add(Intent(name, "{}"))
                        ranges.add(i..j)
                        from = j + 1
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
