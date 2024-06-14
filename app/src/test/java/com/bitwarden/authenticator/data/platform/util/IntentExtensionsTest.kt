package com.bitwarden.authenticator.data.platform.util

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntentExtensionsTest {
    @Test
    fun `isSuspicious should return true when extras are not empty`() {
        val intent = mockk<Intent> {
            every { data } returns mockk()
            every { extras } returns mockk {
                every { isEmpty } returns false
            }
        }

        assertTrue(intent.isSuspicious)
    }

    @Test
    fun `isSuspicious should return true when extras are null`() {
        val intent = mockk<Intent> {
            every { data } returns mockk()
            every { extras } returns null
        }

        assertTrue(intent.isSuspicious)
    }

    @Test
    fun `isSuspicious should return true when data is not null`() {
        val intent = mockk<Intent> {
            every { data } returns mockk()
            every { extras } returns null
        }

        assertTrue(intent.isSuspicious)
    }

    @Test
    fun `isSuspicious should return false when data and extras are null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { extras } returns null
        }

        assertFalse(intent.isSuspicious)
    }

    @Test
    fun `isSuspicious should return false when data is null and extras are empty`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { extras } returns mockk {
                every { isEmpty } returns true
            }
        }

        assertFalse(intent.isSuspicious)
    }
}
