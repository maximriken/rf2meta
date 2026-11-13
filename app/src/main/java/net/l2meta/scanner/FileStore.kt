package net.l2meta.scanner

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/**
 * Збереження файлів у «Завантаження/L2Meta» (без додаткових дозволів).
 */
object FileStore {

    private val FOLDER = Environment.DIRECTORY_DOWNLOADS + "/L2Meta"

    fun savePng(ctx: Context, bitmap: Bitmap, baseName: String): Uri =
        save(ctx, "$baseName.png", "image/png") { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

    fun saveCsv(ctx: Context, fileName: String, content: String): Uri =
        save(ctx, fileName, "text/csv") { out ->
            // BOM, щоб і Excel правильно показував кирилицю. Google Таблиці його просто ігнорують.
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(content.toByteArray(Charsets.UTF_8))
        }

    private fun save(ctx: Context, name: String, mime: String, write: (OutputStream) -> Unit): Uri {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, FOLDER)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("не вдалося створити файл $name")
        try {
            resolver.openOutputStream(uri)?.use(write) ?: error("не вдалося відкрити файл $name")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }
}
