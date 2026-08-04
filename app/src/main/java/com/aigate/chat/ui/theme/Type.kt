package com.aigate.chat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aigate.chat.R

/** فونت فارسی وزیر */
val Vazir = FontFamily(
	Font(R.font.vazir_light, FontWeight.Light),
	Font(R.font.vazir_regular, FontWeight.Normal),
	Font(R.font.vazir_medium, FontWeight.Medium),
	Font(R.font.vazir_bold, FontWeight.Bold),
)

val AppTypography = Typography(
	displayLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 34.sp),
	displayMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 28.sp),
	headlineLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 26.sp),
	headlineMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 22.sp),
	headlineSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 19.sp),
	titleLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 18.sp),
	titleMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 16.sp),
	titleSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp),
	bodyLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 26.sp),
	bodyMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 24.sp),
	bodySmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),
	labelLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 14.sp),
	labelMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp),
	labelSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
