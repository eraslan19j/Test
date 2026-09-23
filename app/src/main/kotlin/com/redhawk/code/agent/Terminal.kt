package com.redhawk.code.agent

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Kısıtlı komut çalıştırıcı (ajan terminali).
 *
 * - SADECE salt-okunur komutlar (allowlist); shell yok, pipe/yönlendirme yok.
 * - SADECE uygulama deposunda çalışır — bağlı klasör (SAF ağacı) bir
 *   dosya yolu değildir, shell ile girilemez; orada list_files/read_file kullanılır.
 * - 10 sn zaman aşımı, 8KB çıktı sınırı, depo-dışı yol yasak.
 */
object Terminal {

    /** komut -> izinli alt-fiiller (null = alt-fiil kısıtı yok) */
    private val ALLOW = mapOf(
        "ls" to null, "cat" to null, "head" to null, "tail" to null,
        "echo" to null, "pwd" to null, "find" to null, "grep" to null,
        "wc" to null, "du" to null, "stat" to null, "file" to null,
        "uname" to null, "date" to null,
        "git" to setOf("status", "log", "diff", "branch")
    )
    private val FORBIDDEN = charArrayOf(
        ';', '|', '&', '`', '$', '(', ')', '<', '>', '\n', '\r', '\\', '"', '\''
    )
    const val MAX_OUT = 8000

    fun repoDir(ctx: Context): File =
        File(ctx.filesDir, "workspace").apply { mkdirs() }

    fun run(ctx: Context, command: String): String {
        val cmd = command.trim()
        if (cmd.isEmpty()) return "HATA: boş komut."
        if (cmd.length > 500) return "HATA: komut çok uzun (max 500)."
        if (FORBIDDEN.any { cmd.contains(it) }) {
            return "HATA: yasak karakter var (shell yok: ; | & ` \$ ( ) < > kabul edilmez)."
        }
        val parts = cmd.split(Regex("\\s+"))
        val bin = parts[0].substringAfterLast('/')
        val sub = ALLOW[bin]
            ?: return "HATA: '$bin' komutuna izin yok. " +
                "İzinliler: ${ALLOW.keys.sorted().joinToString(", ")}."
        if (sub != null) {
            val verb = parts.getOrNull(1)
                ?: return "HATA: git alt-komutu gerekli (status/log/diff/branch)."
            if (verb !in sub) {
                return "HATA: 'git $verb' yasak (salt-okunur: status, log, diff, branch)."
            }
        }
        if (parts.any {
                it == ".." || it.startsWith("../") || it.contains("/../") ||
                    it.startsWith("/") || it.startsWith("~")
            }
        ) {
            return "HATA: depo dışına çıkılamaz."
        }
        return try {
            val pb = ProcessBuilder(parts).directory(repoDir(ctx)).redirectErrorStream(true)
            val p = pb.start()
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                return "HATA: zaman aşımı (10 sn)."
            }
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            val clipped = if (out.length > MAX_OUT) out.take(MAX_OUT) + "\n…(kesildi)"
            else out
            if (clipped.isBlank()) "(çıktı yok, kod ${p.exitValue()})"
            else "kod ${p.exitValue()}:\n$clipped"
        } catch (t: Throwable) {
            "HATA: çalıştırılamadı: ${t.message}"
        }
    }
}
