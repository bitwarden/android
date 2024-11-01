@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.graphics.toColorInt
import java.net.URI
import java.net.URISyntaxException
import java.text.Normalizer
import java.util.Locale
import kotlin.math.floor

/**
 * This character takes up no space but can be used to ensure a string is not empty. It can also
 * be used to insert "safe" line-break positions in a string.
 *
 * Note: Is a string only contains this character, it is _not_ considered blank.
 */
const val ZERO_WIDTH_CHARACTER: String = "\u200B"

/**
 * URI scheme for a native Android application.
 */
private const val ANDROID_APP_URI_SCHEME: String = "androidapp://"

/**
 * Returns the original [String] only if:
 *
 * - it is non-null
 * - it is not blank (where blank refers to empty strings of those containing only white space)
 *
 * Otherwise [ZERO_WIDTH_CHARACTER] is returned.
 */
fun String?.orZeroWidthSpace(): String = this.orNullIfBlank() ?: ZERO_WIDTH_CHARACTER

/**
 * Whether or not string is a valid email address.
 *
 * This just checks if the string contains the "@" symbol.
 */
fun String.isValidEmail(): Boolean = contains("@")

/**
 * Returns `true` if the given [String] is a non-blank, valid URI and `false` otherwise.
 *
 * Note that this does not require the URI to contain a URL scheme like `https://`.
 */
fun String.isValidUri(): Boolean =
    try {
        URI.create(this)
        this.isNotBlank()
    } catch (_: IllegalArgumentException) {
        false
    }

/**
 * Returns the host name (or path as a fallback) for the given [String] if it represents a
 * well-formed URI, or `null` otherwise.
 */
fun String.toHostOrPathOrNull(): String? {
    val uri = try {
        URI(this)
    } catch (e: URISyntaxException) {
        return null
    }
    return uri.host ?: uri.path
}

/**
 * Returns the original [String] prefixed with `androidapp://` if it doesn't already contain.
 */
fun String.toAndroidAppUriString(): String {
    return if (this.startsWith(ANDROID_APP_URI_SCHEME)) this else "$ANDROID_APP_URI_SCHEME$this"
}

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
 * Returns the given [String] in a lowercase form using the primary [Locale] from the current
 * context.
 */
@Composable
fun String.lowercaseWithCurrentLocal(): String {
    return lowercase(LocalContext.current.resources.configuration.locales[0])
}

/**
 * A helper method to apply the [visualTransformation] to the given string and and returns the
 * transformed [AnnotatedString].
 */
@Composable
fun String.withVisualTransformation(
    visualTransformation: VisualTransformation,
): AnnotatedString =
    remember(key1 = this) {
        visualTransformation.filter(toAnnotatedString()).text
    }

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
        if (widthPx > 0 && this.isNotEmpty()) {
            val stringLengthPx = measurer
                .measure(text = this, softWrap = false, style = monospacedTextStyle)
                .size
                .width
            val linesRequired = stringLengthPx / widthPx
            val charsPerLine = floor(this.length / linesRequired).toInt()
            this
                .chunked(size = charsPerLine)
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
 * Converts a hex string to a [Color].
 *
 * Supported formats:
 * - "rrggbb" / "#rrggbb"
 * - "aarrggbb" / "#aarrggbb"
 */
fun String.hexToColor(): Color = if (startsWith("#")) {
    Color(toColorInt())
} else {
    Color("#$this".toColorInt())
}

/**
 * Creates a new [String] that represents a unique color in the hex representation (`"#AARRGGBB"`).
 * This can be applied to any [String] in order to provide some deterministic color value based on
 * arbitrary [String] properties.
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress("MagicNumber")
fun String.toHexColorRepresentation(): String {
    // Produces a string with exactly two hexadecimal digits.
    // Ex:
    // 0 -> "00"
    // 10 -> "0a"
    // 1000 -> "e8"
    fun Int.toTwoDigitHexString(): String =
        this.toHexString().takeLast(2)

    // Calculates separate red, blue, and green values from different positions in the hash and then
    // combines then into a single color.
    val hash = this.hashCode()
    val red = (hash and 0x0000FF).toTwoDigitHexString()
    val green = ((hash and 0x00FF00) shr 8).toTwoDigitHexString()
    val blue = ((hash and 0xFF0000) shr 16).toTwoDigitHexString()
    return "#ff$red$green$blue"
}

/**
 * Returns a copy of this string having its first letter titlecased using the rules of the specified
 * [locale], or the original string if it's empty or already starts with a title case letter.
 */
fun String.capitalize(locale: Locale = Locale.getDefault()): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

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
