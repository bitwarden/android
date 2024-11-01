package com.x8bit.bitwarden.ui.platform.theme.type

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
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The default [BitwardenTypography] for the app.
 */
val bitwardenTypography: BitwardenTypography = BitwardenTypography(
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
    eyebrowMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFamily = FontFamily(Font(R.font.dm_sans_bold)),
        fontWeight = FontWeight.W700,
        letterSpacing = 0.6.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)

/**
 * Derives a Material [Typography] from the [BitwardenTypography].
 */
fun BitwardenTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
)

@Preview(showBackground = true)
@Composable
private fun BitwardenTypography_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Display large", style = bitwardenTypography.displayLarge)
            Text(text = "Display medium", style = bitwardenTypography.displayMedium)
            Text(text = "Display small", style = bitwardenTypography.displaySmall)
            Text(text = "Headline large", style = bitwardenTypography.headlineLarge)
            Text(text = "Headline medium", style = bitwardenTypography.headlineMedium)
            Text(text = "Headline small", style = bitwardenTypography.headlineSmall)
            Text(text = "Title large", style = bitwardenTypography.titleLarge)
            Text(text = "Title medium", style = bitwardenTypography.titleMedium)
            Text(text = "Title small", style = bitwardenTypography.titleSmall)
            Text(text = "Body large", style = bitwardenTypography.bodyLarge)
            Text(text = "Body medium", style = bitwardenTypography.bodyMedium)
            Text(text = "Body small", style = bitwardenTypography.bodySmall)
            Text(text = "Label large", style = bitwardenTypography.labelLarge)
            Text(text = "Label medium", style = bitwardenTypography.labelMedium)
            Text(text = "Label small", style = bitwardenTypography.labelSmall)
            Text(text = "Sensitive info small", style = bitwardenTypography.sensitiveInfoSmall)
            Text(text = "Sensitive info medium", style = bitwardenTypography.sensitiveInfoMedium)
            Text(text = "Eyebrow medium", style = bitwardenTypography.eyebrowMedium)
        }
    }
}
