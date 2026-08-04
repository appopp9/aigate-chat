package com.aigate.chat.net

import com.aigate.chat.model.AppSettings
import com.aigate.chat.model.AttachmentKind
import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.Provider
import com.aigate.chat.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed interface StreamEvent {
	data class Delta(val text: String) : StreamEvent
	data class Usage(val promptTokens: Int, val completionTokens: Int) : StreamEvent
	data class Failure(val message: String) : StreamEvent
	object Done : StreamEvent
}

data class PingResult(val ok: Boolean, val latencyMs: Long, val message: String)

/** client omumi baraye API haye sazegar ba OpenAI va Anthropic */
class AiClient {

	private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

	private val client: OkHttpClient = OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.writeTimeout(180, TimeUnit.SECONDS)
		.readTimeout(0, TimeUnit.SECONDS)
		.build()

	companion object {
		fun normalizeBaseUrl(raw: String): String {
			var b = raw.trim().trimEnd('/')
			if (b.isEmpty()) return b
			if (!b.startsWith("http://") && !b.startsWith("https://")) b = "https://" + b
			return b
		}

		const val ANTHROPIC_VERSION: String = "2023-06-01"
	}

	private fun endpoint(baseUrl: String, path: String): String =
		normalizeBaseUrl(baseUrl) + "/" + path

	private fun applyAuth(builder: Request.Builder, type: ProviderType, authKey: String) {
		if (authKey.isBlank()) return
		if (type == ProviderType.ANTHROPIC) {
			builder.addHeader("x-api-key", authKey)
			builder.addHeader("anthropic-version", ANTHROPIC_VERSION)
		} else {
			builder.addHeader("Authorization", "Bearer " + authKey)
		}
	}

	// ---------------- OpenAI body ----------------

	private fun openAiParts(m: ChatMessage, sendFilesAsBase64: Boolean): List<JsonObject> {
		val parts = ArrayList<JsonObject>()
		if (m.activeText.isNotBlank()) {
			parts.add(buildJsonObject {
				put("type", "text")
				put("text", m.activeText)
			})
		}
		for (a in m.attachments) {
			val dataUrl = a.dataUrl
			val textContent = a.textContent
			if (a.kind == AttachmentKind.IMAGE && dataUrl != null) {
				parts.add(buildJsonObject {
					put("type", "image_url")
					putJsonObject("image_url") { put("url", dataUrl) }
				})
			} else if (textContent != null) {
				parts.add(buildJsonObject {
					put("type", "text")
					put(
						"text",
						"\n\n--- file: " + a.name + " ---\n" + textContent + "\n--- end of file ---"
					)
				})
			} else if (dataUrl != null && sendFilesAsBase64) {
				parts.add(buildJsonObject {
					put("type", "file")
					putJsonObject("file") {
						put("filename", a.name)
						put("file_data", dataUrl)
					}
				})
			} else {
				parts.add(buildJsonObject {
					put("type", "text")
					put("text", "[attachment: " + a.name + " (" + a.mimeType + ")]")
				})
			}
		}
		return parts
	}

	private fun openAiContent(m: ChatMessage, sendFilesAsBase64: Boolean): JsonElement {
		// baraye payam matni sade, content bayad String bashad (sazegari bishtar ba gateway ha)
		if (m.attachments.isEmpty()) return JsonPrimitive(m.activeText)
		val parts = openAiParts(m, sendFilesAsBase64)
		if (parts.isEmpty()) return JsonPrimitive(m.activeText)
		return JsonArray(parts)
	}

	private fun buildOpenAiBody(
		model: String,
		settings: AppSettings,
		systemPrompt: String,
		history: List<ChatMessage>,
		stream: Boolean,
	): String = buildJsonObject {
		put("model", model)
		put("stream", stream)
		put("temperature", settings.temperature)
		put("messages", buildJsonArray {
			if (systemPrompt.isNotBlank()) {
				add(buildJsonObject {
					put("role", "system")
					put("content", systemPrompt)
				})
			}
			for (m in history) {
				if (m.role != "user" && m.role != "assistant") continue
				// payam haye khali ra nafrest (gateway error midahad)
				if (m.activeText.isBlank() && m.attachments.isEmpty()) continue
				add(buildJsonObject {
					put("role", m.role)
					put("content", openAiContent(m, settings.sendFilesAsBase64))
				})
			}
		})
	}.toString()

