package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Returns the [VisualTransformation] that alters the output of the text in an input field by
 * appending a '.' followed by the [fileExtension] to the display text.
 */
@Composable
fun nonEditableExtensionVisualTransformation(
    fileExtension: String?,
    fileExtensionColor: Color = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
): VisualTransformation =
    remember(fileExtension, fileExtensionColor) {
        NonEditableExtensionVisualTransformation(
            fileExtension = fileExtension,
            fileExtensionColor = fileExtensionColor,
        )
    }

/**
 * Alters the visual output of the text in an input field.
 *
 * This will append a '.' followed by the [fileExtension] to the display text but not allow users
 * to alter that text. If the `fileExtension` is null, then no alteration will occur.
 */
private class NonEditableExtensionVisualTransformation(
    private val fileExtension: String?,
    private val fileExtensionColor: Color,
) : VisualTransformation {
    override fun filter(
        text: AnnotatedString,
    ): TransformedText = TransformedText(
        text.buildTransformedAnnotatedString(
            fileExtension = fileExtension,
            fileExtensionColor = fileExtensionColor,
        ),
        text.getOffsetMapping(),
    )
}

private fun AnnotatedString.buildTransformedAnnotatedString(
    fileExtension: String?,
    fileExtensionColor: Color,
): AnnotatedString {
    val extension = fileExtension ?: return this
    val builder = AnnotatedString.Builder()
    builder.append(this)
    builder.withStyle(SpanStyle(color = fileExtensionColor)) { append(".$extension") }
    return builder.toAnnotatedString()
}

private fun AnnotatedString.getOffsetMapping(): OffsetMapping =
    object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            // We always use the regular offset here since the extension is off-limits.
            return offset
        }

        override fun transformedToOriginal(
            offset: Int,
        ): Int = if (offset > this@getOffsetMapping.length) {
            // If we are in the extension space, pull us back into the regular text.
            this@getOffsetMapping.length
        } else {
            // We are within the limits, so leave the offset alone.
            offset
        }
    }
