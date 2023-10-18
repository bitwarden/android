package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.JwtTokenDataJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JwtTokenUtilsTest {
    @Test
    fun `parseJwtTokenDataOrNull for a valid token input should return a JwtTokenData`() {
        val testJwtToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE2OTc0OTIxMTQsImV4cCI6MTY5NzQ5NTcxN" +
                "CwiaXNzIjoiaHR0cHM6Ly9pZGVudGl0eS5iaXR3YXJkZW4uY29tIiwiY2xpZW50X2lkIjoibW9iaWxl" +
                "Iiwic3ViIjoiMmExMzViMjMtZTFmYi00MmM5LWJlYzMtNTczODU3YmM4MTgxIiwiYXV0aF90aW1lIjo" +
                "xNjk3NDkyMTE0LCJpZHAiOiJiaXR3YXJkZW4iLCJwcmVtaXVtIjpmYWxzZSwiZW1haWwiOiJ0ZXN0QG" +
                "JpdHdhcmRlbi5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwic3N0YW1wIjoiSkRIUzRSTUxFNEtGV" +
                "EI0TFRIMjVTNkVLRktGTlhOQ0IiLCJuYW1lIjoiQml0d2FyZGVuIFRlc3RlciIsImRldmljZSI6IjNk" +
                "ODYxNTU3LWI0Y2MtNDQxZi05YjE4LWM0NTAyYTcxN2UwYiIsImp0aSI6IjA1M0U5NUEzNjFBNEI4QUY" +
                "yREEyRDIyNzNDREUxRDVFIiwiaWF0IjoxNjk3NDkyMTE0LCJzY29wZSI6WyJhcGkiLCJvZmZsaW5lX2" +
                "FjY2VzcyJdLCJhbXIiOlsiQXBwbGljYXRpb24iXX0.RP2-wABK63Osu-tJY6KJjqVRSJ3-JR_OOdc3N" +
                "nm4C5U"

        assertEquals(
            JwtTokenDataJson(
                userId = "2a135b23-e1fb-42c9-bec3-573857bc8181",
                email = "test@bitwarden.com",
                isEmailVerified = true,
                name = "Bitwarden Tester",
                expirationAsEpochTime = 1697495714,
                hasPremium = false,
                authenticationMethodsReference = listOf("Application"),
            ),
            parseJwtTokenDataOrNull(jwtToken = testJwtToken),
        )
    }

    @Test
    fun `parseJwtTokenDataOrNull for an invalid token input should return null`() {
        assertNull(
            parseJwtTokenDataOrNull(jwtToken = "invalid JWT token"),
        )
    }
}
