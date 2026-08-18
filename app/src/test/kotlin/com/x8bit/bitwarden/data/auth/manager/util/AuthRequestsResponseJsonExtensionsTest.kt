package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthRequestsResponseJsonExtensionsTest {
    @Test
    fun `toAuthRequest should map each property and apply the given fingerprint`() {
        val fingerprint = "fingerprint"

        val result = AUTH_REQUEST_RESPONSE_JSON.toAuthRequest(fingerprint = fingerprint)

        assertEquals(
            AuthRequest(
                id = "1",
                publicKey = "publicKey",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "key",
                masterPasswordHash = "verySecureHash",
                creationDate = Instant.parse("2024-09-13T00:00:00Z"),
                responseDate = Instant.parse("2024-09-13T00:05:00Z"),
                requestApproved = true,
                originUrl = "www.bitwarden.com",
                fingerprint = fingerprint,
            ),
            result,
        )
    }

    @Test
    fun `toAuthRequest should map a null requestApproved to false`() {
        val result = AUTH_REQUEST_RESPONSE_JSON
            .copy(requestApproved = null)
            .toAuthRequest(fingerprint = "fingerprint")

        assertEquals(false, result.requestApproved)
    }
}

private val AUTH_REQUEST_RESPONSE_JSON: AuthRequestsResponseJson.AuthRequest =
    AuthRequestsResponseJson.AuthRequest(
        id = "1",
        publicKey = "publicKey",
        platform = "Android",
        ipAddress = "192.168.0.1",
        key = "key",
        masterPasswordHash = "verySecureHash",
        creationDate = Instant.parse("2024-09-13T00:00:00Z"),
        responseDate = Instant.parse("2024-09-13T00:05:00Z"),
        requestApproved = true,
        originUrl = "www.bitwarden.com",
    )
