package com.redhawk.code.agent

import android.content.Context
import com.redhawk.code.llm.model.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ajana sunulan araçlar (9 araç: dosyalar + terminal + internet).
 * Yeni araç eklemek = listeye ToolSpec + execute dalı +
 * ToolIntentParser.TOOL_NAMES'e ad + PermissionCatalog eşleşmesi.
 * Yıkıcı araçlar (delete/move/chmod) doğal-dille tetiklenmez,
 * sadece <tool>/fonksiyon formatıyla gelir (güvenlik).
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
            name = "delete_file",
            description = "Bir dosya veya klasörü SİLER (klasörse içiyle birlikte). Geri alınamaz! Bu işlem kullanıcı onayından geçer.",
            parametersJsonSchema = """{"type":"object","properties":{"path":{"type":"string","description":"Silinecek dosya/klasör yolu"}},"required":["path"]}"""
        ),
        ToolSpec(
            name = "move_file",
            description = "Bir dosya/klasörü taşır veya yeniden adlandırır (from -> to). Hedefin klasörleri oluşturulur. Bu işlem kullanıcı onayından geçer.",
            parametersJsonSchema = """{"type":"object","properties":{"from":{"type":"string","description":"Kaynak yol"},"to":{"type":"string","description":"Hedef yol"}},"required":["from","to"]}"""
        ),
        ToolSpec(
            name = "chmod_file",
            description = "Bir dosyanın POSIX iznini değiştirir (yalnızca uygulama deposu; SAF'ta çalışmaz). mode 3 basamaklıdır (örn: 755). Bu işlem kullanıcı onayından geçer.",
            parametersJsonSchema = """{"type":"object","properties":{"path":{"type":"string","description":"Dosya yolu"},"mode":{"type":"string","description":"3 basamaklı kip, örn: '755'"}},"required":["path","mode"]}"""
        ),
        ToolSpec(
            name = "run_command",
            description = "Uygulama deposunda salt-okunur komut çalıştırır: ls, cat, head, tail, find, grep, wc, du, stat, file, echo, pwd, uname, date, git (status/log/diff/branch). Shell yok, pipe/yönlendirme yok. Bağlı klasörde (SAF) çalışmaz.",
            parametersJsonSchema = """{"type":"object","properties":{"command":{"type":"string","description":"Çalıştırılacak komut, örn: 'ls -la', 'grep -r parola src'"}},"required":["command"]}"""
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
        )
    )

    /** true = çalıştırmadan önce kullanıcı onayı gerekir (777 hariç) */
    fun needsApproval(toolName: String): Boolean =
        toolName == "write_file" || toolName == "delete_file" ||
            toolName == "move_file" || toolName == "chmod_file"

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
            "delete_file" -> {
                val p = arg(argsJson, "path")
                if (p.isBlank()) "HATA: path gerekli." else ProjectFiles.delete(ctx, projectUri, p)
            }
            "move_file" -> {
                val f = arg(argsJson, "from")
                val t = arg(argsJson, "to")
                if (f.isBlank() || t.isBlank()) "HATA: from ve to gerekli."
                else ProjectFiles.move(ctx, projectUri, f, t)
            }
            "chmod_file" -> {
                val p = arg(argsJson, "path")
                val m = arg(argsJson, "mode")
                if (p.isBlank() || m.isBlank()) "HATA: path ve mode gerekli (örn: 755)."
                else ProjectFiles.chmod(ctx, projectUri, p, m)
            }
            "run_command" -> {
                val c = arg(argsJson, "command")
                if (c.isBlank()) "HATA: command gerekli."
                else if (!projectUri.isNullOrBlank()) "HATA: run_command bu klasörde ÇALIŞMAZ (bağlı klasör SAF üzerinden erişiliyor). Bunun yerine MUTLAKA list_files aracını kullan: <tool name=\"list_files\">{\"path\": \"\"}</tool>"
                else Terminal.run(ctx, c)
            }
            "web_search" -> {
                val q = arg(argsJson, "query")
                if (q.isBlank()) "HATA: query gerekli." else WebTools.search(q)
            }
            "fetch_url" -> {
                val u = arg(argsJson, "url")
                if (u.isBlank()) "HATA: url gerekli." else WebTools.fetch(u)
            }
            else -> "HATA: bilinmeyen araç '$name'."
        }
    }

    /** Onay diyaloğunda gösterilecek özet (write_file'da gerçek diff) */
    suspend fun preview(
        ctx: Context, projectUri: String?, name: String, argsJson: String
    ): String = withContext(Dispatchers.IO) {
        when (name) {
            "write_file" -> {
                val p = arg(argsJson, "path").ifBlank { "(yol yok)" }
                val c = arg(argsJson, "content")
                val old = runCatching { ProjectFiles.read(ctx, projectUri, p) }.getOrNull() ?: ""
                if (old.startsWith("HATA")) {
                    "YENİ DOSYA: $p\nBoyut: ${c.length} karakter\n\n${c.take(600)}"
                } else {
                    "Dosya: $p\n${smallDiff(old, c)}"
                }
            }
            "delete_file" -> {
                val p = arg(argsJson, "path").ifBlank { "(yol yok)" }
                "⚠ SİLİNECEK: $p\nBu işlem geri alınamaz!"
            }
            "move_file" -> "TAŞINACAK: ${arg(argsJson, "from").ifBlank { "?" }} → " +
                arg(argsJson, "to").ifBlank { "?" }
            "chmod_file" -> "CHMOD: ${arg(argsJson, "path").ifBlank { "?" }} → " +
                arg(argsJson, "mode").ifBlank { "?" }
            "run_command" -> "KOMUT: ${arg(argsJson, "command").take(600)}"
            else -> argsJson.take(600)
        }
    }

    /** Baştan/sondan ortak satırları atlayan minik diff */
    private fun smallDiff(old: String, new: String): String {
        val a = old.lines()
        val b = new.lines()
        var h = 0
        while (h < a.size && h < b.size && a[h] == b[h]) h++
        var ta = a.size - 1
        var tb = b.size - 1
        while (ta > h && tb > h && a[ta] == b[tb]) { ta--; tb-- }
        val sb = StringBuilder("Fark ${h + 1}. satırdan itibaren:\n")
        var n = 0
        for (i in h..ta) {
            if (i >= a.size || n >= 30) break
            sb.append("- ").appendLine(a[i].take(200)); n++
        }
        for (i in h..tb) {
            if (i >= b.size || n >= 60) break
            sb.append("+ ").appendLine(b[i].take(200)); n++
        }
        if ((ta - h) + (tb - h) > n) sb.append("…(devamı var)")
        return sb.toString().take(2000)
    }
}
