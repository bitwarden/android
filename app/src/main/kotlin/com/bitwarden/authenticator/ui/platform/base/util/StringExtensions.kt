package com.bitwarden.authenticator.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import java.text.Normalizer
import kotlin.math.floor

/**
 * This character takes up no space but can be used to ensure a string is not empty. It can also
 * be used to insert "safe" line-break positions in a string.
 *
 * Note: Is a string only contains this charactor, it is _not_ considered blank.
 */
const val ZERO_WIDTH_CHARACTER: String = "\u200B"
/**
 * Returns the original [String] only if:
 *
 * - it is non-null
 * - it is not blank (where blank refers to empty strings of those containing only white space)
 *
 * Otherwise `null` is returned.
 */
fun String?.orNullIfBlank(): String? = this?.takeUnless { it.isBlank() }

/**
 * Returns a new [String] that includes line breaks after [widthPx] worth of text. This is useful
 * for long values that need to smoothly flow onto the next line without the OS inserting line
 * breaks earlier at special characters.
 *
 * Note that the internal calculation used assumes that [monospacedTextStyle] is based on a
 * monospaced font like Roboto Mono.
 */
@Composable
fun String.withLineBreaksAtWidth(
    widthPx: Float,
    monospacedTextStyle: TextStyle,
): String {
    val measurer = rememberTextMeasurer()
    return remember(this, widthPx, monospacedTextStyle) {
        val characterSizePx = measurer
            .measure("*", monospacedTextStyle)
            .size
            .width
        val perLineCharacterLimit = floor(widthPx / characterSizePx).toInt()
        if (widthPx > 0) {
            this
                .chunked(perLineCharacterLimit)
                .joinToString(separator = "\n")
        } else {
            this
        }
    }
}

/**
 * Returns the [String] as an [AnnotatedString].
 */
fun String.toAnnotatedString(): AnnotatedString = AnnotatedString(text = this)

/**
 * Normalizes the [String] by removing diacritics, such as an umlaut.
 *
 * Example: áéíóů --> aeiou
 */
fun String.removeDiacritics(): String =
    "\\p{InCombiningDiacriticalMarks}+"
        .toRegex()
        .replace(
            Normalizer.normalize(this, Normalizer.Form.NFKD),
            "",
        )