	// ---------------- Anthropic body ----------------

	private fun anthropicParts(m: ChatMessage): List<JsonObject> {
		val parts = ArrayList<JsonObject>()
		if (m.activeText.isNotBlank()) {
			parts.add(buildJsonObject {
				put("type", "text")
				put("text", m.activeText)
			})
		}
		for (a in m.attachments) {
			val dataUrl = a.dataUrl
			val textContent = a.textContent
			val base64 = dataUrl?.substringAfter("base64,", "").orEmpty()
			if (a.kind == AttachmentKind.IMAGE && base64.isNotEmpty()) {
				parts.add(buildJsonObject {
					put("type", "image")
					putJsonObject("source") {
						put("type", "base64")
						put("media_type", a.mimeType.ifBlank { "image/jpeg" })
						put("data", base64)
					}
				})
			} else if (textContent != null) {
				parts.add(buildJsonObject {
					put("type", "text")
					put(
						"text",
						"\n\n--- file: " + a.name + " ---\n" + textContent + "\n--- end of file ---"
					)
				})
			} else if (a.mimeType == "application/pdf" && base64.isNotEmpty()) {
				parts.add(buildJsonObject {
					put("type", "document")
					putJsonObject("source") {
						put("type", "base64")
						put("media_type", "application/pdf")
						put("data", base64)
					}
				})
			} else {
				parts.add(buildJsonObject {
					put("type", "text")
					put("text", "[attachment: " + a.name + " (" + a.mimeType + ")]")
				})
			}
		}
		return parts
	}

	private fun buildAnthropicBody(
		model: String,
		settings: AppSettings,
		systemPrompt: String,
		history: List<ChatMessage>,
		stream: Boolean,
	): String = buildJsonObject {
		put("model", model)
		put("stream", stream)
		put("temperature", settings.temperature)
		put("max_tokens", settings.maxTokens)
		if (systemPrompt.isNotBlank()) put("system", systemPrompt)
		put("messages", buildJsonArray {
			for (m in history) {
				if (m.role != "user" && m.role != "assistant") continue
				val parts = anthropicParts(m)
				if (parts.isEmpty()) continue
				add(buildJsonObject {
					put("role", m.role)
					put("content", JsonArray(parts))
				})
			}
		})
	}.toString()

	// ---------------- request ----------------

	private fun errorText(code: Int, body: String): String {
		val base = "HTTP " + code + " - " + body.take(400)
		val low = body.lowercase()
		val hint = when {
			low.contains("do_request_failed") || low.contains("upstream error") ->
				"\n\nراهنما: این خطا از خود سرویس API است و یعنی این مدل روی سرور در دسترس نیست. یک مدل دیگر انتخاب کن."
			code == 401 || code == 403 -> "\n\nراهنما: کلید API معتبر نیست."
			code == 404 -> "\n\nراهنما: آدرس Base URL یا نام مدل اشتباه است."
			code == 429 -> "\n\nراهنما: محدودیت نرخ یا اتمام اعتبار."
			else -> ""
		}
		return base + hint
	}

	private fun chatPath(type: ProviderType): String =
		if (type == ProviderType.ANTHROPIC) "messages" else "chat/completions"

	private fun buildBody(
		provider: Provider,
		model: String,
		settings: AppSettings,
		systemPrompt: String,
		history: List<ChatMessage>,
		stream: Boolean,
	): String = if (provider.type == ProviderType.ANTHROPIC) {
		buildAnthropicBody(model, settings, systemPrompt, history, stream)
	} else {
		buildOpenAiBody(model, settings, systemPrompt, history, stream)
	}

	private fun postRequest(provider: Provider, path: String, body: String, stream: Boolean): Request {
		val builder = Request.Builder()
			.url(endpoint(provider.baseUrl, path))
			.post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
		applyAuth(builder, provider.type, provider.authKey)
		if (stream) builder.addHeader("Accept", "text/event-stream")
		return builder.build()
	}

	// ---------------- parse ----------------

	private fun parseOpenAiDelta(payload: String): String? = try {
		val choice = json.parseToJsonElement(payload).jsonObject["choices"]
			?.jsonArray?.firstOrNull()?.jsonObject
		val delta = choice?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
		delta ?: choice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
	} catch (t: Throwable) {
		null
	}

