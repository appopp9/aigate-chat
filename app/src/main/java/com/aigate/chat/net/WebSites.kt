package com.aigate.chat.net

/**
 * profile e site haye halate neshaste web.
 *
 * har site: adres, marker e adrese goftogoo (baraye jodaa kardane chat ha),
 * modelhaye ghabele entekhab va hint haye adrese darkhast e chat (baraye shenoode shabake).
 */
data class WebSite(
	val id: String,
	val label: String,
	val url: String,
	val host: String,
	val sessionMarker: String,
	val models: List<String>,
	val hints: List<String>,
)

object WebSites {

	val DEEPSEEK = WebSite(
		id = "deepseek",
		label = "DeepSeek (وب)",
		url = "https://chat.deepseek.com/",
		host = "chat.deepseek.com",
		sessionMarker = "/a/chat/s/",
		models = listOf("DeepSeek (وب)", "DeepSeek R1 (استدلال)"),
		hints = listOf("completion", "chat/comple"),
	)

	val CHATGPT = WebSite(
		id = "chatgpt",
		label = "ChatGPT (وب)",
		url = "https://chatgpt.com/",
		host = "chatgpt.com",
		sessionMarker = "/c/",
		models = listOf("ChatGPT (وب)", "ChatGPT Thinking (استدلال)"),
		hints = listOf("/backend-api/conversation", "/backend-alt/conversation", "/conversation", "completion"),
	)

	val CLAUDE = WebSite(
		id = "claude",
		label = "Claude (وب)",
		url = "https://claude.ai/new",
		host = "claude.ai",
		sessionMarker = "/chat/",
		models = listOf("Claude (وب)", "Claude Extended Thinking (استدلال)"),
		hints = listOf("/completion", "retry_completion", "/messages", "completion"),
	)

	val QWEN = WebSite(
		id = "qwen",
		label = "Qwen (وب)",
		url = "https://chat.qwen.ai/",
		host = "qwen.ai",
		sessionMarker = "/c/",
		models = listOf("Qwen (وب)", "Qwen Thinking (استدلال)"),
		hints = listOf("chat/completions", "/api/v2/chat", "/api/chat", "completion"),
	)

	val all: List<WebSite> = listOf(DEEPSEEK, CHATGPT, CLAUDE, QWEN)

	/** site e motenaseb ba adrese dade shode; pishfarz DeepSeek. */
	fun forUrl(url: String): WebSite {
		val host = url.substringAfter("//", url).substringBefore("/").lowercase()
		if (host.isBlank()) return DEEPSEEK
		return all.firstOrNull { host.contains(it.host) } ?: DEEPSEEK
	}

	fun byId(id: String): WebSite = all.firstOrNull { it.id == id } ?: DEEPSEEK
}
