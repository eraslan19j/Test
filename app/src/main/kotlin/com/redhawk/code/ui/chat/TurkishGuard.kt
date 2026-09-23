package com.redhawk.code.ui.chat

/**
 * TÜRKÇE GARANTİSİ (v2.1): model direktifi yok saysa bile cevap
 * balonunda İngilizce cümle kalmaz. İngilizce SİLİNMEZ — düşünme
 * paneline taşınır (panel kapalı durur, bilgi kaybolmaz).
 *
 * İki katman:
 *  1. BELGE düzeyi: kod dışı metnin TAMAMI İngilizceyse toptan taşı.
 *  2. CÜMLE düzeyi: karışık metinde İngilizce cümleleri ayıkla.
 *
 * Korunanlar: ``` kod blokları, satır-içi `kod`, URL, yol,
 * araç adları (TOOLX'e nötrlenir). Güçlü İngilizce sinyali
 * (önek veya skor≥3) kod-vetosunu deler.
 */
object TurkishGuard {

    data class Result(val thinking: String, val response: String)

    const val FALLBACK =
        "Model Türkçe yanıt üretemedi. Ham çıktıyı düşünme panelinden görebilirsin."

    private val TR_CHARS = setOf('ç', 'Ç', 'ğ', 'Ğ', 'ı', 'İ', 'ö', 'Ö', 'ş', 'Ş', 'ü', 'Ü')
    // Yapışık cümleler de bölünür: "summarize.- Çalışma..." gibi
    private val SENT_SPLIT = Regex("(?<=[.!?…])\\s+|(?<=[.!?…])[-–—]+\\s*|(?<=\\.)(?=[A-ZÇĞİÖŞÜ])")
    private const val FENCE = "```"
    private val TOOL_TOKEN =
        Regex("\\b(list_files|read_file|write_file|delete_file|move_file|chmod_file|web_search|fetch_url|run_command)\\b")
    private val INLINE_CODE = Regex("`[^`\\n]*`")

    /** Öz-anlatım: "`list_files` komutunu çalıştırdım, ..." (balonda görünmez) */
    private val META_NOUNS =
        "(komut\\w*|ara[cç]\\w*|list_files|read_file|write_file|delete_file|move_file|chmod_file|run_command|web_search|fetch_url)"
    private val META_VERBS =
        "(çalıştırdım|çalıştırıyorum|çalıştıracağım|çalıştırıldı|kullandım|kullanıyorum|kullanıldı|çağırdım|çağırıyorum|yürüttüm|yürütüyorum)"
    private val META_PREFIX_TR =
        Regex("^[^.!?…\\n]{0,80}?$META_NOUNS[^.!?…\\n]{0,30}?$META_VERBS\\s*,\\s*")
    private val META_FULL_TR =
        Regex("^[^.!?…\\n]{0,120}?$META_NOUNS[^.!?…\\n]{0,40}?$META_VERBS[\\s.\"”`'!]*$")

    /** Küçük modellerin kendi-kendine-konuşma girişleri */
    private val SELF_TALK_START = listOf(
        "the user ask", "the user says", "the user wants", "the user wrote",
        "the user is asking", "user asks", "they want", "as an ai",
        "as a language model", "we should", "we can", "we must", "we need",
        "we will", "we don't", "we do not", "we cannot", "we have to",
        "i should", "i must", "i need", "i will", "i'll", "i am going",
        "i can", "i am ", "i'm ", "i've ", "i have ", "i just ",
        "i don't", "i do not", "i will check", "let's", "let us", "let me",
        "according to", "the instructions say",
        "the developer says", "my response", "in this response",
        "okay,", "certainly", "first,", "to answer", "to list", "to read",
        "the assistant", "based on", "in order to", "it seems",
        "this means", "that means", "this will", "it will",
        "here is", "here are", "there is", "there are", "the output",
        "use tool", "using ", "of course", "all right", "you can",
        "the folder", "the file", "the files", "your folder",
        "the current", "current directory", "the directory",
        "in the current", "the path", "for example", "such as",
        "no problem", "checking ", "listing ", "reading ", "searching ",
        "the response shows", "so we can", "we can summarize",
        "then ", "i ran ", "i executed "
    )

