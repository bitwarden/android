package com.x8bit.bitwarden.ui.platform.components.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

/**
 * Returns the [VisualTransformation] that alters the output of the text in an input field by
 * applying different colors to the digits and special characters, letters will remain unaffected.
 */
@Composable
fun nonLetterColorVisualTransformation(): VisualTransformation =
    NonLetterColorVisualTransformation(
        digitColor = MaterialTheme.colorScheme.primary,
        specialCharacterColor = MaterialTheme.colorScheme.error,
    )

/**
 * Alters the visual output of the text in an input field.
 *
 * All numbers in the text will have the [digitColor] applied to it and special characters will
 * have the [specialCharacterColor] applied.
 */
private class NonLetterColorVisualTransformation(
    private val digitColor: Color,
    private val specialCharacterColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            buildTransformedAnnotatedString(text.toString()),
            OffsetMapping.Identity,
        )

    private fun buildTransformedAnnotatedString(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        text.toCharArray().forEach { char ->
            when {
                char.isDigit() -> builder.withStyle(SpanStyle(color = digitColor)) { append(char) }

                !char.isLetter() -> {
                    builder.withStyle(SpanStyle(color = specialCharacterColor)) { append(char) }
                }

                else -> builder.append(char)
            }
        }
        return builder.toAnnotatedString()
    }
}
