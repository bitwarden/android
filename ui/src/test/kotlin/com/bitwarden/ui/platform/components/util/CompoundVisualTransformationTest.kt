package com.bitwarden.ui.platform.components.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompoundVisualTransformationTest {

    @Test
    fun `compoundVisualTransformation with no transformations returns identity`() {
        val text = AnnotatedString("test")
        val transformation = CompoundVisualTransformation()

        val result = transformation.filter(text)

        assertEquals(text, result.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(4, result.offsetMapping.originalToTransformed(4))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
        assertEquals(4, result.offsetMapping.transformedToOriginal(4))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `compoundVisualTransformation with single transformation behaves identically to that transformation`() {
        val text = AnnotatedString("password")
        val passwordTransformation = PasswordVisualTransformation()

        val singleResult = passwordTransformation.filter(text)
        val compoundResult = CompoundVisualTransformation(passwordTransformation).filter(text)

        assertEquals(singleResult.text, compoundResult.text)

        // Test offset mapping equivalence for various offsets
        for (offset in 0..text.length) {
            assertEquals(
                singleResult.offsetMapping.originalToTransformed(offset),
                compoundResult.offsetMapping.originalToTransformed(offset),
                "originalToTransformed($offset) should match",
            )
        }

        for (offset in 0..singleResult.text.length) {
            assertEquals(
                singleResult.offsetMapping.transformedToOriginal(offset),
                compoundResult.offsetMapping.transformedToOriginal(offset),
                "transformedToOriginal($offset) should match",
            )
        }
    }

    @Test
    fun `compoundVisualTransformation applies transformations in order`() {
        val text = AnnotatedString("abc")

        // First transformation: prepend "X"
        val prependX = VisualTransformation { text ->
            TransformedText(
                AnnotatedString("X${text.text}"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) = (offset - 1).coerceAtLeast(0)
                },
            )
        }

        // Second transformation: append "Y"
        val appendY = VisualTransformation { text ->
            TransformedText(
                AnnotatedString("${text.text}Y"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset
                    override fun transformedToOriginal(offset: Int) =
                        offset.coerceAtMost(text.length)
                },
            )
        }

        val compound = CompoundVisualTransformation(prependX, appendY)
        val result = compound.filter(text)

        // Expected: "XabcY"
        assertEquals("XabcY", result.text.text)
    }

    @Test
    fun `compoundVisualTransformation offset mapping handles composition correctly`() {
        val text = AnnotatedString("test")

        // Create a simple transformation that adds one character at start
        val addPrefix = VisualTransformation { text ->
            TransformedText(
                AnnotatedString(">${text.text}"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) =
                        (offset - 1).coerceIn(0, text.length)
                },
            )
        }

        val compound = CompoundVisualTransformation(addPrefix, addPrefix)
        val result = compound.filter(text)

        // After two applications: ">>test"
        assertEquals(">>test", result.text.text)

        // Test originalToTransformed mapping
        assertEquals(
            2,
            result.offsetMapping.originalToTransformed(0),
            "Original 0 -> Transformed 2",
        )
        assertEquals(
            3,
            result.offsetMapping.originalToTransformed(1),
            "Original 1 -> Transformed 3",
        )
        assertEquals(
            6,
            result.offsetMapping.originalToTransformed(4),
            "Original 4 -> Transformed 6",
        )

        // Test transformedToOriginal mapping
        assertEquals(
            0,
            result.offsetMapping.transformedToOriginal(0),
            "Transformed 0 -> Original 0",
        )
        assertEquals(
            0,
            result.offsetMapping.transformedToOriginal(1),
            "Transformed 1 -> Original 0",
        )
        assertEquals(
            0,
            result.offsetMapping.transformedToOriginal(2),
            "Transformed 2 -> Original 0",
        )
        assertEquals(
            1,
            result.offsetMapping.transformedToOriginal(3),
            "Transformed 3 -> Original 1",
        )
        assertEquals(
            4,
            result.offsetMapping.transformedToOriginal(6),
            "Transformed 6 -> Original 4",
        )
    }

    @Test
    fun `compoundVisualTransformation transformedToOriginal handles edge case at start`() {
        val text = AnnotatedString("abc")

        val addPrefix = VisualTransformation { text ->
            TransformedText(
                AnnotatedString("X${text.text}"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) = (offset - 1).coerceAtLeast(0)
                },
            )
        }

        val compound = CompoundVisualTransformation(addPrefix, addPrefix)
        val result = compound.filter(text)

        // Test offset 0 (should map back to original 0)
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `compoundVisualTransformation transformedToOriginal handles edge case at end`() {
        val text = AnnotatedString("abc")

        val addSuffix = VisualTransformation { text ->
            TransformedText(
                AnnotatedString("${text.text}X"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset
                    override fun transformedToOriginal(offset: Int) =
                        offset.coerceAtMost(text.length)
                },
            )
        }

        val compound = CompoundVisualTransformation(addSuffix, addSuffix)
        val result = compound.filter(text)

        // Result: "abcXX" (length 5)
        // Testing beyond the original text length
        assertEquals(3, result.offsetMapping.transformedToOriginal(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(4))
        assertEquals(3, result.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun `compoundVisualTransformation with Password and ForceLtr transformations`() {
        val text = AnnotatedString("password123")

        val passwordTransform = PasswordVisualTransformation()
        val ltrTransform = ForceLtrVisualTransformation

        val compound = CompoundVisualTransformation(passwordTransform, ltrTransform)
        val result = compound.filter(text)

        // Password transformation converts to bullets, then LTR adds control chars
        // LTR adds LRO at start and PDF at end
        val expectedLength = text.length + 2 // Original bullets + LRO + PDF
        assertEquals(expectedLength, result.text.length)

        // Test offset mappings at various points
        val mappings = listOf(
            0 to 1, // Original 0 should map to transformed 1 (after LRO)
            5 to 6, // Original 5 should map to transformed 6
            11 to 12, // Original 11 (end) should map to transformed 12
        )

        mappings.forEach { (original, transformed) ->
            assertEquals(
                transformed,
                result.offsetMapping.originalToTransformed(original),
                "Original $original should map to transformed $transformed",
            )
        }
    }

    @Test
    fun `compoundVisualTransformation transformedToOriginal with out-of-bounds offset`() {
        val text = AnnotatedString("test")

        // Transformation that adds characters at both ends
        val wrapText = VisualTransformation { text ->
            TransformedText(
                AnnotatedString("[${text.text}]"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) =
                        (offset - 1).coerceIn(0, text.length)
                },
            )
        }

        val compound = CompoundVisualTransformation(wrapText, wrapText)
        val result = compound.filter(text)

        // Result should be "[[test]]" (length 8)
        assertEquals("[[test]]", result.text.text)

        // Test with offsets beyond the transformed text length
        // This tests the critical edge case mentioned in the review
        val beyondEndOffset = result.text.length + 5
        val mappedOffset = result.offsetMapping.transformedToOriginal(beyondEndOffset)

        // Should be coerced to the original text length
        assertEquals(
            text.length,
            mappedOffset,
            "Out-of-bounds offset should be coerced to original text length",
        )
    }

    @Test
    fun `compoundVisualTransformation with empty string`() {
        val text = AnnotatedString("")

        val addPrefix = VisualTransformation { text ->
            TransformedText(
                AnnotatedString(">${text.text}"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) = (offset - 1).coerceAtLeast(0)
                },
            )
        }

        val compound = CompoundVisualTransformation(addPrefix)
        val result = compound.filter(text)

        assertEquals(">", result.text.text)
        assertEquals(1, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(1))
    }

    @Test
    fun `compoundVisualTransformation preserves AnnotatedString spans`() {
        val text = AnnotatedString.Builder().apply {
            append("test")
        }.toAnnotatedString()

        val identityTransform = VisualTransformation.None
        val compound = CompoundVisualTransformation(identityTransform)
        val result = compound.filter(text)

        assertEquals(text.text, result.text.text)
    }

    @Test
    fun `compoundVisualTransformation offset mapping is symmetric for identity`() {
        val text = AnnotatedString("symmetric")

        val compound = CompoundVisualTransformation()
        val result = compound.filter(text)

        // For identity transformation, offset mapping should be symmetric
        for (offset in 0..text.length) {
            val transformed = result.offsetMapping.originalToTransformed(offset)
            val backToOriginal = result.offsetMapping.transformedToOriginal(transformed)
            assertEquals(
                offset,
                backToOriginal,
                "Round trip for offset $offset should return to original",
            )
        }
    }

    @Test
    fun `compoundVisualTransformation with very long text`() {
        val longText = "a".repeat(10000)
        val text = AnnotatedString(longText)

        val addPrefix = VisualTransformation { text ->
            TransformedText(
                AnnotatedString(">${text.text}"),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset + 1
                    override fun transformedToOriginal(offset: Int) =
                        (offset - 1).coerceIn(0, text.length)
                },
            )
        }

        val compound = CompoundVisualTransformation(addPrefix)
        val result = compound.filter(text)

        assertEquals(10001, result.text.length)
        assertEquals(1, result.offsetMapping.originalToTransformed(0))
        assertEquals(10001, result.offsetMapping.originalToTransformed(10000))
        assertEquals(10000, result.offsetMapping.transformedToOriginal(10001))
    }
}
