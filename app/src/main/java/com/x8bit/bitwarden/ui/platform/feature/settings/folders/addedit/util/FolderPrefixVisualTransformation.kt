package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Convenience function to provide a remembered [FolderPrefixVisualTransformation] within
 * a composable scope.
 */
@Composable
fun folderPrefixVisualTransformation(
    parentFolderPath: String?,
): VisualTransformation {
    val prefixSpanStyle = SpanStyle(
        color = BitwardenTheme.colorScheme.text.secondary,
        fontStyle = BitwardenTheme.typography.labelMedium.fontStyle,
    )
    val textSpanStyle = SpanStyle(
        color = BitwardenTheme.colorScheme.text.primary,
        fontStyle = BitwardenTheme.typography.bodyMedium.fontStyle,
    )
    return remember {
        FolderPrefixVisualTransformation(
            parentFolderPath = parentFolderPath,
            prefixSpanStyle = prefixSpanStyle,
            textSpanStyle = textSpanStyle,
        )
    }
}

/**
 * Visual transformation which will prefix a parent folder's path (abridged to the show
 * "../parentA/" if there are more than one parent in the path) when adding a new sub folder (child)
 * in a TextField.
 *
 * @property parentFolderPath An optional string which if delimited by "/" will be
 * parsed to show a formatted prefix before the entered text. If null the entered text is not
 * transformed.
 * @property prefixSpanStyle SpanStyle to apply to the folder prefix if present.
 * @property textSpanStyle SpanStyle to apply to the entered text.
 */
private class FolderPrefixVisualTransformation(
    private val parentFolderPath: String?,
    private val prefixSpanStyle: SpanStyle,
    private val textSpanStyle: SpanStyle,
) : VisualTransformation {
    private var folderPrefix: String = ""

    init {
        val folders = parentFolderPath?.split("/").orEmpty()
        folderPrefix =
            if (folders.size > 1) {
                "../${folders.last()}/"
            } else if (folders.size == 1) {
                "${folders.last()}/"
            } else {
                ""
            }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        return parentFolderPath
            ?.let {
                TransformedText(
                    prefixFolderPath(text.toString()),
                    FolderPrefixOffsetMapping(),
                )
            }
            ?: TransformedText(
                text,
                OffsetMapping.Identity,
            )
    }

    private fun prefixFolderPath(text: String): AnnotatedString {
        return buildAnnotatedString {
            withStyle(style = prefixSpanStyle) {
                append(folderPrefix)
            }
            withStyle(style = textSpanStyle) {
                append(text)
            }
        }
    }

    private inner class FolderPrefixOffsetMapping : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return offset + folderPrefix.length
        }

        override fun transformedToOriginal(offset: Int): Int {
            return if (offset <= folderPrefix.length) {
                0
            } else {
                offset - folderPrefix.length
            }
        }
    }
}
