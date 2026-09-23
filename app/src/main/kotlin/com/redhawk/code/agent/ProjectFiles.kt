package com.redhawk.code.agent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Ajanın dosya erişim katmanı.
 *
 * İki kök desteklenir:
 *  - Proje klasörü seçildiyse (SAF ağaç izni): oraya çalışır.
 *  - Seçilmediyse: uygulamanın özel "workspace" klasörüne çalışır (izin gerekmez,
 *    dosyalara başka uygulama erişemez ama ajan tam yetkilidir).
 *
 * Güvenlik: ".." ve mutlak yol kaçışları engellenir, boyut üst sınırları vardır.
 */
object ProjectFiles {

    const val MAX_READ_CHARS = 60_000
    const val MAX_FILE_BYTES = 300_000L

    sealed interface Root {
        data class Saf(val treeUri: Uri) : Root
        data class Local(val dir: File) : Root
    }

    fun resolveRoot(ctx: Context, projectUri: String?): Root {
        if (!projectUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(projectUri)
                val doc = DocumentFile.fromTreeUri(ctx, uri)
                if (doc != null && doc.isDirectory) return Root.Saf(uri)
            } catch (_: Throwable) { /* yedeğe düş */ }
        }
        return localRoot(ctx)
    }

    private fun localRoot(ctx: Context): Root.Local {
        val dir = File(ctx.filesDir, "workspace").apply { mkdirs() }
        return Root.Local(dir)
    }

    fun displayName(ctx: Context, projectUri: String?): String {
        if (projectUri.isNullOrBlank()) return "Uygulama deposu"
        return try {
            DocumentFile.fromTreeUri(ctx, Uri.parse(projectUri))?.name ?: "Seçili klasör"
        } catch (_: Throwable) { "Seçili klasör" }
    }

    /** ".." ve aşırı derinlik engelle; null = geçersiz yol */
    private fun safeSegments(path: String): List<String>? {
        val segs = path.replace('\\', '/').trim().trim('/').split('/')
            .filter { it.isNotBlank() && it != "." }
        if (segs.any { it == ".." }) return null
        if (segs.size > 20) return null
        return segs
    }

    fun list(ctx: Context, projectUri: String?, path: String): String {
        val segs = safeSegments(path) ?: return "HATA: geçersiz yol."
        return when (val root = resolveRoot(ctx, projectUri)) {
            is Root.Local -> {
                var dir = root.dir
                for (s in segs) dir = File(dir, s)
                if (!dir.exists()) return "HATA: '$path' bulunamadı."
                if (!dir.isDirectory) return "HATA: '$path' bir klasör değil."
                val items = dir.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                    ?: return "HATA: listelenemedi."
                if (items.isEmpty()) return "(boş klasör)"
                items.joinToString("\n") { f ->
                    (if (f.isDirectory) "[D] " else "[F] ${f.length()}b ") + f.name
                }
            }
            is Root.Saf -> {
                var dir = DocumentFile.fromTreeUri(ctx, root.treeUri)
                    ?: return "HATA: proje klasörüne erişilemiyor (izin kalkmış olabilir)."
                for (s in segs) {
                    dir = dir.findFile(s) ?: return "HATA: '$path' bulunamadı."
                    if (!dir.isDirectory) return "HATA: '$path' bir klasör değil."
                }
                val items = dir.listFiles()
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name ?: "" }))
                if (items.isEmpty()) return "(boş klasör)"
                items.joinToString("\n") { f ->
                    (if (f.isDirectory) "[D] " else "[F] ${f.length()}b ") + (f.name ?: "?")
                }
            }
        }
    }

    fun read(ctx: Context, projectUri: String?, path: String): String {
        val segs = safeSegments(path)
        if (segs.isNullOrEmpty()) return "HATA: geçersiz yol."
        return when (val root = resolveRoot(ctx, projectUri)) {
            is Root.Local -> {
                var f = root.dir
                for (s in segs) f = File(f, s)
                if (!f.exists() || !f.isFile) return "HATA: '$path' bulunamadı."
                if (f.length() > MAX_FILE_BYTES) return "HATA: dosya çok büyük (${f.length()} bayt)."
                val text = runCatching { f.readText() }
                    .getOrElse { return "HATA: okunamadı: ${it.message}" }
                truncate(text)
            }
            is Root.Saf -> {
                var dir = DocumentFile.fromTreeUri(ctx, root.treeUri)
                    ?: return "HATA: proje klasörüne erişilemiyor (izin kalkmış olabilir)."
                for (s in segs.dropLast(1)) {
                    dir = dir.findFile(s) ?: return "HATA: '$path' bulunamadı."
                }
                val file = dir.findFile(segs.last()) ?: return "HATA: '$path' bulunamadı."
                if (file.isDirectory) return "HATA: '$path' bir klasör."
                if (file.length() > MAX_FILE_BYTES) return "HATA: dosya çok büyük."
                val text = runCatching {
                    ctx.contentResolver.openInputStream(file.uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }.getOrElse { return "HATA: okunamadı: ${it.message}" }
                    ?: return "HATA: dosya açılamadı."
                truncate(text)
            }
        }
    }

    fun write(ctx: Context, projectUri: String?, path: String, content: String): String {
        val segs = safeSegments(path)
        if (segs.isNullOrEmpty()) return "HATA: geçersiz yol."
        if (content.length > MAX_FILE_BYTES) return "HATA: içerik çok büyük (max 300KB)."
        return when (val root = resolveRoot(ctx, projectUri)) {
            is Root.Local -> {
                var dir = root.dir
                for (s in segs.dropLast(1)) {
                    dir = File(dir, s)
                    dir.mkdirs()
                }
                val f = File(dir, segs.last())
                val existed = f.exists()
                runCatching { f.writeText(content) }
                    .getOrElse { return "HATA: yazılamadı: ${it.message}" }
                if (existed) "GÜNCELLENDİ: $path (${content.length} karakter)"
                else "OLUŞTURULDU: $path (${content.length} karakter)"
            }
            is Root.Saf -> {
                var dir = DocumentFile.fromTreeUri(ctx, root.treeUri)
                    ?: return "HATA: proje klasörüne erişilemiyor (izin kalkmış olabilir)."
                for (s in segs.dropLast(1)) {
                    dir = dir.findFile(s) ?: dir.createDirectory(s)
                        ?: return "HATA: klasör oluşturulamadı: $s"
                }
                val name = segs.last()
                val existing = dir.findFile(name)
                if (existing != null && existing.isDirectory) return "HATA: '$path' bir klasör."
                val target = existing
                    ?: dir.createFile(mimeFor(name), name)
                    ?: return "HATA: dosya oluşturulamadı."
                runCatching {
                    ctx.contentResolver.openOutputStream(target.uri, "w")?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("akış açılamadı")
                }.getOrElse { return "HATA: yazılamadı: ${it.message}" }
                if (existing != null) "GÜNCELLENDİ: $path (${content.length} karakter)"
                else "OLUŞTURULDU: $path (${content.length} karakter)"
            }
        }
    }

    fun delete(ctx: Context, projectUri: String?, path: String): String {
        val segs = safeSegments(path)
        if (segs.isNullOrEmpty()) return "HATA: geçersiz yol."
        return when (val root = resolveRoot(ctx, projectUri)) {
            is Root.Local -> {
                var f = root.dir
                for (s in segs) f = File(f, s)
                if (!f.exists()) return "HATA: '$path' bulunamadı."
                val ok = runCatching { if (f.isDirectory) f.deleteRecursively() else f.delete() }
                    .getOrDefault(false)
                if (ok) "SİLİNDİ: $path" else "HATA: silinemedi."
            }
            is Root.Saf -> {
                var dir = DocumentFile.fromTreeUri(ctx, root.treeUri)
                    ?: return "HATA: proje klasörüne erişilemiyor (izin kalkmış olabilir)."
                for (s in segs.dropLast(1)) {
                    dir = dir.findFile(s) ?: return "HATA: '$path' bulunamadı."
                }
                val target = dir.findFile(segs.last()) ?: return "HATA: '$path' bulunamadı."
                if (runCatching { target.delete() }.getOrDefault(false)) "SİLİNDİ: $path"
                else "HATA: silinemedi."
            }
        }
    }

    fun move(ctx: Context, projectUri: String?, from: String, to: String): String {
        val fSegs = safeSegments(from)
        val tSegs = safeSegments(to)
        if (fSegs.isNullOrEmpty() || tSegs.isNullOrEmpty()) return "HATA: geçersiz yol."
        if (fSegs == tSegs) return "HATA: kaynak ve hedef aynı."
        return when (val root = resolveRoot(ctx, projectUri)) {
            is Root.Local -> {
                var src = root.dir
                for (s in fSegs) src = File(src, s)
                if (!src.exists()) return "HATA: '$from' bulunamadı."
                var dstDir = root.dir
                for (s in tSegs.dropLast(1)) {
                    dstDir = File(dstDir, s)
                    dstDir.mkdirs()
                }
                val dst = File(dstDir, tSegs.last())
                if (dst.exists()) return "HATA: hedef zaten var: '$to'."
                if (src.renameTo(dst)) "TAŞINDI: $from → $to"
                else "HATA: taşınamadı."
            }
            is Root.Saf -> {
                val tree = DocumentFile.fromTreeUri(ctx, root.treeUri)
                    ?: return "HATA: proje klasörüne erişilemiyor (izin kalkmış olabilir)."
                var srcParent = tree
                for (s in fSegs.dropLast(1)) {
                    srcParent = srcParent.findFile(s)
                        ?: return "HATA: '$from' bulunamadı."
                }
                val src = srcParent.findFile(fSegs.last())
                    ?: return "HATA: '$from' bulunamadı."
                var dstParent = tree
                for (s in tSegs.dropLast(1)) {
                    dstParent = dstParent.findFile(s) ?: dstParent.createDirectory(s)
                        ?: return "HATA: klasör oluşturulamadı: $s"
                }
                val dstName = tSegs.last()
                if (dstParent.findFile(dstName) != null) return "HATA: hedef zaten var: '$to'."
                // Aynı klasörse yeniden adlandır (hızlı); değilse kopyala-sil
                if (srcParent.uri == dstParent.uri) {
                    if (runCatching { src.renameTo(dstName) != null }.getOrDefault(false))
                        "YENİDEN ADLANDIRILDI: $from → $to"
                    else "HATA: yeniden adlandırılamadı."
                } else {
                    val copied = runCatching {
                        copySaf(ctx, src, dstParent, dstName, longArrayOf(0))
                    }.getOrDefault(false)
                    if (!copied) return "HATA: kopyalanamadı (20MB sınırı aşılmış olabilir)."
                    runCatching { src.delete() }
                    "TAŞINDI: $from → $to (kopyala-sil)"
                }
            }
        }
    }

    private const val MOVE_MAX_BYTES = 20_000_000L

    /** SAF kopyalama (bayt bayt; metin/binary fark etmez). total[0] = sayaç. */
    private fun copySaf(
        ctx: Context, src: DocumentFile, dstParent: DocumentFile,
        name: String, total: LongArray
    ): Boolean {
        if (src.isDirectory) {
            val dir = dstParent.createDirectory(name) ?: return false
            for (c in src.listFiles()) {
                if (!copySaf(ctx, c, dir, c.name ?: "?", total)) return false
            }
            return true
        }
        if (src.length() + total[0] > MOVE_MAX_BYTES) return false
        val target = dstParent.createFile(mimeFor(name), name) ?: return false
        ctx.contentResolver.openInputStream(src.uri)?.use { inp ->
            ctx.contentResolver.openOutputStream(target.uri, "w")?.use { out ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = inp.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    total[0] += n
                    if (total[0] > MOVE_MAX_BYTES) return false
                }
            } ?: return false
        } ?: return false
        return true
    }

    fun chmod(ctx: Context, projectUri: String?, path: String, mode: String): String {
        if (!Regex("^[0-7]{3}$").matches(mode)) return "HATA: kip 3 basamak olmalı (örn: 755)."
        val segs = safeSegments(path)
        if (segs.isNullOrEmpty()) return "HATA: geçersiz yol."
        val root = resolveRoot(ctx, projectUri)
        if (root !is Root.Local) return "HATA: SAF klasöründe chmod yok (yalnızca uygulama deposu)."
        var f = root.dir
        for (s in segs) f = File(f, s)
        if (!f.exists()) return "HATA: '$path' bulunamadı."
        val owner = mode[0].digitToInt()
        val r = owner and 4 != 0
        val w = owner and 2 != 0
        val x = owner and 1 != 0
        val ok = runCatching {
            f.setReadable(r, true) && f.setWritable(w, true) && f.setExecutable(x, true)
        }.getOrDefault(false)
        return if (ok) "CHMOD: $path → $mode (sahip: " +
            (if (r) "r" else "-") + (if (w) "w" else "-") + (if (x) "x" else "-") + ")"
        else "HATA: chmod uygulanamadı."
    }

    private fun truncate(text: String): String =
        if (text.length > MAX_READ_CHARS) text.take(MAX_READ_CHARS) + "\n…(kesildi, dosyanın devamı var)"
        else text

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "json" -> "application/json"
        "xml" -> "text/xml"
        else -> "text/plain"
    }
}
