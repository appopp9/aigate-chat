package com.aigate.chat.util

/** بلوک‌های محتوایی که از پاسخ مدل استخراج می‌شوند */
sealed interface ContentBlock {
	data class Text(val text: String) : ContentBlock
	data class Code(val lang: String, val code: String) : ContentBlock
	data class Image(val url: String, val alt: String) : ContentBlock
	data class FileLink(val url: String, val label: String) : ContentBlock
}

object ContentParser {

	private val imageRegex = Regex("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)")
	private val linkRegex = Regex("(?<!!)\\[([^\\]]*)\\]\\((https?://[^)\\s]+)\\)")
	private val bareImageRegex = Regex("(?<![(\\w])(https?://\\S+\\.(?:png|jpe?g|webp|gif))", RegexOption.IGNORE_CASE)
	private val bareFileRegex =
		Regex("(?<![(\\w])(https?://\\S+\\.(?:zip|pdf|rar|7z|tar|gz|csv|xlsx|docx|pptx|apk|txt|json))", RegexOption.IGNORE_CASE)

	fun parse(raw: String): List<ContentBlock> {
		val blocks = ArrayList<ContentBlock>()
		val segments = raw.split("```")
		for (index in segments.indices) {
			val segment = segments[index]
			if (segment.isEmpty()) continue
			if (index % 2 == 1) {
				val firstLine = segment.substringBefore('\n', "").trim()
				val isLang = firstLine.isNotEmpty() && firstLine.length < 20 && !firstLine.contains(' ')
				val lang = if (isLang) firstLine else ""
				val code = if (isLang) segment.substringAfter('\n', "") else segment
				blocks.add(ContentBlock.Code(lang, code.trimEnd()))
			} else {
				blocks.addAll(parseInline(segment))
			}
		}
		if (blocks.isEmpty()) blocks.add(ContentBlock.Text(raw))
		return blocks
	}

	private fun parseInline(text: String): List<ContentBlock> {
		val result = ArrayList<ContentBlock>()
		var cursor = 0
		val matches = ArrayList<Triple<IntRange, String, String>>() // range, kind, payload

		for (m in imageRegex.findAll(text)) {
			matches.add(Triple(m.range, "image:" + (m.groupValues[1]), m.groupValues[2]))
		}
		for (m in bareImageRegex.findAll(text)) {
			if (matches.none { it.first.contains(m.range.first) }) {
				matches.add(Triple(m.range, "image:", m.groupValues[1]))
			}
		}
		for (m in linkRegex.findAll(text)) {
			val url = m.groupValues[2]
			if (isFileUrl(url) && matches.none { it.first.contains(m.range.first) }) {
				matches.add(Triple(m.range, "file:" + m.groupValues[1], url))
			}
		}
		for (m in bareFileRegex.findAll(text)) {
			if (matches.none { it.first.contains(m.range.first) }) {
				matches.add(Triple(m.range, "file:", m.groupValues[1]))
			}
		}
		// تصاویر base64 مستقیم در متن
		for (m in Regex("data:image/[a-zA-Z]+;base64,[A-Za-z0-9+/=]+").findAll(text)) {
			if (matches.none { it.first.contains(m.range.first) }) {
				matches.add(Triple(m.range, "image:", m.value))
			}
		}

		matches.sortBy { it.first.first }

		for (match in matches) {
			val range = match.first
			if (range.first > cursor) {
				val chunk = text.substring(cursor, range.first)
				if (chunk.isNotBlank()) result.add(ContentBlock.Text(chunk.trim()))
			}
			val kind = match.second
			val payload = match.third
			if (kind.startsWith("image:")) {
				result.add(ContentBlock.Image(payload, kind.removePrefix("image:")))
			} else {
				val label = kind.removePrefix("file:").ifBlank { payload.substringAfterLast('/').substringBefore('?') }
				result.add(ContentBlock.FileLink(payload, label))
			}
			cursor = range.last + 1
		}
		if (cursor < text.length) {
			val chunk = text.substring(cursor)
			if (chunk.isNotBlank()) result.add(ContentBlock.Text(chunk.trim()))
		}
		return result
	}

	private fun isFileUrl(url: String): Boolean {
		val clean = url.substringBefore('?').lowercase()
		return listOf(".zip", ".pdf", ".rar", ".7z", ".tar", ".gz", ".csv", ".xlsx", ".docx", ".pptx", ".apk", ".txt", ".json")
			.any { clean.endsWith(it) }
	}

	fun codeBlocks(raw: String): List<Pair<String, String>> =
		parse(raw).filterIsInstance<ContentBlock.Code>().map { Pair(it.lang, it.code) }
}
