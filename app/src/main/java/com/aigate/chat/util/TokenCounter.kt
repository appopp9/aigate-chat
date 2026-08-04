package com.aigate.chat.util

import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.Provider
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * shomaresh taghribi token va takhmine hazine.
 * mohasebe kamelan mahalli ast va hich darkhasti be server nemizanad.
 */
object TokenCounter {

	/** horoofe latin taghriban 4 character be ezaye har token, farsi taghriban 2 */
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
			total += if (textContent != null) countText(textContent) else 765
		}
		return total
	}

	fun countHistory(history: List<ChatMessage>): Int {
		var total = 0
		for (m in history) total += countMessage(m)
		return total
	}

	/** hazine bar hasbe dollar ba gheymate ekhtesasi har model */
	fun estimateCost(
		provider: Provider?,
		model: String,
		promptTokens: Int,
		completionTokens: Int,
	): Double {
		if (provider == null) return 0.0
		val pricing = provider.pricingFor(model)
		val input = pricing.inputPricePerM * (promptTokens / 1_000_000.0)
		val output = pricing.outputPricePerM * (completionTokens / 1_000_000.0)
		return input + output
	}

	/** nosakheye ghadimi: gheymate pishfarze provider */
	fun estimateCost(provider: Provider?, promptTokens: Int, completionTokens: Int): Double =
		estimateCost(provider, provider?.defaultModel.orEmpty(), promptTokens, completionTokens)

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
