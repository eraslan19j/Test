package com.redhawk.code.ui.chat

/**
 * TÜRKÇE GARANTİSİ: model sistem direktifini yok sayıp İngilizce
 * yazsa bile, görünen cevap balonunda İngilizce cümle kalmaz.
 *
 * İngilizce cümleler SİLİNMEZ — düşünme paneline taşınır
 * (panel iş bitince kapalı durur, bilgi kaybolmaz).
 * Kod blokları (```...```), satır-içi kod, yol ve URL'ler korunur.
 *
 * Sadece yanıt dili Türkçe iken uygulanır (VM'de turkishOnly).
 */
object TurkishGuard {

    data class Result(val thinking: String, val response: String)

    const val FALLBACK =
        "Model Türkçe yanıt üretemedi. Ham çıktıyı düşünme panelinden görebilirsin."

    private val TR_CHARS = setOf('ç', 'Ç', 'ğ', 'Ğ', 'ı', 'İ', 'ö', 'Ö', 'ş', 'Ş', 'ü', 'Ü')
    private val SENT_SPLIT = Regex("(?<=[.!?…])\\s+|(?<=\\.)(?=[A-ZÇĞİÖŞÜ])")
    private const val FENCE = "```"

    /** Küçük modellerin kendi-kendine-konuşma girişleri */
    private val SELF_TALK_START = listOf(
        "the user ask", "the user says", "the user wants", "the user wrote",
        "the user is asking", "user asks", "they want", "as an ai",
        "as a language model", "we should", "we can", "we must", "we need",
        "we don't have", "we do not have", "we cannot", "we have to",
        "i should", "i must", "i need", "i will", "i'll", "i am going",
        "let's", "let us", "according to policy", "the instructions say",
        "the developer says", "my response", "in this response",
        "okay,", "certainly", "first,", "to answer", "the assistant",
        "based on", "in order to", "it seems", "this means", "that means",
        "here is", "here are", "there is", "there are", "the output",
        "we don't", "we do not", "i don't", "i do not"
    )

    /** Tek kelimelik İngilizce yanıtlar (cevap boşalırsa FALLBACK devreye girer) */
    private val LONE_ENGLISH = setOf(
        "done", "ok", "okay", "yes", "no", "sure", "hello", "hi",
        "thanks", "thank you", "of course", "all done"
    )

    /**
     * Yaygın İngilizce kelimeler — bilinçli DAR tutuldu:
     * Türkçe teknik cümlelerde geçen write/file/path/use/make gibi
     * kelimeler YOK (yoksa Türkçe cümleler yanlış yakalanır).
     */
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
        "your", "our", "ours", "its", "their", "ourselves", "there", "here",
        "cannot", "going", "like", "well", "much", "many", "some", "any",
        "each", "both", "few", "own", "same", "too", "can", "will", "don",
        "doesn", "isn", "aren", "wasn", "weren", "haven", "hasn", "hadn",
        "won", "wouldn", "shouldn", "couldn", "mustn", "didn"
    )

    fun enforce(thinking: String, response: String): Result {
        if (response.isBlank()) return Result(thinking, response)
        val moved = mutableListOf<String>()
        val out = StringBuilder()
        // ``` bloklarını koru (tek sayılı parçalar koddur; kapanmamış
        // blok akış sırasında kod sayılır)
        val parts = response.split(FENCE)
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
            if (isEnglish(t)) moved.add(t) else sb.append(sent)
        }
        return sb.toString()
    }

    private fun isEnglish(s: String): Boolean {
        val bare = s.trim()
            .removePrefix("- ").removePrefix("* ").removePrefix("> ").trim()
        if (bare.isEmpty()) return false
        // Türkçe karakter varsa Türkçe'dir (en güçlü sinyal)
        if (bare.any { it in TR_CHARS }) return false
        val low = bare.lowercase().trimEnd('.', '!', '?', '…', ' ')
        if (low in LONE_ENGLISH) return true
        // Kod/yol/URL kokan satırlara dokunma
        if (bare.contains("http") || bare.contains("://") || bare.contains('`')) return false
        if (bare.contains('/') || bare.contains('\\')) return false
        if (bare.count { it in "(){}[];=<>|\"" } >= 2) return false
        val letters = bare.filter { it.isLetter() }
        if (letters.length < 6) return false
        // Tırnak içi kullanıcı alıntısını yoksay
        val dequoted = low.replace(Regex("\"[^\"]*\""), "")
        if (SELF_TALK_START.any { dequoted.startsWith(it) }) return true
        val tokens = low.split(Regex("[^a-zçğıöşü]+")).filter { it.length >= 2 }
        val enCount = tokens.count { it in EN_WORDS }
        return enCount >= 2
    }
}
