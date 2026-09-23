package com.redhawk.code.agent

enum class PermissionProfile(val label: String, val icon: String) {
    SAFE("Güvenli", "🛡"),
    STANDARD("Standart", "⚡"),
    FULL("Tam (777)", "🚀")
}

data class PermissionItem(
    val id: String,
    val icon: String,
    val title: String,
    val subtitle: String
)

object PermissionCatalog {

    /** chmod bu değerdeyse onay diyalogları atlanır (AI otomatik devam eder) */
    const val FULL_AUTO_CHMOD = "777"

    val items = listOf(
        PermissionItem("read", "📁", "Dosyaları oku ve ara",
            "Ajan'ın proje dosyalarını incelemesine ve kod içinde arama yapmasına izin ver."),
        PermissionItem("write", "📄", "Dosya/klasör oluştur ve düzenle",
            "Proje içinde yeni dosya, klasör ve kod değişiklikleri oluşturmasına izin ver."),
        PermissionItem("delete", "🗑", "Dosya/klasör sil",
            "Ajan'ın projedeki dosya veya klasörleri kaldırmasına izin ver."),
        PermissionItem("move", "📋", "Taşı ve yeniden adlandır",
            "Proje yapısını koruyarak yolları taşımasına veya yeniden adlandırmasına izin ver."),
        PermissionItem("chmod", "🛡", "POSIX izinlerini değiştir",
            "Proje dosyaları ve çalıştırılabilir araçlar için chmod değişikliklerine izin ver."),
        PermissionItem("terminal", "⌨", "Terminal erişimi (chmod)",
            "Onaylanan komutları aktif proje terminalinde çalıştırmasına izin ver.")
    )

    /** Profile göre hangi izinler açık */
    fun defaultFor(profile: PermissionProfile): Set<String> = when (profile) {
        PermissionProfile.SAFE -> setOf("read")
        PermissionProfile.STANDARD -> setOf("read", "write", "move")
        PermissionProfile.FULL -> items.map { it.id }.toSet()
    }

    /** Araç -> gereken izin (null = izin gerekmez: internet araçları) */
    fun permissionFor(toolName: String): String? = when (toolName) {
        "list_files", "read_file" -> "read"
        "write_file" -> "write"
        "delete_file" -> "delete"
        "move_file" -> "move"
        "chmod_file" -> "chmod"
        "run_command" -> "terminal"
        else -> null
    }
}