	private fun parseAnthropicDelta(payload: String): String? = try {
		val root = json.parseToJsonElement(payload).jsonObject
		val type = root["type"]?.jsonPrimitive?.contentOrNull
		if (type == "content_block_delta") {
			root["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
		} else {
			null
		}
	} catch (t: Throwable) {
		null
	}

	private fun parseUsage(payload: String, type: ProviderType): StreamEvent.Usage? = try {
		val root = json.parseToJsonElement(payload).jsonObject
		if (type == ProviderType.ANTHROPIC) {
			val usage = root["message"]?.jsonObject?.get("usage")?.jsonObject
				?: root["usage"]?.jsonObject
			val input = usage?.get("input_tokens")?.jsonPrimitive?.intOrNull
			val output = usage?.get("output_tokens")?.jsonPrimitive?.intOrNull
			if (input != null || output != null) StreamEvent.Usage(input ?: 0, output ?: 0) else null
		} else {
			val usage = root["usage"]?.jsonObject
			val input = usage?.get("prompt_tokens")?.jsonPrimitive?.intOrNull
			val output = usage?.get("completion_tokens")?.jsonPrimitive?.intOrNull
			if (input != null || output != null) StreamEvent.Usage(input ?: 0, output ?: 0) else null
		}
	} catch (t: Throwable) {
		null
	}

	// ---------------- streaming chat ----------------

	fun streamChat(
		provider: Provider,
		model: String,
		settings: AppSettings,
		systemPrompt: String,
		history: List<ChatMessage>,
	): Flow<StreamEvent> = flow {
		if (model.isBlank()) {
			emit(StreamEvent.Failure("model entekhab nashode — az menu-ye bala yek model entekhab kon"))
			return@flow
		}
		val body = buildBody(provider, model, settings, systemPrompt, history, true)
		val request = postRequest(provider, chatPath(provider.type), body, true)
		try {
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) {
					val errorBody = response.body?.string().orEmpty()
					emit(StreamEvent.Failure(errorText(response.code, errorBody)))
					return@flow
				}
				val source = response.body?.source()
				if (source == null) {
					emit(StreamEvent.Failure("pasokhe khali az server"))
					return@flow
				}
				var sawData = false
				val raw = StringBuilder()
				while (true) {
					val line = source.readUtf8Line() ?: break
					if (line.isBlank()) continue
					if (!line.startsWith("data:")) {
						if (raw.length < 4000) raw.append(line).append("\n")
						continue
					}
					sawData = true
					val payload = line.removePrefix("data:").trim()
					if (payload == "[DONE]") break
					val delta = if (provider.type == ProviderType.ANTHROPIC) {
						parseAnthropicDelta(payload)
					} else {
						parseOpenAiDelta(payload)
					}
					if (delta != null && delta.isNotEmpty()) emit(StreamEvent.Delta(delta))
					val usage = parseUsage(payload, provider.type)
					if (usage != null) emit(usage)
				}
				if (!sawData) {
					// server SSE nadad — shayad javab yekja bud
					val whole = raw.toString().trim()
					val text = if (whole.isNotEmpty()) parseWholeBody(whole, provider.type) else null
					if (text != null && text.isNotEmpty()) {
						emit(StreamEvent.Delta(text))
					} else {
						emit(StreamEvent.Failure("stream pasokhi nadasht"))
					}
				}
			}
			emit(StreamEvent.Done)
		} catch (t: Throwable) {
			emit(StreamEvent.Failure(t.message ?: "khataye shabake"))
		}
	}.flowOn(Dispatchers.IO)

	private fun parseWholeBody(text: String, type: ProviderType): String? = try {
		val root = json.parseToJsonElement(text).jsonObject
		if (type == ProviderType.ANTHROPIC) {
			val blocks = root["content"]?.jsonArray
			val sb = StringBuilder()
			if (blocks != null) {
				for (block in blocks) {
					val piece = block.jsonObject["text"]?.jsonPrimitive?.contentOrNull
					if (piece != null) sb.append(piece)
				}
			}
			sb.toString()
		} else {
			root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")
				?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
		}
	} catch (t: Throwable) {
		null
	}

	// ---------------- non-streaming chat ----------------

	suspend fun complete(
		provider: Provider,
		model: String,
		settings: AppSettings,
		systemPrompt: String,
		history: List<ChatMessage>,
	): Result<String> = withContext(Dispatchers.IO) {
		if (model.isBlank()) {
			return@withContext Result.failure<String>(
				RuntimeException("model entekhab nashode — az menu-ye bala yek model entekhab kon")
			)
		}
		try {
			val body = buildBody(provider, model, settings, systemPrompt, history, false)
			val request = postRequest(provider, chatPath(provider.type), body, false)
			client.newCall(request).execute().use { response ->
				val text = response.body?.string().orEmpty()
				if (!response.isSuccessful) {
					return@withContext Result.failure<String>(
						RuntimeException(errorText(response.code, text))
					)
				}
				val content = parseWholeBody(text, provider.type).orEmpty()
				if (content.isEmpty()) {
					Result.failure<String>(RuntimeException("pasokhe khali: " + text.take(300)))
				} else {
					Result.success(content)
				}
			}
		} catch (t: Throwable) {
			Result.failure<String>(t)
		}
	}

	// ---------------- models ----------------

	suspend fun listModels(
		baseUrl: String,
		authKey: String,
		type: ProviderType,
	): Result<List<String>> = withContext(Dispatchers.IO) {
		try {
			val builder = Request.Builder().url(endpoint(baseUrl, "models")).get()
			applyAuth(builder, type, authKey)
			client.newCall(builder.build()).execute().use { response ->
				val text = response.body?.string().orEmpty()
				if (!response.isSuccessful) {
					return@withContext Result.failure<List<String>>(
						RuntimeException("HTTP " + response.code + " - " + text.take(300))
					)
				}
				val root = json.parseToJsonElement(text).jsonObject
				val array = root["data"]?.jsonArray ?: root["models"]?.jsonArray
				val ids = ArrayList<String>()
				if (array != null) {
					for (item in array) {
						val obj = item.jsonObject
						val id = obj["id"]?.jsonPrimitive?.contentOrNull
							?: obj["name"]?.jsonPrimitive?.contentOrNull
						if (id != null) ids.add(id)
					}
				}
				Result.success(ids.distinct())
			}
		} catch (t: Throwable) {
			Result.failure<List<String>>(t)
		}
	}

	// ---------------- ping ----------------

	suspend fun ping(baseUrl: String, authKey: String, type: ProviderType): PingResult =
		withContext(Dispatchers.IO) {
			val started = System.currentTimeMillis()
			try {
				val builder = Request.Builder().url(endpoint(baseUrl, "models")).get()
				applyAuth(builder, type, authKey)
				client.newCall(builder.build()).execute().use { response ->
					val elapsed = System.currentTimeMillis() - started
					val bodyText = response.body?.string().orEmpty()
					if (response.isSuccessful) {
						PingResult(true, elapsed, "ettesal salem")
					} else {
						PingResult(false, elapsed, "HTTP " + response.code + " - " + bodyText.take(160))
					}
				}
			} catch (t: Throwable) {
				PingResult(false, System.currentTimeMillis() - started, t.message ?: "khataye shabake")
			}
		}

	// ---------------- image ----------------

	suspend fun generateImage(
		provider: Provider,
		model: String,
		prompt: String,
	): Result<String> = withContext(Dispatchers.IO) {
		try {
			val body = buildJsonObject {
				put("model", model)
				put("prompt", prompt)
				put("n", 1)
				put("size", "1024x1024")
			}.toString()
			val request = postRequest(provider, "images/generations", body, false)
			client.newCall(request).execute().use { response ->
				val text = response.body?.string().orEmpty()
				if (!response.isSuccessful) {
					return@withContext Result.failure<String>(
						RuntimeException("HTTP " + response.code + " - " + text.take(400))
					)
				}
				val first = json.parseToJsonElement(text).jsonObject["data"]
					?.jsonArray?.firstOrNull()?.jsonObject
				val url = first?.get("url")?.jsonPrimitive?.contentOrNull
				val b64 = first?.get("b64_json")?.jsonPrimitive?.contentOrNull
				when {
					url != null -> Result.success(url)
					b64 != null -> Result.success("data:image/png;base64," + b64)
					else -> Result.failure<String>(RuntimeException("khoruji-e tasvir peyda nashod"))
				}
			}
		} catch (t: Throwable) {
			Result.failure<String>(t)
		}
	}
}
