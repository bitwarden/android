package com.x8bit.bitwarden.data.auth.manager.util

import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthRequestExtensionsTest {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2024-09-13T00:04:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `isActionable should return true when unanswered and under five minutes old`() {
        assertTrue(AUTH_REQUEST.isActionable(clock = clock))
    }

    @Test
    fun `isActionable should return false when the request has been approved`() {
        assertFalse(AUTH_REQUEST.copy(requestApproved = true).isActionable(clock = clock))
    }

    @Test
    fun `isActionable should return false when the request has a response date`() {
        assertFalse(
            AUTH_REQUEST
                .copy(responseDate = Instant.parse("2024-09-13T00:03:00Z"))
                .isActionable(clock = clock),
        )
    }

    @Test
    fun `isActionable should return false when the request is over five minutes old`() {
        assertFalse(
            AUTH_REQUEST
                .copy(creationDate = Instant.parse("2024-09-12T23:58:00Z"))
                .isActionable(clock = clock),
        )
    }
}

private val AUTH_REQUEST: AuthRequest = AuthRequest(
    id = "1",
    publicKey = "publicKey",
    platform = "Android",
    ipAddress = "192.168.0.1",
    key = "key",
    masterPasswordHash = "verySecureHash",
    creationDate = Instant.parse("2024-09-13T00:00:00Z"),
    responseDate = null,
    requestApproved = false,
    originUrl = "www.bitwarden.com",
    fingerprint = "fingerprint",
)
