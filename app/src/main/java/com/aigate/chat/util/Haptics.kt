package com.aigate.chat.util

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** لمسه (haptic feedback) با احترام به تنظیم کاربر و نسخه‌ی اندروید */
class Haptics(private val view: android.view.View, private val enabled: Boolean) {

	fun tap() {
		if (!enabled) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
		} else {
			view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
		}
	}

	fun strong() {
		if (!enabled) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
		} else {
			view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
		}
	}
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
	val view = LocalView.current
	return remember(view, enabled) { Haptics(view, enabled) }
}