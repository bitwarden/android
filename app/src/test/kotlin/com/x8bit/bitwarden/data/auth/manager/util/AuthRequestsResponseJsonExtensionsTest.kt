package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthRequestsResponseJsonExtensionsTest {
    @Test
    fun `toAuthRequest should map each property and apply the given values`() {
        val fingerprint = "fingerprint"
        val responseDate = Instant.parse("2024-09-13T00:10:00Z")

        val result = AUTH_REQUEST_RESPONSE_JSON.toAuthRequest(
            fingerprint = fingerprint,
            publicKey = "givenPublicKey",
            responseDate = responseDate,
            isRequestApproved = false,
        )

        assertEquals(
            AuthRequest(
                id = "1",
                publicKey = "givenPublicKey",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "key",
                masterPasswordHash = "verySecureHash",
                creationDate = Instant.parse("2024-09-13T00:00:00Z"),
                responseDate = responseDate,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = fingerprint,
            ),
            result,
        )
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
