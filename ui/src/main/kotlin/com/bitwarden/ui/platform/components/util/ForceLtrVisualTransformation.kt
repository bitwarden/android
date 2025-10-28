package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Unicode characters for forcing LTR direction
private const val LRO = "\u202A"
private const val PDF = "\u202C"

/**
 * A [VisualTransformation] that forces the output to have an LTR text direction.
 *
 * This is useful for password fields where the input should always be LTR, even when the rest of
 * the UI is RTL.
 */
private object ForceLtrVisualTransformation : VisualTransformation {
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
 * Remembers a [ForceLtrVisualTransformation] for the given [transformations].
 *
 * This is an optimization to avoid creating a new [ForceLtrVisualTransformation] on every
 * recomposition.
 */
@Composable
fun forceLtrVisualTransformation(): VisualTransformation = remember {
    ForceLtrVisualTransformation
}
