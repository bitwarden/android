package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Unicode characters for forcing LTR direction
internal const val LRO = "\u202A"
internal const val PDF = "\u202C"

/**
 * A [VisualTransformation] that forces the output to have an LTR text direction.
 *
 * This transformation wraps text with Unicode directional control characters (LRO/PDF)
 * to ensure left-to-right rendering regardless of the UI's locale or text direction.
 *
 * ## When to Use
 *
 * Apply this transformation to fields containing **standardized, technical data** that is
 * always interpreted from left-to-right, regardless of locale:
 * - Passwords and sensitive authentication data
 * - Social Security Numbers (SSN)
 * - Driver's license numbers
 * - Passport numbers
 * - Payment card numbers
 * - Email addresses (technical format)
 * - Phone numbers (standardized format)
 * - URIs and technical identifiers
 *
 * ## When NOT to Use
 *
 * Do NOT apply this transformation to **locale-dependent text** that may legitimately
 * use RTL scripts:
 * - Personal names (may use Arabic, Hebrew, etc.)
 * - Company names
 * - Addresses
 * - Usernames (user choice)
 * - Notes and other free-form text
 *
 * ## Implementation Notes
 *
 * - Only applies LTR transformation when text is **visible**
 * - Do NOT use with obscured text (e.g., password bullets) as masked characters
 *   are directionally neutral
 * - Can be composed with other transformations using [compoundVisualTransformation]
 *
 * @see compoundVisualTransformation
 */
internal object ForceLtrVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val forcedLtrText = buildAnnotatedString {
            append(LRO)
            append(text)
            append(PDF)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                (offset - 1).coerceIn(0, text.length)
        }

        return TransformedText(forcedLtrText, offsetMapping)
    }
}

/**
 * Remembers a [ForceLtrVisualTransformation] transformation.
 *
 * This is an optimization to avoid creating a new [ForceLtrVisualTransformation] on every
 * recomposition.
 */
@Composable
fun forceLtrVisualTransformation(): VisualTransformation = remember {
    ForceLtrVisualTransformation
}
