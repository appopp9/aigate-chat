package com.aigate.chat.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** ذخیرهی خروجی‌های مدل (کد، عکس، فایل، zip) در پوشهی Download */
object FileSaver {

	private val http: OkHttpClient = OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(120, TimeUnit.SECONDS)
		.build()

	suspend fun saveText(context: Context, fileName: String, text: String, mime: String = "text/plain"): String? =
		saveBytes(context, fileName, mime, text.toByteArray())

	suspend fun saveBytes(context: Context, fileName: String, mime: String, bytes: ByteArray): String? =
		withContext(Dispatchers.IO) {
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					val values = ContentValues()
					values.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
					values.put(MediaStore.Downloads.MIME_TYPE, mime)
					values.put(MediaStore.Downloads.IS_PENDING, 1)
					val resolver = context.contentResolver
					val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
						?: return@withContext null
					resolver.openOutputStream(uri)?.use { it.write(bytes) }
					val done = ContentValues()
					done.put(MediaStore.Downloads.IS_PENDING, 0)
					resolver.update(uri, done, null, null)
					"Download/" + fileName
				} else {
					val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					if (!dir.exists()) dir.mkdirs()
					val outFile = File(dir, fileName)
					FileOutputStream(outFile).use { it.write(bytes) }
					outFile.absolutePath
				}
			} catch (t: Throwable) {
				null
			}
		}

	/** ذخیرهی منبع تصویر/فایل که از API اومده: هم data URL هم لینک http */
	suspend fun saveFromUrl(context: Context, url: String, suggestedName: String? = null): String? =
		withContext(Dispatchers.IO) {
			try {
				if (url.startsWith("data:")) {
					val mime = url.substringAfter("data:").substringBefore(";").ifBlank { "application/octet-stream" }
					val b64 = url.substringAfter("base64,", "")
					if (b64.isEmpty()) return@withContext null
					val bytes = Base64.decode(b64, Base64.DEFAULT)
					val name = suggestedName ?: ("aigate_" + System.currentTimeMillis() + "." + extForMime(mime))
					return@withContext saveBytes(context, name, mime, bytes)
				}
				val request = Request.Builder().url(url).get().build()
				http.newCall(request).execute().use { response ->
					if (!response.isSuccessful) return@withContext null
					val bytes = response.body?.bytes() ?: return@withContext null
					val mime = response.header("Content-Type") ?: AttachmentUtils.guessMime(url)
					val fallback = url.substringAfterLast('/').substringBefore('?').ifBlank {
						"aigate_" + System.currentTimeMillis() + "." + extForMime(mime)
					}
					saveBytes(context, suggestedName ?: fallback, mime.substringBefore(';'), bytes)
				}
			} catch (t: Throwable) {
				null
			}
		}

	/** همهی بلوک‌های کد یک پیام را داخل یک فایل zip ذخیره می‌کند */
	suspend fun saveCodeBlocksAsZip(
		context: Context,
		blocks: List<Pair<String, String>>,
	): String? = withContext(Dispatchers.IO) {
		if (blocks.isEmpty()) return@withContext null
		try {
			val buffer = java.io.ByteArrayOutputStream()
			ZipOutputStream(buffer).use { zip ->
				blocks.forEachIndexed { index, pair ->
					val lang = pair.first
					val code = pair.second
					val entryName = "snippet_" + (index + 1) + "." + extForLang(lang)
					zip.putNextEntry(ZipEntry(entryName))
					zip.write(code.toByteArray())
					zip.closeEntry()
				}
			}
			val name = "aigate_code_" + System.currentTimeMillis() + ".zip"
			saveBytes(context, name, "application/zip", buffer.toByteArray())
		} catch (t: Throwable) {
			null
		}
	}

	fun extForLang(lang: String): String = when (lang.lowercase().trim()) {
		"kotlin", "kt" -> "kt"
		"java" -> "java"
		"python", "py" -> "py"
		"javascript", "js" -> "js"
		"typescript", "ts" -> "ts"
		"tsx" -> "tsx"
		"jsx" -> "jsx"
		"html" -> "html"
		"css" -> "css"
		"json" -> "json"
		"xml" -> "xml"
		"yaml", "yml" -> "yml"
		"bash", "sh", "shell" -> "sh"
		"sql" -> "sql"
		"go" -> "go"
		"rust", "rs" -> "rs"
		"c" -> "c"
		"cpp", "c++" -> "cpp"
		"csharp", "cs" -> "cs"
		"php" -> "php"
		"swift" -> "swift"
		"dart" -> "dart"
		"ruby", "rb" -> "rb"
		else -> "txt"
	}

	private fun extForMime(mime: String): String = when {
		mime.contains("png") -> "png"
		mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
		mime.contains("webp") -> "webp"
		mime.contains("gif") -> "gif"
		mime.contains("pdf") -> "pdf"
		mime.contains("zip") -> "zip"
		mime.contains("json") -> "json"
		mime.contains("text") -> "txt"
		else -> "bin"
	}
}
