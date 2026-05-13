package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.network.exception.CookieRedirectException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException

class ThrowableExtensionsTest {

    @Test
    fun `userFriendlyMessage should return message for CookieRedirectException`() {
        val message = "Your request was interrupted because the app needed to " +
            "re-authenticate. Please try again."
        val exception = CookieRedirectException(
            hostname = "example.com",
            message = message,
        )
        assertEquals(
            message,
            exception.userFriendlyMessage,
        )
    }

    @Test
    fun `userFriendlyMessage should return null for IOException`() {
        val exception = IOException("io error")
        assertNull(exception.userFriendlyMessage)
    }

    @Test
    fun `userFriendlyMessage should return null for RuntimeException`() {
        val exception = RuntimeException("runtime error")
        assertNull(exception.userFriendlyMessage)
    }
}
