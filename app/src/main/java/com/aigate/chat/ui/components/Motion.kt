package com.aigate.chat.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable

// شتاب حرکت: در حالت ساده تندتر و خطی‌تر، در حالت نئوبروتالیسم کشدارتر و فنری
val NeoEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun motionFast(): Int = if (LocalFlatStyle.current) 90 else 170

@Composable
fun motionMedium(): Int = if (LocalFlatStyle.current) 150 else 280

@Composable
fun motionSlow(): Int = if (LocalFlatStyle.current) 220 else 400

@Composable
fun <T> motionSpec(): FiniteAnimationSpec<T> =
	if (LocalFlatStyle.current) {
		tween(durationMillis = motionFast(), easing = NeoEasing)
	} else {
		spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)
	}

@Composable
fun motionEnter(): EnterTransition {
	val flat = LocalFlatStyle.current
	val duration = motionMedium()
	return fadeIn(animationSpec = tween(durationMillis = duration, easing = NeoEasing)) +
		scaleIn(
			initialScale = if (flat) 0.98f else 0.9f,
			animationSpec = tween(durationMillis = duration, easing = NeoEasing),
		) +
		expandVertically(animationSpec = tween(durationMillis = duration, easing = NeoEasing))
}

@Composable
fun motionExit(): ExitTransition {
	val flat = LocalFlatStyle.current
	val duration = motionFast()
	return fadeOut(animationSpec = tween(durationMillis = duration, easing = NeoEasing)) +
		scaleOut(
			targetScale = if (flat) 0.98f else 0.92f,
			animationSpec = tween(durationMillis = duration, easing = NeoEasing),
		) +
		shrinkVertically(animationSpec = tween(durationMillis = duration, easing = NeoEasing))
}
