package com.bitwarden.ui.platform.util

import android.content.Intent
import android.os.BadParcelableException
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntentExtensionsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Intent::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Intent::class)
    }

    @Test
    fun `validate should not modify the current intent`() {
        val intent = mockk<Intent>(relaxed = true) {
            every { getStringExtra("token") } returns "myToken"
        }

        intent.validate()

        assertEquals("myToken", intent.getStringExtra("token"))
    }

    @Test
    fun `validate should remove extras if BadParcelableException is thrown`() {
        val mockIntent = mockk<Intent>(relaxed = true)

        every { mockIntent.extras } throws BadParcelableException("Bad parcel")

        mockIntent.validate()
        verify { mockIntent.replaceExtras(null as Bundle?) }
    }

    @Test
    fun `validate should remove extras if ClassNotFoundException is thrown`() {
        val mockIntent = mockk<Intent>(relaxed = true)

        every { mockIntent.extras } throws ClassNotFoundException("Bad parcel")

        mockIntent.validate()
        verify { mockIntent.replaceExtras(null as Bundle?) }
    }

    @Test
    fun `validate should remove extras if RuntimeException is thrown`() {
        val mockIntent = mockk<Intent>(relaxed = true)

        every { mockIntent.extras } throws RuntimeException("Bad parcel")

        mockIntent.validate()
        verify { mockIntent.replaceExtras(null as Bundle?) }
    }
}
