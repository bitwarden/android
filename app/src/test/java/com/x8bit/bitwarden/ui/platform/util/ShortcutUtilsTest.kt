package com.x8bit.bitwarden.ui.platform.util

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShortcutUtilsTest {
    @Test
    fun `isMyVaultShortcut should return true when dataString is my vault deeplink`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns "bitwarden://my_vault"
        }
        assertTrue(mockIntent.isMyVaultShortcut)
    }

    @Test
    fun `isMyVaultShortcut should return false when dataString is not my vault deeplink`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns "bitwarden://some_other_vault"
        }
        assertFalse(mockIntent.isMyVaultShortcut)
    }

    @Test
    fun `isMyVaultShortcut should return false when dataString is null`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns null
        }
        assertFalse(mockIntent.isMyVaultShortcut)
    }

    @Test
    fun `isPasswordGeneratorShortcut should return true when dataString is my vault deeplink`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns "bitwarden://password_generator"
        }
        assertTrue(mockIntent.isPasswordGeneratorShortcut)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordGeneratorShortcut should return false when dataString is not my vault deeplink`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns "bitwarden://some_other_generator"
        }
        assertFalse(mockIntent.isPasswordGeneratorShortcut)
    }

    @Test
    fun `isPasswordGeneratorShortcut should return false when dataString is null`() {
        val mockIntent = mockk<Intent> {
            every { dataString } returns null
        }
        assertFalse(mockIntent.isPasswordGeneratorShortcut)
    }
}
