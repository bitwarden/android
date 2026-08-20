package com.bitwarden.network.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CipherWithIdJsonRequestTest {

    @Test
    fun `toCipherWithIdJsonRequest should carry over the encryption metadata`() {
        val request = createMockCipherJsonRequest(number = 1)

        val result = request.toCipherWithIdJsonRequest(id = "mockId-1")

        assertEquals("mockId-1", result.id)
        assertEquals(request.encryptedFor, result.encryptedFor)
        assertEquals(request.encryptedByKeyId, result.encryptedByKeyId)
    }
}
