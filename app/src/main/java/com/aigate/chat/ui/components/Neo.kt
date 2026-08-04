package com.aigate.chat.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NeoShape: Shape = RoundedCornerShape(6.dp)
val NeoShapeLarge: Shape = RoundedCornerShape(12.dp)

/** سایهی سخت و جابه‌جاشدهی سبک نئوبروتالیسم */
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

/** کارت نئوبروتال */
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
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()
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
			.then(
				if (onClick != null) Modifier.clickable(
					interactionSource = interactionSource,
					indication = null,
					onClick = onClick,
				) else Modifier
			)
			.padding(contentPadding)
	) {
		content()
	}
}

/** دکمهی نئوبروتال با انیمیشن فشردن */
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
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()
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
			.background(if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant)
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
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center,
		) {
			if (icon != null) {
				Icon(
					icon,
					contentDescription = null,
					modifier = Modifier.size(18.dp),
					tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Spacer(Modifier.size(8.dp))
			}
			Text(
				text,
				style = MaterialTheme.typography.labelLarge,
				color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

/** دکمهی آیکونی مربعی */
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
	val border = MaterialTheme.colorScheme.outline
	val interactionSource = remember { MutableInteractionSource() }
	val pressed by interactionSource.collectIsPressedAsState()
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
			.background(if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant)
			.border(2.5.dp, border, NeoShape)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				enabled = enabled,
				onClick = onClick,
			),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			icon,
			contentDescription = contentDescription,
			tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(boxSize * 0.45f),
		)
	}
}

/** چیپ رنگی */
@Composable
fun NeoChip(
	text: String,
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.surface,
	selected: Boolean = false,
	icon: ImageVector? = null,
	onClick: (() -> Unit)? = null,
) {
	val border = MaterialTheme.colorScheme.outline
	val shape = RoundedCornerShape(22.dp)
	Box(
		modifier = modifier
			.clip(shape)
			.background(color)
			.border(if (selected) 3.dp else 2.dp, border, shape)
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
