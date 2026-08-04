package com.aigate.chat.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class Haptics(
	private val enabled: Boolean,
	private val feedback: HapticFeedback?,
) {
	fun tap() {
		if (!enabled) return
		feedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
	}

	fun strong() {
		if (!enabled) return
		feedback?.performHapticFeedback(HapticFeedbackType.LongPress)
	}
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
	val feedback = LocalHapticFeedback.current
	return remember(enabled, feedback) { Haptics(enabled, feedback) }
}
