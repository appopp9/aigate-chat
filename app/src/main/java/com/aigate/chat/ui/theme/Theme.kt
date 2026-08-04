package com.aigate.chat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- پالت پایه‌ی Neobrutalism ----
val NeoYellow = Color(0xFFFFD93D)
val NeoPink = Color(0xFFFF5C8A)
val NeoCyan = Color(0xFF4CC9F0)
val NeoLime = Color(0xFFB5F44A)
val NeoPurple = Color(0xFFA78BFA)
val NeoOrange = Color(0xFFFF8A3D)
val NeoBlack = Color(0xFF101010)
val NeoCream = Color(0xFFFFF6E9)
val NeoWhite = Color(0xFFFFFFFF)
val NeoDarkBg = Color(0xFF15151A)
val NeoDarkCard = Color(0xFF212129)

val AccentPalette = listOf(NeoYellow, NeoPink, NeoCyan, NeoLime, NeoPurple, NeoOrange)

/** یک تم رنگی کامل */
data class NeoThemeSpec(
	val name: String,
	val accent: Color,
	val secondary: Color,
	val lightBackground: Color,
	val darkBackground: Color,
	val darkSurface: Color,
)

val NeoThemes: List<NeoThemeSpec> = listOf(
	NeoThemeSpec("کلاسیک زرد", NeoYellow, NeoPink, NeoCream, NeoDarkBg, NeoDarkCard),
	NeoThemeSpec("صورتی داغ", NeoPink, NeoPurple, Color(0xFFFFF0F4), Color(0xFF1A1218), Color(0xFF2A1D25)),
	NeoThemeSpec("فیروزه‌ای سرد", NeoCyan, NeoLime, Color(0xFFEFF9FF), Color(0xFF101A20), Color(0xFF19262E)),
	NeoThemeSpec("لایم اسیدی", NeoLime, NeoOrange, Color(0xFFF4FFE8), Color(0xFF13190F), Color(0xFF1D2618)),
	NeoThemeSpec("بنفش کیهانی", NeoPurple, NeoCyan, Color(0xFFF5F0FF), Color(0xFF16121F), Color(0xFF221B2F)),
	NeoThemeSpec("نارنجی آتشی", NeoOrange, NeoYellow, Color(0xFFFFF3E8), Color(0xFF1C1409), Color(0xFF2A1F12)),
)

fun themeSpecAt(index: Int): NeoThemeSpec =
	NeoThemes.getOrElse(index) { NeoThemes[0] }

@Composable
fun AiGateTheme(
	darkTheme: Boolean,
	themeIndex: Int = 0,
	content: @Composable () -> Unit,
) {
	val spec = themeSpecAt(themeIndex)

	val lightColors = lightColorScheme(
		primary = NeoBlack,
		onPrimary = NeoWhite,
		primaryContainer = spec.accent,
		onPrimaryContainer = NeoBlack,
		secondary = spec.secondary,
		onSecondary = NeoBlack,
		secondaryContainer = spec.secondary,
		onSecondaryContainer = NeoBlack,
		tertiary = spec.accent,
		onTertiary = NeoBlack,
		background = spec.lightBackground,
		onBackground = NeoBlack,
		surface = NeoWhite,
		onSurface = NeoBlack,
		surfaceVariant = Color(0xFFF2ECE0),
		onSurfaceVariant = Color(0xFF3A3A3A),
		outline = NeoBlack,
		error = Color(0xFFE23636),
		onError = NeoWhite,
	)

	val darkColors = darkColorScheme(
		primary = spec.accent,
		onPrimary = NeoBlack,
		primaryContainer = spec.accent,
		onPrimaryContainer = NeoBlack,
		secondary = spec.secondary,
		onSecondary = NeoBlack,
		secondaryContainer = spec.secondary,
		onSecondaryContainer = NeoBlack,
		tertiary = spec.accent,
		onTertiary = NeoBlack,
		background = spec.darkBackground,
		onBackground = Color(0xFFF5F2EA),
		surface = spec.darkSurface,
		onSurface = Color(0xFFF5F2EA),
		surfaceVariant = Color(0xFF2C2C36),
		onSurfaceVariant = Color(0xFFCFCBC2),
		outline = Color(0xFFF5F2EA),
		error = Color(0xFFFF6B6B),
		onError = NeoBlack,
	)

	MaterialTheme(
		colorScheme = if (darkTheme) darkColors else lightColors,
		typography = AppTypography,
		content = content,
	)
}
