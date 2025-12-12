package com.bitwarden.ui.platform.components.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForceLtrVisualTransformationTest {

    @Test
    fun `forceLtrVisualTransformation adds LRO and PDF characters`() {
        val text = AnnotatedString("password")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}password$PDF", result.text.text)
        assertEquals(10, result.text.length) // Original 8 + LRO + PDF
    }

    @Test
    fun `forceLtrVisualTransformation with empty string`() {
        val text = AnnotatedString("")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("$LRO$PDF", result.text.text)
        assertEquals(2, result.text.length)
    }

    @Test
    fun `forceLtrVisualTransformation originalToTransformed adds 1 to offset`() {
        val text = AnnotatedString("test")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // LRO is inserted at position 0, so all original offsets shift by 1
        assertEquals(1, result.offsetMapping.originalToTransformed(0))
        assertEquals(2, result.offsetMapping.originalToTransformed(1))
        assertEquals(3, result.offsetMapping.originalToTransformed(2))
        assertEquals(4, result.offsetMapping.originalToTransformed(3))
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
    }

    @Test
    fun `forceLtrVisualTransformation transformedToOriginal subtracts 1 and coerces`() {
        val text = AnnotatedString("test")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // Transformed text is "[LRO]test[PDF]" (length 6)
        assertEquals(0, result.offsetMapping.transformedToOriginal(0)) // LRO position
        assertEquals(0, result.offsetMapping.transformedToOriginal(1)) // First char
        assertEquals(1, result.offsetMapping.transformedToOriginal(2))
        assertEquals(2, result.offsetMapping.transformedToOriginal(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5)) // PDF position
    }

    @Test
    fun `forceLtrVisualTransformation transformedToOriginal coerces negative offsets to 0`() {
        val text = AnnotatedString("test")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // When transformedToOriginal receives 0, it computes (0 - 1) = -1
        // This should be coerced to 0
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `forceLtrVisualTransformation transformedToOriginal coerces beyond text length`() {
        val text = AnnotatedString("test")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // Transformed text length is 6, but we test with larger offset
        val beyondEnd = 10
        val mappedOffset = result.offsetMapping.transformedToOriginal(beyondEnd)

        // Should be coerced to original text length (4)
        assertEquals(4, mappedOffset)
    }

    @Test
    fun `forceLtrVisualTransformation preserves AnnotatedString spans`() {
        val text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("bold")
            }
            append("normal")
        }

        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // The transformed text should still have spans (though offset by 1)
        assertTrue(result.text.text.startsWith(LRO))
        assertTrue(result.text.text.endsWith(PDF))
        assertTrue(result.text.text.contains("boldnormal"))
    }

    @Test
    fun `forceLtrVisualTransformation with RTL characters`() {
        // Arabic text "مرحبا" (Hello)
        val text = AnnotatedString("مرحبا")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        // Should wrap RTL text with LTR control characters
        assertEquals("${LRO}مرحبا$PDF", result.text.text)
        assertTrue(result.text.text.startsWith(LRO))
        assertTrue(result.text.text.endsWith(PDF))
    }

    @Test
    fun `forceLtrVisualTransformation with mixed LTR and RTL characters`() {
        // Mixed English and Arabic
        val text = AnnotatedString("Hello مرحبا World")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}Hello مرحبا World$PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation with special characters`() {
        val text = AnnotatedString("p@ssw0rd!#$%")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}p@ssw0rd!#$%$PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation with numbers only`() {
        val text = AnnotatedString("123456")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}123456$PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation offset mapping is consistent at boundaries`() {
        val text = AnnotatedString("abc")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // Test at start boundary
        val startOriginal = 0
        val startTransformed = result.offsetMapping.originalToTransformed(startOriginal)
        val backToStart = result.offsetMapping.transformedToOriginal(startTransformed)
        assertEquals(startOriginal, backToStart)

        // Test at end boundary
        val endOriginal = text.length
        val endTransformed = result.offsetMapping.originalToTransformed(endOriginal)
        val backToEnd = result.offsetMapping.transformedToOriginal(endTransformed)
        assertEquals(endOriginal, backToEnd)
    }

    @Test
    fun `forceLtrVisualTransformation with very long text`() {
        val longText = "a".repeat(10000)
        val text = AnnotatedString(longText)
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        // Should handle long strings without issues
        assertEquals(10002, result.text.length) // 10000 + LRO + PDF
        assertTrue(result.text.text.startsWith(LRO))
        assertTrue(result.text.text.endsWith(PDF))

        // Test offset mapping at various points in long text
        assertEquals(5001, result.offsetMapping.originalToTransformed(5000))
        assertEquals(5000, result.offsetMapping.transformedToOriginal(5001))
    }

    @Test
    fun `forceLtrVisualTransformation with whitespace`() {
        val text = AnnotatedString("   ")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("$LRO   $PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation with newlines`() {
        val text = AnnotatedString("line1\nline2\nline3")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}line1\nline2\nline3$PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation with existing unicode control characters`() {
        // Text already containing direction control characters
        val text = AnnotatedString("${LRO}test$PDF")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        // Should add additional control characters
        assertEquals("$LRO${LRO}test$PDF$PDF", result.text.text)
    }

    @Test
    fun `forceLtrVisualTransformation round trip maintains offset relationships`() {
        val text = AnnotatedString("password123")
        val transformation = ForceLtrVisualTransformation
        val result = transformation.filter(text)

        // For each original offset, going to transformed and back should preserve the offset
        for (originalOffset in 0..text.length) {
            val transformed = result.offsetMapping.originalToTransformed(originalOffset)
            val backToOriginal = result.offsetMapping.transformedToOriginal(transformed)
            assertEquals(
                originalOffset,
                backToOriginal,
                "Round trip failed for offset $originalOffset",
            )
        }
    }

    @Test
    fun `forceLtrVisualTransformation with single character`() {
        val text = AnnotatedString("a")
        val transformation = ForceLtrVisualTransformation

        val result = transformation.filter(text)

        assertEquals("${LRO}a$PDF", result.text.text)
        assertEquals(3, result.text.length)

        // Test offset mappings
        assertEquals(1, result.offsetMapping.originalToTransformed(0))
        assertEquals(2, result.offsetMapping.originalToTransformed(1))
        assertEquals(0, result.offsetMapping.transformedToOriginal(1))
        assertEquals(1, result.offsetMapping.transformedToOriginal(2))
    }
}
