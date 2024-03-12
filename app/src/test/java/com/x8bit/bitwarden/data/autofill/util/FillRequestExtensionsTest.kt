package com.x8bit.bitwarden.data.autofill.util

import android.service.autofill.FillRequest
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FillRequestExtensionsTest {
    private val expectedInlinePresentationSpecs: List<InlinePresentationSpec> = mockk()
    private val expectedInlineSuggestionsRequest: InlineSuggestionsRequest = mockk {
        every { this@mockk.inlinePresentationSpecs } returns expectedInlinePresentationSpecs
        every { this@mockk.maxSuggestionCount } returns MAX_INLINE_SUGGESTIONS_COUNT
    }
    private val fillRequest: FillRequest = mockk {
        every { this@mockk.inlineSuggestionsRequest } returns expectedInlineSuggestionsRequest
    }

    @Test
    fun `getInlinePresentationSpecs should return empty list when request is null`() {
        // Setup
        val autofillAppInfo: AutofillAppInfo = mockk {
            every { sdkInt } returns 30
        }
        val expected: List<InlinePresentationSpec> = emptyList()

        // Test
        val actual = null.getInlinePresentationSpecs(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = false,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getInlinePresentationSpecs should return empty list when disabled`() {
        // Setup
        val autofillAppInfo: AutofillAppInfo = mockk {
            every { sdkInt } returns 30
        }
        val expected: List<InlinePresentationSpec> = emptyList()

        // Test
        val actual = fillRequest.getInlinePresentationSpecs(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = false,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getInlinePresentationSpecs should return null when enabled pre-R`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = mockk(),
            packageName = "com.x8bit.bitwarden",
            sdkInt = 17,
        )

        // Test
        val actual = fillRequest.getInlinePresentationSpecs(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = true,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `getInlinePresentationSpecs should return populated list when enabled and post-R`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = mockk(),
            packageName = "com.x8bit.bitwarden",
            sdkInt = 34,
        )
        val expected = expectedInlinePresentationSpecs

        // Test
        val actual = fillRequest.getInlinePresentationSpecs(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getMaxInlineSuggestionsCount should return 0 when request is null`() {
        // Setup
        val autofillAppInfo: AutofillAppInfo = mockk()
        val expected = 0

        // Test
        val actual = null.getMaxInlineSuggestionsCount(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = false,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getMaxInlineSuggestionsCount should return 0 when disabled`() {
        // Setup
        val autofillAppInfo: AutofillAppInfo = mockk()
        val expected = 0

        // Test
        val actual = fillRequest.getMaxInlineSuggestionsCount(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = false,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getMaxInlineSuggestionsCount should return 0 when enabled and pre-R`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = mockk(),
            packageName = "com.x8bit.bitwarden",
            sdkInt = 17,
        )
        val expected = 0

        // Test
        val actual = fillRequest.getMaxInlineSuggestionsCount(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getMaxInlineSuggestionsCount should return the max count when enabled and post-R`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = mockk(),
            packageName = "com.x8bit.bitwarden",
            sdkInt = 34,
        )
        val expected = MAX_INLINE_SUGGESTIONS_COUNT

        // Test
        val actual = fillRequest.getMaxInlineSuggestionsCount(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }
}

private const val MAX_INLINE_SUGGESTIONS_COUNT: Int = 42
