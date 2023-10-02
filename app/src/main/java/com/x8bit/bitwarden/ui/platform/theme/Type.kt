package com.x8bit.bitwarden.ui.platform.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.x8bit.bitwarden.R

val Typography: Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = (0).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontWeight = FontWeight.W500,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp,
    ),
)
