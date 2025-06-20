package com.x8bit.bitwarden.ui.vault.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.ui.vault.model.TotpData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TotpIntentUtilsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::getTotpDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::getTotpDataOrNull)
    }

    @Test
    fun `getTotpDataOrNull with null data should return null`() {
        val intent = mockk<Intent> {
            every { data } returns null
        }

        assertNull(intent.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with null uri getTotpDataOrNull should return null`() {
        val uri = mockk<Uri> {
            every { getTotpDataOrNull() } returns null
        }
        val intent = mockk<Intent> {
            every { data } returns uri
        }

        assertNull(intent.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with valid uri getTotpDataOrNull should return totpData`() {
        val totpData = mockk<TotpData>()
        val uri = mockk<Uri> {
            every { getTotpDataOrNull() } returns totpData
        }
        val intent = mockk<Intent> {
            every { data } returns uri
        }
        println(intent)

        assertEquals(totpData, intent.getTotpDataOrNull())
    }
}
