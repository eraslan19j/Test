package com.redhawk.code.data.skills

data class Skill(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val prompt: String
)

object SkillCatalog {
    val all = listOf(
        Skill(
            id = "design-taste",
            title = "design-taste-frontend",
            badge = "ANTI-SLOP",
            description = "Sıradan AI şablonlarını engelleyen frontend tasarım motoru",
            prompt = "Frontend tasarımı yaparken sıradan (generic) AI şablonlarından kaç. " +
                "Amaçlı boşluk, özgün tipografi ve net hiyerarşi kullan."
        ),
        Skill(
            id = "full-output",
            title = "full-output-enforcement",
            badge = "KOD GÜVENLİĞİ",
            description = "LLM kod kesintisi ve '// rest of code' yasaklayan tam çıktı direktifi",
            prompt = "Kod üretirken asla '// rest of code', '...' veya kesinti kullanma. " +
                "Her zaman tam, çalışabilir kod üret."
        ),
        Skill(
            id = "gpt-taste",
            title = "gpt-taste",
            badge = "ANİMASYON & GSAP",
            description = "GSAP ScrollTrigger, gapless bento grid ve editorial tipografi",
            prompt = "Animasyon istendiğinde GSAP ScrollTrigger (pinning, stacking, scrubbing), " +
                "gapless bento grid ve editorial tipografi tercih et."
        ),
        Skill(
            id = "high-end-visual",
            title = "high-end-visual-design",
            badge = "LÜKS AJANS",
            description = "Lüks ajans standardı font, boşluk, gölge ve kart mimarisi",
            prompt = "Lüks ajans standardı: geniş boşluk, ince font, yumuşak gölge, " +
                "minimal kart mimarisi."
        ),
        Skill(
            id = "image-to-code",
            title = "image-to-code",
            badge = "GÖRSEL MOTOR",
            description = "Referans görselleri yüksek sadakatle koda döken görsel motor",
            prompt = "Verilen referans görselin yapısını, hiyerarşisini ve görsel dilini " +
                "yüksek sadakatle koda çevir."
        ),
        Skill(
            id = "industrial-brutalist",
            title = "industrial-brutalist-ui",
            badge = "BRUTALIST",
            description = "İsviçre tipografisi + terminal estetiği brutalism UI motoru",
            prompt = "İsviçre grid sistemi, monospace font, ham kenarlar, terminal estetiği."
        ),
        Skill(
            id = "minimalist",
            title = "minimalist-ui",
            badge = "MİNİMALİST",
            description = "Sıcak monokrom palet, düz bento grid, gölgesiz temiz editöryel arayüz",
            prompt = "Sıcak monokrom palet, düz bento grid, gölge yok, temiz editöryel tipografi."
        ),
        Skill(
            id = "redesign",
            title = "redesign-existing-project",
            badge = "DENETİM & REFACTOR",
            description = "Mevcut projeyi işlevselliği bozmadan ajans kalitesine yükselt",
            prompt = "Mevcut kodu refactor ederken işlevselliği koru, sadece görsel ve " +
                "yapısal kaliteyi yükselt."
        ),
        Skill(
            id = "stitch-taste",
            title = "stitch-design-taste",
            badge = "MİKRO-HAREKET",
            description = "Google Stitch anlamsal tasarım sistemi ve asimetrik mikro-hareket",
            prompt = "Anlamsal tasarım sistemi, asimetrik mikro-hareket, yumuşak geçişler."
        ),
        Skill(
            id = "debugger",
            title = "debugger",
            badge = "HATA AYIKLAMA",
            description = "Sistematik hata ayıklama: oku, izole et, hipotez, düzelt, doğrula",
            prompt = "Sistematik hata ayıklama: kodu oku, sorunu izole et, hipotez kur, " +
                "minimum değişiklikle düzelt, sonucu doğrula."
        ),
        Skill(
            id = "code-reviewer",
            title = "code-reviewer",
            badge = "KOD İNCELEME",
            description = "Kod kalite denetçisi: güvenlik, okunabilirlik, performans",
            prompt = "Kod incelemesi: güvenlik açıkları, okunabilirlik, performans ve " +
                "yapı sorunlarını vurgula. Somut öneri ver."
        ),
        Skill(
            id = "tester",
            title = "tester",
            badge = "TEST",
            description = "Otomatik test üreticisi (unit + integration)",
            prompt = "Test üretirken edge case'leri, hata yollarını ve sınırları test et."
        ),
    )

    fun byId(id: String): Skill? = all.firstOrNull { it.id == id }
}
