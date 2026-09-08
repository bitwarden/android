package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Returns a [VisualTransformation] that masks text with [mask]. When [staticCharacterCount] is
 * non-null, the transformed text always contains that number of mask characters.
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
        AnnotatedString(
            mask.toString().repeat(n = staticCharacterCount ?: text.text.length),
        ),
        object : OffsetMapping {
            override fun originalToTransformed(
                offset: Int,
            ): Int = staticCharacterCount?.let { offset.coerceAtMost(it) } ?: offset

            override fun transformedToOriginal(
                offset: Int,
            ): Int = offset.coerceAtMost(text.length)
        },
    )
}
