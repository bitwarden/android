package com.x8bit.bitwarden.data.billing.util

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PremiumCheckoutUtilsTest {

    @Test
    fun `getPremiumCheckoutCallbackResult should return Success for result success`() {
        val uri = mockk<Uri> {
            every { getQueryParameter("result") } returns "success"
        }
        assertEquals(
            PremiumCheckoutCallbackResult.Success,
            uri.getPremiumCheckoutCallbackResult(),
        )
    }

    @Test
    fun `getPremiumCheckoutCallbackResult should return Canceled for result canceled`() {
        val uri = mockk<Uri> {
            every { getQueryParameter("result") } returns "canceled"
        }
        assertEquals(
            PremiumCheckoutCallbackResult.Canceled,
            uri.getPremiumCheckoutCallbackResult(),
        )
    }

    @Test
    fun `getPremiumCheckoutCallbackResult should return Success for case insensitive SUCCESS`() {
        val uri = mockk<Uri> {
            every { getQueryParameter("result") } returns "SUCCESS"
        }
        assertEquals(
            PremiumCheckoutCallbackResult.Success,
            uri.getPremiumCheckoutCallbackResult(),
        )
    }

    @Test
    fun `getPremiumCheckoutCallbackResult should return Canceled when result param is missing`() {
        val uri = mockk<Uri> {
            every { getQueryParameter("result") } returns null
        }
        assertEquals(
            PremiumCheckoutCallbackResult.Canceled,
            uri.getPremiumCheckoutCallbackResult(),
        )
    }

    @Test
    fun `getPremiumCheckoutCallbackResult should return Canceled for null Uri`() {
        val uri: Uri? = null
        assertEquals(
            PremiumCheckoutCallbackResult.Canceled,
            uri.getPremiumCheckoutCallbackResult(),
        )
    }
}
