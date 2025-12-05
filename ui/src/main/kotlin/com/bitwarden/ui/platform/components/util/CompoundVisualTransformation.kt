package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A [VisualTransformation] that chains multiple other [VisualTransformation]s.
 *
 * This is useful for applying multiple transformations to a text field. The
 * transformations are applied in the order they are provided, with each transformation's
 * output becoming the input for the next transformation.
 *
 * ## Example Usage
 *
 * Combining password masking with LTR direction enforcement:
 * ```kotlin
 * val visualTransformation = compoundVisualTransformation(
 *     PasswordVisualTransformation(),
 *     forceLtrVisualTransformation()
 * )
 * TextField(
 *     value = password,
 *     visualTransformation = visualTransformation
 * )
 * ```
 *
 * Combining color transformation with LTR for readonly fields:
 * ```kotlin
 * val visualTransformation = compoundVisualTransformation(
 *     nonLetterColorVisualTransformation(),
 *     forceLtrVisualTransformation()
 * )
 * ```
 *
 * ## Important Notes
 *
 * - Offset mapping is correctly composed through all transformations
 * - The order of transformations matters (first applied is first in the list)
 * - Use [compoundVisualTransformation] function for proper `remember` optimization
 *
 * @param transformations The visual transformations to apply in order
 * @see compoundVisualTransformation
 * @see forceLtrVisualTransformation
 */
internal class CompoundVisualTransformation(
    vararg val transformations: VisualTransformation,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return transformations.fold(
            TransformedText(
                text,
                OffsetMapping.Identity,
            ),
        ) { acc, transformation ->
            val result = transformation.filter(acc.text)

            val composedMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val originalTransformed = acc.offsetMapping.originalToTransformed(offset)
                    return result.offsetMapping.originalToTransformed(originalTransformed)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val resultOriginal = result.offsetMapping.transformedToOriginal(offset)
                    return acc.offsetMapping.transformedToOriginal(resultOriginal)
                }
            }
            TransformedText(result.text, composedMapping)
        }
    }
}

/**
 * Remembers a [CompoundVisualTransformation] for the given [transformations].
 *
 * This is an optimization to avoid creating a new [CompoundVisualTransformation] on every
 * recomposition.
 */
@Composable
fun compoundVisualTransformation(
    vararg transformations: VisualTransformation,
): VisualTransformation =
    remember(*transformations) {
        CompoundVisualTransformation(*transformations)
    }
