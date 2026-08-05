package com.aigate.chat.util

import com.aigate.chat.model.Provider

/**
 * masiryab e hooshmand e model: baraye payam haye sade model e arzoontar
 * va baraye kar haye sangin model e ghavitar entekhab mishavad.
 */
object ModelRouter {

	private val heavyWords = listOf(
		"کد", "باگ", "خطا", "دیباگ", "برنامه", "الگوریتم", "ریاضی", "اثبات",
		"تحلیل", "معما", "بهینه", "معماری", "رفاکتور", "استدلال",
		"code", "bug", "debug", "algorithm", "proof", "math", "refactor", "architecture",
		"sql", "kotlin", "python", "regex", "benchmark", "analyze",
	)

	private val strongMarkers = listOf(
		"opus", "gpt-5", "gpt-4.1", "gpt-4o", "gpt-4", "o3", "o1", "sonnet", "pro",
		"r1", "reason", "think", "large", "70b", "235b", "deepseek-chat", "ultra",
	)

	private val lightMarkers = listOf(
		"mini", "nano", "flash", "haiku", "lite", "small", "turbo", "8b", "7b", "3b",
	)

	/** aya in payam be model e ghavi niaz darad? */
	fun needsStrong(prompt: String, hasAttachments: Boolean): Boolean {
		if (hasAttachments) return true
		if (prompt.length > 600) return true
		if (prompt.contains("```")) return true
		val lower = prompt.lowercase()
		return heavyWords.any { lower.contains(it) }
	}

	/**
	 * model e monaseb ra entekhab mikonad. agar natavanad tasmim begirad,
	 * hamin model e darkhast shode barmigardad.
	 */
	fun pick(
		provider: Provider,
		requested: String,
		prompt: String,
		hasAttachments: Boolean,
	): String {
		val pool = if (provider.favoriteModels.isNotEmpty()) {
			provider.favoriteModels
		} else {
			provider.models
		}
		if (pool.size < 2) return requested
		val strong = needsStrong(prompt, hasAttachments)

		fun outPrice(model: String): Double = provider.pricingFor(model).outputPricePerM
		fun tier(model: String): Int {
			val lower = model.lowercase()
			if (lightMarkers.any { lower.contains(it) }) return 0
			if (strongMarkers.any { lower.contains(it) }) return 2
			return 1
		}

		val sorted = pool.sortedWith(
			compareBy<String> { tier(it) }.thenBy { outPrice(it) }
		)
		val chosen = if (strong) sorted.lastOrNull() else sorted.firstOrNull()
		return chosen ?: requested
	}

	fun reason(model: String, strong: Boolean): String =
		if (strong) "پیام سنگین بود ← " + model else "پیام ساده بود ← " + model
}
