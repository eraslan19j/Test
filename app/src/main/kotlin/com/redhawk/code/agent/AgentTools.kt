package com.redhawk.code.agent

import android.content.Context
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ajana sunulan araçlar (Faz 1: dosya işlemleri).
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
