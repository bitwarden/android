package com.bitwarden.network.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CipherWithIdJsonRequestTest {

    @Test
    fun `toCipherWithIdJsonRequest should convert a CipherJsonRequest and an ID`() {
        val result = createMockCipherJsonRequest(number = 1)
            .toCipherWithIdJsonRequest(id = "mockId-1")

        assertEquals(
            createMockCipherWithIdJsonRequest(
                number = 1,
                id = "mockId-1",
            ),
            result,
        )
    }
}
