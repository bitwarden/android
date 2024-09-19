package com.x8bit.bitwarden.ui.platform.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x8bit.bitwarden.R

val Typography: Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    displayMedium = TextStyle(
        fontSize = 44.sp,
        lineHeight = 52.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 28.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_medium)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_semi_bold)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_regular)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)

val nonMaterialTypography: NonMaterialTypography = NonMaterialTypography(
    sensitiveInfoSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular_mono)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    sensitiveInfoMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular_mono)),
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    bodySmallProminent = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W700,
        letterSpacing = 0.4.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    labelMediumProminent = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontWeight = FontWeight.W600,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    eyebrowMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_bold)),
        fontWeight = FontWeight.W700,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
)

/**
 * Models typography that live outside of the Material Theme spec.
 */
data class NonMaterialTypography(
    val bodySmallProminent: TextStyle,
    val labelMediumProminent: TextStyle,
    val sensitiveInfoSmall: TextStyle,
    val sensitiveInfoMedium: TextStyle,
    val eyebrowMedium: TextStyle,
)

@Preview(showBackground = true)
@Composable
private fun Typography_preview() {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Display large", style = Typography.displayLarge)
        Text(text = "Display medium", style = Typography.displayMedium)
        Text(text = "Display small", style = Typography.displaySmall)
        Text(text = "Headline large", style = Typography.headlineLarge)
        Text(text = "Headline medium", style = Typography.headlineMedium)
        Text(text = "Headline small", style = Typography.headlineSmall)
        Text(text = "Title large", style = Typography.titleLarge)
        Text(text = "Title medium", style = Typography.titleMedium)
        Text(text = "Title small", style = Typography.titleSmall)
        Text(text = "Body large", style = Typography.bodyLarge)
        Text(text = "Body medium", style = Typography.bodyMedium)
        Text(text = "Body small", style = Typography.bodySmall)
        Text(text = "Label large", style = Typography.labelLarge)
        Text(text = "Label medium", style = Typography.labelMedium)
        Text(text = "Label small", style = Typography.labelSmall)
        Text(text = "Sensitive info small", style = nonMaterialTypography.sensitiveInfoSmall)
        Text(text = "Sensitive info medium", style = nonMaterialTypography.sensitiveInfoMedium)
        Text(text = "Body small prominent", style = nonMaterialTypography.bodySmallProminent)
        Text(text = "Label medium prominent", style = nonMaterialTypography.labelMediumProminent)
        Text(text = "Eyebrow medium", style = nonMaterialTypography.eyebrowMedium)
    }
}
