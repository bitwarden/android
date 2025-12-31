package com.bitwarden.ui.platform.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Returns a [TextStyle] that forces left-to-right text direction while maintaining
 * locale-aware alignment.
 *
 * This extension is designed for sensitive alphanumeric content (passwords, TOTP codes)
 * that must always read left-to-right regardless of system locale, but should align
 * according to the layout direction (right-aligned in RTL locales, left-aligned in LTR).
 *
 * **Implementation:**
 * - Sets `textDirection = TextDirection.Ltr` to force LTR reading order
 * - Sets `textAlign` conditionally:
 *   - `TextAlign.End` in RTL layouts (aligns to right side)
 *   - `TextAlign.Start` in LTR layouts (aligns to left side)
 *
 * **Use cases:**
 * - Password fields that should read "Pass123!" not "!321ssaP" in RTL locales
 * - TOTP verification codes that should read "123 456" not "654 321"
 * - Any alphanumeric content requiring LTR reading with locale-aware positioning
 *
 * @return A merged [TextStyle] with forced LTR direction and locale-aware alignment.
 */
@OmitFromCoverage
@Composable
fun TextStyle.withForcedLtr(): TextStyle {
    val layoutDirection = LocalLayoutDirection.current
    return merge(
        TextStyle(
            textDirection = TextDirection.Ltr,
            textAlign = when (layoutDirection) {
                LayoutDirection.Rtl -> TextAlign.End
                LayoutDirection.Ltr -> TextAlign.Start
            },
        ),
    )
}
