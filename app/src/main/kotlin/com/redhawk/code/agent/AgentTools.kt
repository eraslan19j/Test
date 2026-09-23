package com.redhawk.code.agent

import android.content.Context
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ajana sunulan araçlar (Faz 2: dosyalar + internet).
 * Yeni araç eklemek = listeye ToolSpec + execute dalı eklemek.
 */
object AgentTools {

    val specs: List<ToolSpec> = listOf(
        ToolSpec(
            name = "list_files",
            description = "Çalışma klasöründeki dosya ve klasörleri listeler. path boş bırakılırsa kök listelenir.",
            parametersJsonSchema = """{"type":"object","properties":{"path":{"type":"string","description":"Klasör yolu, örn: '', 'src', 'app/src'"}},"required":[]}"""
        ),
        ToolSpec(
            name = "read_file",
            description = "Bir metin dosyasının içeriğini okur (en fazla 60 bin karakter).",
            parametersJsonSchema = """{"type":"object","properties":{"path":{"type":"string","description":"Dosya yolu, örn: 'Main.kt', 'app/build.gradle'"}},"required":["path"]}"""
        ),
        ToolSpec(
            name = "write_file",
            description = "Bir dosyaya metin yazar. Dosya yoksa gerekli klasörlerle birlikte oluşturur, varsa ÜZERİNE YAZAR. content dosyanın TAM yeni içeriği olmalıdır. Bu işlem kullanıcı onayından geçer.",
            parametersJsonSchema = """{"type":"object","properties":{"path":{"type":"string","description":"Dosya yolu"},"content":{"type":"string","description":"Dosyanın tam yeni içeriği"}},"required":["path","content"]}"""
        ),
        ToolSpec(
            name = "web_search",
            description = "İnternette arama yapar (DuckDuckGo, ücretsiz). Güncel bilgi, dokümantasyon, hata çözümleri için kullan. İlk 5 sonucu döndürür.",
            parametersJsonSchema = """{"type":"object","properties":{"query":{"type":"string","description":"Aranacak metin"}},"required":["query"]}"""
        ),
        ToolSpec(
            name = "fetch_url",
            description = "Bir web sayfasını okuyup düz metne çevirir (en fazla 8000 karakter). Dokümantasyon ve makale okumak için kullan.",
            parametersJsonSchema = """{"type":"object","properties":{"url":{"type":"string","description":"http(s) ile başlayan sayfa adresi"}},"required":["url"]}"""
        ),
        ToolSpec(
            name = "run_command",
            description = "Uygulama deposunda salt-okunur komut çalıştırır: ls, cat, head, tail, find, grep, wc, du, stat, file, echo, pwd, uname, date, git (status/log/diff/branch). Shell yok, pipe/yönlendirme yok. Bağlı klasörde (SAF) çalışmaz; orada list_files/read_file kullan.",
            parametersJsonSchema = """{"type":"object","properties":{"command":{"type":"string","description":"Çalıştırılacak komut, örn: 'ls -la', 'grep -r parola src'"}},"required":["command"]}"""
        )
    )

    /** true = çalıştırmadan önce kullanıcı onayı gerekir */
    fun needsApproval(toolName: String): Boolean = toolName == "write_file"

    fun arg(argsJson: String, key: String): String = try {
        kotlinx.serialization.json.Json.parseToJsonElement(argsJson)
            .jsonObject[key]?.jsonPrimitive?.content ?: ""
    } catch (_: Throwable) { "" }

    suspend fun execute(ctx: Context, projectUri: String?, name: String, argsJson: String): String {
        return when (name) {
            "list_files" -> ProjectFiles.list(ctx, projectUri, arg(argsJson, "path"))
            "read_file" -> {
                val p = arg(argsJson, "path")
                if (p.isBlank()) "HATA: path gerekli." else ProjectFiles.read(ctx, projectUri, p)
            }
            "write_file" -> {
                val p = arg(argsJson, "path")
                if (p.isBlank()) "HATA: path gerekli."
                else ProjectFiles.write(ctx, projectUri, p, arg(argsJson, "content"))
            }
            "web_search" -> {
                val q = arg(argsJson, "query")
                if (q.isBlank()) "HATA: query gerekli." else WebTools.search(q)
            }
            "fetch_url" -> {
                val u = arg(argsJson, "url")
                if (u.isBlank()) "HATA: url gerekli." else WebTools.fetch(u)
            }
            "run_command" -> {
                val c = arg(argsJson, "command")
                if (c.isBlank()) "HATA: command gerekli."
                else if (!projectUri.isNullOrBlank()) "HATA: komutlar yalnızca uygulama deposunda çalışır. Bağlı klasörde list_files/read_file kullan."
                else Terminal.run(ctx, c)
            }
            else -> "HATA: bilinmeyen araç '$name'."
        }
    }

    /** Onay diyaloğunda gösterilecek özet */
    fun preview(name: String, argsJson: String): String = when (name) {
        "write_file" -> {
            val p = arg(argsJson, "path").ifBlank { "(yol yok)" }
            val c = arg(argsJson, "content")
            "Dosya: $p\nBoyut: ${c.length} karakter\n\nÖnizleme (ilk 600 karakter):\n${c.take(600)}"
        }
        else -> argsJson.take(600)
    }
}
