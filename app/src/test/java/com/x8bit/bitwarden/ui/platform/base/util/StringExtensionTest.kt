package com.x8bit.bitwarden.ui.platform.base.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StringExtensionTest {

    @Test
    fun `emails without an @ character should be invalid`() {
        val invalidEmails = listOf(
            "",
            " ",
            "test.com",
        )
        invalidEmails.forEach {
            assertFalse(it.isValidEmail())
        }
    }

    @Test
    fun `emails with an @ character should be valid`() {
        val validEmails = listOf(
            "@",
            "test@test.com",
            " test@test ",
        )
        validEmails.forEach {
            assertTrue(it.isValidEmail())
        }
    }

    @Test
    fun `isValidUri should return true for an absolute URL`() {
        assertTrue("https://abc.com".isValidUri())
    }

    @Test
    fun `isValidUri should return true for an absolute non-URL path`() {
        assertTrue("file:///abc/com".isValidUri())
    }

    @Test
    fun `isValidUri should return true for a relative URI`() {
        assertTrue("abc.com".isValidUri())
    }

    @Test
    fun `isValidUri should return false for a blank or empty String`() {
        listOf(
            "",
            "  ",
        )
            .forEach { badUri ->
                assertFalse(badUri.isValidUri())
            }
    }

    @Test
    fun `isValidUri should return false when there are invalid characters present`() {
        listOf(
            "abc com",
            "abc<>com",
            "abc[]com",
        )
            .forEach { badUri ->
                assertFalse(badUri.isValidUri())
            }
    }
}
