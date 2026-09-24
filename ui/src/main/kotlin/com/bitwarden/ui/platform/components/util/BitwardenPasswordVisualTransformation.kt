package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.bitwarden.ui.platform.base.util.toAnnotatedString

/**
 * Returns a [VisualTransformation] that masks text with [mask]. When [staticCharacterCount] is
 * non-null and the text is non-empty, the transformed text contains that number of mask
 * characters. Blank text is never masked.
 */
@Composable
fun passwordVisualTransformation(
    mask: Char = '\u2022',
    staticCharacterCount: Int? = null,
): VisualTransformation =
    remember(mask, staticCharacterCount) {
        BitwardenPasswordVisualTransformation(
            mask = mask,
            staticCharacterCount = staticCharacterCount,
        )
    }

private class BitwardenPasswordVisualTransformation(
    private val mask: Char,
    private val staticCharacterCount: Int?,
) : VisualTransformation {
    override fun filter(
        text: AnnotatedString,
    ): TransformedText = TransformedText(
        text = mask
            .toString()
            .repeat(staticCharacterCount?.takeIf { text.text.isNotEmpty() } ?: text.text.length)
            .toAnnotatedString(),
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(
                offset: Int,
            ): Int = staticCharacterCount?.let { offset.coerceAtMost(it) } ?: offset

            override fun transformedToOriginal(
                offset: Int,
            ): Int = offset.coerceAtMost(text.length)
        },
    )
}
