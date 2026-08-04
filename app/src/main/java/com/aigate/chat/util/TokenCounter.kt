package com.aigate.chat.util

import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.Provider
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * شمارش تقریبی توکن و تخمین هزینه.
 * محاسبه کاملاً محلی است و هیچ درخواستی به سرور نمی‌زند.
 */
object TokenCounter {

	/** حروف لاتین تقریباً ۴ کاراکتر به ازای هر توکن، فارسی تقریباً ۲ کاراکتر */
	fun countText(text: String): Int {
		if (text.isEmpty()) return 0
		var ascii = 0
		var wide = 0
		for (ch in text) {
			if (ch.code < 128) ascii++ else wide++
		}
		val estimate = (ascii / 4.0) + (wide / 2.0)
		return max(1, estimate.roundToInt())
	}

	fun countMessage(message: ChatMessage): Int {
		var total = countText(message.activeText) + 4
		for (a in message.attachments) {
			val textContent = a.textContent
			total += if (textContent != null) {
				countText(textContent)
			} else {
				// تخمین تقریبی برای تصویر/فایل باینری
				765
			}
		}
		return total
	}

	fun countHistory(history: List<ChatMessage>): Int {
		var total = 0
		for (m in history) total += countMessage(m)
		return total
	}

	/** هزینه برحسب دلار بر اساس قیمت هر یک میلیون توکن */
	fun estimateCost(provider: Provider?, promptTokens: Int, completionTokens: Int): Double {
		if (provider == null) return 0.0
		val input = provider.inputPricePerM * (promptTokens / 1_000_000.0)
		val output = provider.outputPricePerM * (completionTokens / 1_000_000.0)
		return input + output
	}

	fun formatTokens(count: Int): String = when {
		count >= 1_000_000 -> ((count / 100_000).toDouble() / 10.0).toString() + "M"
		count >= 1_000 -> ((count / 100).toDouble() / 10.0).toString() + "K"
		else -> count.toString()
	}

	fun formatCost(cost: Double): String {
		if (cost <= 0.0) return "$0"
		return when {
			cost < 0.01 -> "$" + ((cost * 1_000_000).roundToInt() / 1_000_000.0).toString()
			cost < 1.0 -> "$" + ((cost * 10_000).roundToInt() / 10_000.0).toString()
			else -> "$" + ((cost * 100).roundToInt() / 100.0).toString()
		}
	}
}
