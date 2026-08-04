package com.aigate.chat.util

import com.aigate.chat.model.Conversation
import com.aigate.chat.model.Provider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** خروجی گرفتن از گفتگو */
object Exporter {

	private fun stamp(time: Long): String =
		SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(time))

	fun safeName(title: String): String {
		val cleaned = title.replace(Regex("[^\\p{L}\\p{N}\\-_ ]"), "").trim().replace(' ', '_')
		return if (cleaned.isEmpty()) "conversation" else cleaned.take(40)
	}

	fun toMarkdown(conversation: Conversation, provider: Provider?): String {
		val sb = StringBuilder()
		sb.append("# ").append(conversation.title).append("\n\n")
		sb.append("- API: ").append(provider?.name ?: "-").append("\n")
		sb.append("- Model: ").append(conversation.model.ifBlank { "-" }).append("\n")
		sb.append("- Date: ").append(stamp(conversation.updatedAt)).append("\n")
		sb.append("- Tokens: ").append(conversation.totalTokens)
			.append(" | Cost: ").append(TokenCounter.formatCost(conversation.totalCost)).append("\n\n")
		sb.append("---\n\n")
		for (m in conversation.messages) {
			val who = when (m.role) {
				"user" -> "👤 User"
				"assistant" -> "🤖 Assistant"
				else -> m.role
			}
			sb.append("## ").append(who).append("  \n")
			sb.append("*").append(stamp(m.createdAt)).append("*\n\n")
			for (a in m.attachments) {
				sb.append("> 📎 ").append(a.name).append("\n\n")
			}
			sb.append(m.activeText).append("\n\n")
		}
		return sb.toString()
	}

	fun toPlainText(conversation: Conversation): String {
		val sb = StringBuilder()
		sb.append(conversation.title).append("\n").append("=".repeat(30)).append("\n\n")
		for (m in conversation.messages) {
			val who = if (m.role == "user") "کاربر" else "دستیار"
			sb.append("[").append(who).append("] ").append(stamp(m.createdAt)).append("\n")
			sb.append(m.activeText).append("\n\n")
		}
		return sb.toString()
	}

	/** HTML ساده — قابل باز کردن در مرورگر و چاپ/ذخیره به PDF */
	fun toHtml(conversation: Conversation, provider: Provider?): String {
		val sb = StringBuilder()
		sb.append("<!DOCTYPE html><html dir=\"rtl\" lang=\"fa\"><head><meta charset=\"utf-8\">")
		sb.append("<title>").append(escape(conversation.title)).append("</title><style>")
		sb.append("body{font-family:Vazirmatn,Tahoma,sans-serif;background:#FFF6E9;color:#101010;padding:24px;line-height:1.9}")
		sb.append(".msg{border:3px solid #101010;border-radius:10px;padding:14px 18px;margin:18px 0;box-shadow:-6px 6px 0 #101010}")
		sb.append(".user{background:#FFD93D}.assistant{background:#fff}")
		sb.append(".who{font-weight:bold;margin-bottom:8px}")
		sb.append("pre{background:#101010;color:#B5F44A;padding:12px;border-radius:8px;overflow:auto;direction:ltr;text-align:left}")
		sb.append("h1{border-bottom:5px solid #101010;padding-bottom:8px}")
		sb.append("</style></head><body>")
		sb.append("<h1>").append(escape(conversation.title)).append("</h1>")
		sb.append("<p>API: ").append(escape(provider?.name ?: "-"))
			.append(" | مدل: ").append(escape(conversation.model))
			.append(" | توکن: ").append(conversation.totalTokens)
			.append(" | هزینه: ").append(TokenCounter.formatCost(conversation.totalCost)).append("</p>")
		for (m in conversation.messages) {
			val cls = if (m.role == "user") "user" else "assistant"
			val who = if (m.role == "user") "کاربر" else "دستیار"
			sb.append("<div class=\"msg ").append(cls).append("\">")
			sb.append("<div class=\"who\">").append(who).append(" — ").append(stamp(m.createdAt)).append("</div>")
			sb.append(renderBody(m.activeText))
			sb.append("</div>")
		}
		sb.append("</body></html>")
		return sb.toString()
	}

	private fun renderBody(text: String): String {
		val parts = text.split("```")
		val sb = StringBuilder()
		for (index in parts.indices) {
			val part = parts[index]
			if (index % 2 == 1) {
				val body = part.substringAfter('\n', part)
				sb.append("<pre>").append(escape(body)).append("</pre>")
			} else {
				sb.append("<p>").append(escape(part).replace("\n", "<br>")).append("</p>")
			}
		}
		return sb.toString()
	}

	private fun escape(text: String): String = text
		.replace("&", "&amp;")
		.replace("<", "&lt;")
		.replace(">", "&gt;")
}
