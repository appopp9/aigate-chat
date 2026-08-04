package com.aigate.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.aigate.chat.model.Attachment
import com.aigate.chat.model.AttachmentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object AttachmentUtils {

	private const val MAX_IMAGE_DIM = 1280
	private const val MAX_TEXT_CHARS = 120000
	private const val MAX_BINARY_BYTES = 12 * 1024 * 1024
	private const val MAX_ZIP_ENTRY_CHARS = 20000

	private val TEXT_EXTENSIONS = setOf(
		"txt", "md", "json", "csv", "tsv", "xml", "yml", "yaml", "html", "htm", "css",
		"js", "ts", "tsx", "jsx", "kt", "kts", "java", "py", "c", "cpp", "h", "hpp",
		"cs", "go", "rs", "rb", "php", "swift", "sh", "bat", "sql", "ini", "conf",
		"toml", "log", "gradle", "properties", "env", "dart", "vue", "svelte"
	)

	suspend fun fromUri(context: Context, uri: Uri): Attachment? = withContext(Dispatchers.IO) {
		try {
			val resolver = context.contentResolver
			val mime = resolver.getType(uri) ?: guessMime(uri.toString())
			var name = "file"
			var size = 0L
			val cursor = resolver.query(uri, null, null, null, null)
			if (cursor != null) {
				cursor.use { c ->
					val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
					val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
					if (c.moveToFirst()) {
						if (nameIdx >= 0) {
							val value = c.getString(nameIdx)
							if (value != null) name = value
						}
						if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
					}
				}
			}
			val ext = name.substringAfterLast('.', "").lowercase()

			// عکس
			if (mime.startsWith("image/")) {
				val dataUrl = imageToDataUrl(context, uri)
				return@withContext Attachment(
					name = name,
					mimeType = "image/jpeg",
					sizeBytes = size,
					kind = AttachmentKind.IMAGE,
					dataUrl = dataUrl,
					localUri = uri.toString(),
				)
			}

			// فایل ZIP — لیست و محتوای فایل‌های متنی درونش خوانده می‌شود
			if (ext == "zip" || mime == "application/zip" || mime == "application/x-zip-compressed") {
				val summary = readZip(context, uri)
				return@withContext Attachment(
					name = name,
					mimeType = "application/zip",
					sizeBytes = size,
					kind = AttachmentKind.FILE,
					textContent = summary,
					localUri = uri.toString(),
				)
			}

			// فایل متنی / کد
			if (mime.startsWith("text/") || ext in TEXT_EXTENSIONS || mime == "application/json") {
				val stream = resolver.openInputStream(uri)
				val text = stream?.bufferedReader()?.use { it.readText() }?.take(MAX_TEXT_CHARS)
				return@withContext Attachment(
					name = name,
					mimeType = mime,
					sizeBytes = size,
					kind = AttachmentKind.FILE,
					textContent = text,
					localUri = uri.toString(),
				)
			}

			// فایل باینری (PDF و ...)
			val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
				?: return@withContext null
			if (bytes.size > MAX_BINARY_BYTES) {
				return@withContext Attachment(
					name = name,
					mimeType = mime,
					sizeBytes = bytes.size.toLong(),
					kind = AttachmentKind.FILE,
					textContent = "[فایل " + name + " بزرگتر از ۱۲ مگابایت بود و ارسال نشد]",
					localUri = uri.toString(),
				)
			}
			val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
			Attachment(
				name = name,
				mimeType = mime,
				sizeBytes = bytes.size.toLong(),
				kind = AttachmentKind.FILE,
				dataUrl = "data:" + mime + ";base64," + b64,
				localUri = uri.toString(),
			)
		} catch (t: Throwable) {
			null
		}
	}

	private fun readZip(context: Context, uri: Uri): String {
		val builder = StringBuilder()
		try {
			context.contentResolver.openInputStream(uri)?.use { input ->
				ZipInputStream(input).use { zip ->
					builder.append("محتوای فایل ZIP:\n")
					var entry = zip.nextEntry
					var count = 0
					while (entry != null && count < 200) {
						if (!entry.isDirectory) {
							val entryName = entry.name
							val ext = entryName.substringAfterLast('.', "").lowercase()
							builder.append("\n▶ ").append(entryName).append("\n")
							if (ext in TEXT_EXTENSIONS) {
								val bytes = zip.readBytes()
								val text = String(bytes).take(MAX_ZIP_ENTRY_CHARS)
								builder.append("```\n").append(text).append("\n```\n")
							}
							count++
						}
						zip.closeEntry()
						entry = zip.nextEntry
					}
				}
			}
		} catch (t: Throwable) {
			builder.append("[خطا در خواندن فایل zip]")
		}
		return builder.toString().take(MAX_TEXT_CHARS)
	}

	private fun imageToDataUrl(context: Context, uri: Uri): String? {
		val resolver = context.contentResolver
		val bounds = BitmapFactory.Options()
		bounds.inJustDecodeBounds = true
		resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
		var sample = 1
		val w = bounds.outWidth
		val h = bounds.outHeight
		while (w / sample > MAX_IMAGE_DIM || h / sample > MAX_IMAGE_DIM) sample *= 2
		val opts = BitmapFactory.Options()
		opts.inSampleSize = sample
		val bitmap: Bitmap = resolver.openInputStream(uri)?.use {
			BitmapFactory.decodeStream(it, null, opts)
		} ?: return null
		val out = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
		bitmap.recycle()
		val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
		return "data:image/jpeg;base64," + b64
	}

	fun guessMime(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
		"png" -> "image/png"
		"jpg", "jpeg" -> "image/jpeg"
		"webp" -> "image/webp"
		"gif" -> "image/gif"
		"pdf" -> "application/pdf"
		"zip" -> "application/zip"
		"txt", "md" -> "text/plain"
		else -> "application/octet-stream"
	}

	fun humanSize(bytes: Long): String = when {
		bytes <= 0L -> ""
		bytes < 1024L -> bytes.toString() + " B"
		bytes < 1024L * 1024L -> (bytes / 1024L).toString() + " KB"
		else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
	}
}