    /** Tek başına İngilizce olan kısa yanıtlar */
    private val LONE_ENGLISH = setOf(
        "done", "ok", "okay", "yes", "no", "sure", "hello", "hi",
        "thanks", "thank you", "of course", "all done", "no problem"
    )

    /** Yaygın İngilizce kelimeler (araç adları önce TOOLX'e nötrlenir) */
    private val EN_WORDS = setOf(
        "the", "and", "for", "with", "from", "that", "this", "these", "those",
        "you", "your", "yours", "they", "them", "their", "theirs", "he", "she",
        "his", "her", "hers", "are", "was", "were", "been", "being",
        "have", "has", "had", "having", "will", "would", "should", "could",
        "shall", "might", "does", "what", "which", "when", "where", "then",
        "than", "also", "just", "about", "because", "while", "although",
        "however", "therefore", "please", "says", "said", "wants", "want",
        "according", "policy", "produce", "provide", "respond", "response",
        "answer", "following", "needs", "need", "between", "over", "after",
        "before", "very", "more", "most", "other", "such", "only", "into",
        "our", "ours", "its", "there", "here", "cannot", "going",
        "like", "well", "much", "many", "some", "any",
        "each", "both", "few", "own", "same", "too", "can", "don",
        "doesn", "isn", "aren", "wasn", "weren", "haven", "hasn", "hadn",
        "won", "wouldn", "shouldn", "couldn", "mustn", "didn",
        "use", "using", "used", "tool", "tools",
        "list", "lists", "listed", "listing", "file", "files",
        "folder", "folders", "directory", "directories",
        "path", "paths", "empty", "root", "shows", "show", "likely",
        "summarize", "summarized", "summary", "ran", "executed"
    )

    fun enforce(thinking: String, response: String): Result {
        if (response.isBlank()) return Result(thinking, response)
        val parts = response.split(FENCE)
        val moved = mutableListOf<String>()

        // Katman 1 — belge düzeyi: kod dışı her şey İngilizceyse toptan taşı
        val nonCode = parts.filterIndexed { i, _ -> i % 2 == 0 }.joinToString("\n")
        if (isEnglishDocument(nonCode)) {
            moved.add(nonCode.trim())
            val out = StringBuilder()
            for (i in parts.indices) {
                if (i > 0) out.append(FENCE)
                if (i % 2 == 1) out.append(parts[i])
            }
            val newThinking = (thinking + "\n" + moved.joinToString("\n")).trim()
            val kept = out.toString().trim()
            return Result(newThinking, if (kept.isBlank()) FALLBACK else kept)
        }

        // Katman 2 — cümle düzeyi: karışık metni ayıkla
        val out = StringBuilder()
        for (i in parts.indices) {
            if (i > 0) out.append(FENCE)
            val part = parts[i]
            if (i % 2 == 1) out.append(part)
            else out.append(filterText(part, moved))
        }
        val kept = out.toString().trim()
        if (moved.isEmpty()) return Result(thinking, response)
        val newThinking = (thinking + "\n" + moved.joinToString("\n")).trim()
        if (kept.isBlank()) return Result(newThinking, FALLBACK)
        return Result(newThinking, out.toString().trim())
    }

    private fun filterText(text: String, moved: MutableList<String>): String {
        if (text.isBlank()) return text
        val matches = SENT_SPLIT.findAll(text).toList()
        if (matches.isEmpty()) {
            val t = text.trim()
            return if (t.isNotEmpty() && isEnglish(t)) {
                moved.add(t); ""
            } else text
        }
        val bounds = (listOf(0) + matches.map { it.range.last + 1 } + text.length)
            .distinct().sorted()
        val sb = StringBuilder()
        for (i in 0 until bounds.size - 1) {
            val sent = text.substring(bounds[i], bounds[i + 1])
            val t = sent.trim()
            if (t.isEmpty()) {
                sb.append(sent); continue
            }
            // Öz-anlatım önce: "komutunu çalıştırdım" balonda görünmez.
            // (Eşleşme küçük harfte aranır, sıyırma orijinalden yapılır.)
            val lowTr = t.lowercase()
            if (META_FULL_TR.matches(lowTr)) {
                moved.add(t); continue
            }
            val metaPrefix = META_PREFIX_TR.find(lowTr)
            if (metaPrefix != null) {
                moved.add(t.substring(metaPrefix.range))
                val rest = t.substring(metaPrefix.range.last + 1).trim()
                if (rest.isNotEmpty()) {
                    if (isEnglish(rest)) moved.add(rest)
                    else { sb.append(rest); sb.append(" ") }
                }
                continue
            }
            if (isEnglish(t)) moved.add(t) else sb.append(sent)
        }
        return sb.toString()
    }

