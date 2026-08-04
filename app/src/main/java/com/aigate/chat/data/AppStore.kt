package com.aigate.chat.data

import android.content.Context
import com.aigate.chat.model.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** ذخیره‌سازی کل وضعیت اپ (API ها، گفتگوها، تنظیمات) در یک فایل JSON */
class AppStore(context: Context) {

	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
		prettyPrint = false
	}

	private val prettyJson = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
		prettyPrint = true
	}

	private val file = File(context.filesDir, "aigate_state.json")

	suspend fun load(): AppState = withContext(Dispatchers.IO) {
		try {
			if (!file.exists()) return@withContext AppState()
			json.decodeFromString(AppState.serializer(), file.readText())
		} catch (t: Throwable) {
			AppState()
		}
	}

	suspend fun save(state: AppState) {
		withContext(Dispatchers.IO) {
			try {
				val text: String = json.encodeToString(AppState.serializer(), state)
				file.writeText(text)
			} catch (t: Throwable) {
				// ذخیره‌سازی ناموفق نباید باعث کرش شود
			}
		}
	}

	/** خروجی پشتیبان به صورت متن JSON */
	fun exportJson(state: AppState): String = prettyJson.encodeToString(AppState.serializer(), state)

	/** بازیابی از متن JSON */
	fun importJson(text: String): Result<AppState> = try {
		Result.success(json.decodeFromString(AppState.serializer(), text))
	} catch (t: Throwable) {
		Result.failure<AppState>(t)
	}
}
