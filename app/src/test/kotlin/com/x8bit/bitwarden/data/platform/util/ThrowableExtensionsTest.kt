package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.ui.platform.resource.BitwardenString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ThrowableExtensionsTest {

    @Test
    fun `toErrorResId should return cookie_redirect_error for CookieRedirectException`() {
        assertEquals(
            BitwardenString.cookie_redirect_error,
            CookieRedirectException(hostname = "example.com").toErrorResId(),
        )
    }

    @Test
    fun `toErrorResId should return cookie_redirect_error for wrapped CookieRedirectException`() {
        assertEquals(
            BitwardenString.cookie_redirect_error,
            Exception(CookieRedirectException(hostname = "example.com")).toErrorResId(),
        )
    }

    @Test
    fun `toErrorResId should return null for other throwable`() {
        assertNull(IllegalStateException().toErrorResId())
    }

    @Test
    fun `toErrorResId should return null for null throwable`() {
        assertNull((null as Throwable?).toErrorResId())
    }
}
