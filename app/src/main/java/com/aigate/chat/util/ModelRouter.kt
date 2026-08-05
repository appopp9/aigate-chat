package com.aigate.chat.util

import com.aigate.chat.model.Provider

/** masiryab e hooshmand e model. */
object ModelRouter {

	private val heavyWords = listOf(
		"کد", "باگ", "خطا", "دیباگ", "برنامه", "الگوریتم", "ریاضی", "اثبات",
		"تحلیل", "معما", "بهینه", "معماری", "رفاکتور", "استدلال",
		"code", "bug", "debug", "algorithm", "proof", "math", "refactor",
		"sql", "kotlin", "python", "regex", "analyze"
	)

	private val strongMarkers = listOf(
		"opus", "gpt-5", "gpt-4.1", "gpt-4o", "gpt-4", "o3", "o1", "sonnet", "pro",
		"r1", "reason", "think", "large", "70b", "235b", "ultra"
	)

	private val lightMarkers = listOf(
		"mini", "nano", "flash", "haiku", "lite", "small", "turbo", "8b", "7b", "3b"
	)

	fun needsStrong(prompt: String, hasAttachments: Boolean): Boolean {
		if (hasAttachments) return true
		if (prompt.length > 600) return true
		if (prompt.contains("```")) return true
		val lower = prompt.lowercase()
		return heavyWords.any { lower.contains(it) }
	}

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
		val sorted = pool.sortedWith(
			compareBy<String>({ tierOf(it) }, { provider.pricingFor(it).outputPricePerM })
		)
		val chosen = if (strong) sorted.lastOrNull() else sorted.firstOrNull()
		return chosen ?: requested
	}

	private fun tierOf(model: String): Int {
		val lower = model.lowercase()
		if (lightMarkers.any { lower.contains(it) }) return 0
		if (strongMarkers.any { lower.contains(it) }) return 2
		return 1
	}
}
