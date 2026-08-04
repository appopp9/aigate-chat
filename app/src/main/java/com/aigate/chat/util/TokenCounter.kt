package com.aigate.chat.util

import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.Provider

/**
 * شمارش توکن‌ها (تقریبی) و تخمین هزینه بر اساس قیمت‌های ذخیره‌شده‌ی Provider.
 * تقریب ساده: ۱ توکن ≈ ۴ کاراکتر انگلیسی / ~۱.۲ کاراکتر فارسی.
 */
object TokenCounter {

	private val TOKENIZE_REGEX = Regex("\\w+|[^\\w\\s]|\\s+")

	private fun estimateTokens(s: String): Int {
		if (s.isEmpty()) return 0
		// ترکیب: تعداد کلمات/نشانه‌ها + تخمین برای متن فارسی/غیرفضا
		val tokens = TOKENIZE_REGEX.findAll(s).count()
		val nonLatin = s.count { it.code > 127 } / 2
		return tokens + nonLatin / 2
	}

	fun countText(text: String): Int = estimateTokens(text)

	fun countMessage(message: ChatMessage): Int {
		var total = estimateTokens(message.content)
		for (a in message.attachments) {
			a.textContent?.let { total += estimateTokens(it) }
		}
		return total
	}

	fun countHistory(history: List<ChatMessage>): Int =
		history.sumOf { countMessage(it) }

	fun estimateCost(provider: Provider, promptTokens: Int, completionTokens: Int): Double {
		val inPrice = if (provider.inputPricePerM > 0.0) provider.inputPricePerM else 0.0
		val outPrice = if (provider.outputPricePerM > 0.0) provider.outputPricePerM else 0.0
		return (promptTokens.toDouble() * inPrice + completionTokens.toDouble() * outPrice) / 1_000_000.0
	}

	fun formatTokens(count: Int): String =
		when {
			count >= 1000 -> String.format("%.1fK", count / 1000.0)
			else -> count.toString()
		}

	fun formatCost(cost: Double): String =
		when {
			cost <= 0.0 -> "—"
			cost < 0.01 -> String.format("$%.6f", cost)
			else -> String.format("$%.4f", cost)
		}
}