    private fun isEnglishDocument(t: String): Boolean {
        val s = INLINE_CODE.replace(t, " ").trim()
        if (s.isEmpty()) return false
        if (s.any { it in TR_CHARS }) return false
        if (s.filter { it.isLetter() }.length < 6) return false
        return looksEnglish(s)
    }

    private fun isEnglish(s: String): Boolean {
        val bare = s.trim()
            .removePrefix("- ").removePrefix("* ").removePrefix("> ").trim()
        if (bare.isEmpty()) return false
        // Türkçe karakter varsa Türkçe'dir (en güçlü sinyal)
        if (bare.any { it in TR_CHARS }) return false
        if (bare.filter { it.isLetter() }.length < 6) return false
        // Kod/yol/URL kokusu vetosu — ama güçlü sinyal vetoyu deler
        // ("The response shows three items (prefixed [D])." gibi)
        val vetoed = bare.contains("http") || bare.contains("://") ||
            bare.contains('`') || bare.contains('/') || bare.contains('\\') ||
            bare.count { it in "(){}[];=<>|\"" } >= 2
        val (prefix, score) = enScore(bare)
        if (vetoed) return prefix || score >= 3
        return prefix || score >= 2
    }

    private fun looksEnglish(s: String): Boolean {
        val (prefix, score) = enScore(s)
        return prefix || score >= 2
    }

    /** (önek-eşleşmesi, İngilizce-kelime-sayısı) */
    /** Türkçeye özgü ek kalıpları (kaba ama etkili sezgi) */
    private val TR_SUFFIX = Regex(
        "(lar|ler|dır|dir|dur|dür|tır|tir|tur|tür|mış|miş|muş|müş|" +
            "yor|acak|ecek|sın|sin|sun|sün|nız|niz|nuz|nüz|" +
            "de|da|te|ta|nin|nın|nun|nün|ki)$"
    )

    private fun enScore(s: String): Pair<Boolean, Int> {
        val neutral = TOOL_TOKEN.replace(s, "TOOLX")
        val low = neutral.lowercase()
            .replace(Regex("\"[^\"]*\""), " ")
            .replace(Regex("\\s+"), " ").trim()
        if (low.trimEnd('.', '!', '?', '…', ' ') in LONE_ENGLISH) return true to 99
        if (SELF_TALK_START.any { low.startsWith(it) }) return true to 99
        // Tırnak temizlenmeden ÖNCE de kontrol et: "User asks: "..."" gibi
        // cümleler tırnak-sıyırma sonrası başlangıcını kaybedebilir.
        val rawLow = neutral.lowercase().trim()
        if (SELF_TALK_START.any { rawLow.startsWith(it) }) return true to 99
        val tokens = low.split(Regex("[^a-zçğıöşü]+")).filter { it.length >= 2 }
        val wordScore = tokens.count { it in EN_WORDS }
        if (wordScore >= 2) return false to wordScore
        // Sözlük-dışı genel sezgi: 3+ kelimelik, Türkçe eki/karakteri
        // OLMAYAN cümleler büyük olasılıkla İngilizcedir.
        if (tokens.size >= 3) {
            val hasTrSuffix = tokens.any { TR_SUFFIX.containsMatchIn(it) }
            val hasTrChar = s.any { it in TR_CHARS }
            if (!hasTrSuffix && !hasTrChar) return true to 2
        }
        return false to wordScore
    }
}
