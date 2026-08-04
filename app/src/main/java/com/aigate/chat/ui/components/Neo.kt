package com.aigate.chat.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// false = استایل نئوبروتال، true = استایل ساده شبیه ChatGPT
val LocalFlatStyle = compositionLocalOf { false }

val NeoShape: Shape = RoundedCornerShape(6.dp)
val NeoShapeLarge: Shape = RoundedCornerShape(12.dp)
val FlatShape: Shape = RoundedCornerShape(16.dp)
val FlatShapeLarge: Shape = RoundedCornerShape(20.dp)

fun Modifier.neoShadow(color: Color, offset: Dp = 5.dp, radius: Dp = 6.dp): Modifier =
	this.drawBehind {
		val dx = offset.toPx()
		val r = radius.toPx()
		drawRoundRect(
			color = color,
			topLeft = Offset(-dx, dx),
			size = size,
			cornerRadius = CornerRadius(r, r),
		)
	}

@Composable
fun NeoBox(
	modifier: Modifier = Modifier,
	background: Color = MaterialTheme.colorScheme.surface,
	shape: Shape = NeoShape,
	cornerRadius: Dp = 6.dp,
	borderWidth: Dp = 2.5.dp,
	shadowOffset: Dp = 5.dp,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	onClick: (() -> Unit)? = null,
	content: @Composable () -> Unit,
) {
	val flat = LocalFlatStyle.current
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()

	val clickableModifier = if (onClick != null) {
		Modifier.clickable(
			interactionSource = interactionSource,
			indication = null,
			onClick = onClick,
		)
	} else {
		Modifier
	}

	if (flat) {
		val flatShape: Shape = FlatShape
		val pressAlpha by animateFloatAsState(
			targetValue = if (pressed && onClick != null) 0.72f else 1f,
			animationSpec = tween(durationMillis = 120),
			label = "flatBoxPress",
		)
		Box(
			modifier = modifier
				.alpha(pressAlpha)
				.clip(flatShape)
				.background(background)
				.border(1.dp, border.copy(alpha = 0.16f), flatShape)
				.then(clickableModifier)
				.padding(contentPadding)
		) {
			content()
		}
		return
	}

	val shift by animateDpAsState(
		targetValue = if (pressed && onClick != null) shadowOffset else 0.dp,
		animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
		label = "neoPress",
	)

	Box(
		modifier = modifier
			.offset(x = -shift, y = shift)
			.neoShadow(border, shadowOffset - shift, cornerRadius)
			.clip(shape)
			.background(background)
			.border(borderWidth, border, shape)
			.then(clickableModifier)
			.padding(contentPadding)
	) {
		content()
	}
}

@Composable
fun NeoButton(
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.primary,
	contentColor: Color = MaterialTheme.colorScheme.onPrimary,
	icon: ImageVector? = null,
	enabled: Boolean = true,
) {
	val flat = LocalFlatStyle.current
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()
	val disabledColor = MaterialTheme.colorScheme.surfaceVariant
	val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant

	val labelRow: @Composable () -> Unit = {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center,
		) {
			if (icon != null) {
				Icon(
					icon,
					contentDescription = null,
					modifier = Modifier.size(18.dp),
					tint = if (enabled) contentColor else disabledContent,
				)
				Spacer(Modifier.size(8.dp))
			}
			Text(
				text,
				style = MaterialTheme.typography.labelLarge,
				color = if (enabled) contentColor else disabledContent,
			)
		}
	}

	if (flat) {
		val scaleValue by animateFloatAsState(
			targetValue = if (pressed && enabled) 0.97f else 1f,
			animationSpec = tween(durationMillis = 110),
			label = "flatBtnPress",
		)
		Box(
			modifier = modifier
				.scale(scaleValue)
				.clip(RoundedCornerShape(24.dp))
				.background(if (enabled) containerColor else disabledColor)
				.clickable(
					interactionSource = interactionSource,
					indication = null,
					enabled = enabled,
					onClick = onClick,
				)
				.padding(horizontal = 18.dp, vertical = 12.dp),
			contentAlignment = Alignment.Center,
		) {
			labelRow()
		}
		return
	}

	val shift by animateDpAsState(
		targetValue = if (pressed && enabled) 4.dp else 0.dp,
		animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
		label = "btnPress",
	)

	Box(
		modifier = modifier
			.offset(x = -shift, y = shift)
			.neoShadow(if (enabled) border else Color.Transparent, 4.dp - shift, 6.dp)
			.clip(NeoShape)
			.background(if (enabled) containerColor else disabledColor)
			.border(2.5.dp, border, NeoShape)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				enabled = enabled,
				onClick = onClick,
			)
			.padding(horizontal = 18.dp, vertical = 12.dp),
		contentAlignment = Alignment.Center,
	) {
		labelRow()
	}
}

@Composable
fun NeoIconButton(
	icon: ImageVector,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.surface,
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	boxSize: Dp = 46.dp,
	contentDescription: String? = null,
) {
	val flat = LocalFlatStyle.current
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()
	val surfaceColor = MaterialTheme.colorScheme.surface
	val disabledColor = MaterialTheme.colorScheme.surfaceVariant
	val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant

	val iconContent: @Composable () -> Unit = {
		Icon(
			icon,
			contentDescription = contentDescription,
			tint = if (enabled) contentColor else disabledContent,
			modifier = Modifier.size(boxSize * 0.45f),
		)
	}

	if (flat) {
		val scaleValue by animateFloatAsState(
			targetValue = if (pressed && enabled) 0.9f else 1f,
			animationSpec = tween(durationMillis = 110),
			label = "flatIconPress",
		)
		val flatBackground = if (containerColor == surfaceColor) Color.Transparent else containerColor
		Box(
			modifier = modifier
				.size(boxSize)
				.scale(scaleValue)
				.clip(RoundedCornerShape(boxSize / 2))
				.background(if (enabled) flatBackground else disabledColor)
				.clickable(
					interactionSource = interactionSource,
					indication = null,
					enabled = enabled,
					onClick = onClick,
				),
			contentAlignment = Alignment.Center,
		) {
			iconContent()
		}
		return
	}

	val shift by animateDpAsState(
		targetValue = if (pressed && enabled) 3.dp else 0.dp,
		animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
		label = "iconPress",
	)

	Box(
		modifier = modifier
			.size(boxSize)
			.offset(x = -shift, y = shift)
			.neoShadow(if (enabled) border else Color.Transparent, 3.dp - shift, 6.dp)
			.clip(NeoShape)
			.background(if (enabled) containerColor else disabledColor)
			.border(2.5.dp, border, NeoShape)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				enabled = enabled,
				onClick = onClick,
			),
		contentAlignment = Alignment.Center,
	) {
		iconContent()
	}
}

@Composable
fun NeoChip(
	text: String,
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.surface,
	selected: Boolean = false,
	icon: ImageVector? = null,
	onClick: (() -> Unit)? = null,
) {
	val flat = LocalFlatStyle.current
	val border = MaterialTheme.colorScheme.outline
	val shape = RoundedCornerShape(22.dp)
	val borderColor = if (flat) border.copy(alpha = if (selected) 0.45f else 0.16f) else border
	val borderWidth = if (flat) 1.dp else if (selected) 3.dp else 2.dp
	Box(
		modifier = modifier
			.clip(shape)
			.background(color)
			.border(borderWidth, borderColor, shape)
			.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
			.padding(horizontal = 12.dp, vertical = 7.dp)
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			if (icon != null) {
				Icon(
					icon,
					contentDescription = null,
					modifier = Modifier.size(15.dp),
					tint = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.size(6.dp))
			}
			Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
		}
	}
}
