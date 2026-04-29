package com.bitwarden.network.util

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.model.BitwardenError
import com.bitwarden.network.model.CreateCipherResponseJson
import com.bitwarden.network.model.toBitwardenError
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ExceptionExtensionsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `parseErrorBodyOrNull with CookieRedirectException should extract message`() {
        val expectedMessage = "Your request was interrupted because the app " +
            "needed to re-authenticate. Please try again."
        val error = CookieRedirectException(hostname = "example.com")
            .toBitwardenError()

        val result = error.parseErrorBodyOrNull<CreateCipherResponseJson.Invalid>(
            codes = listOf(NetworkErrorCode.BAD_REQUEST),
            json = json,
        )

        assertEquals(expectedMessage, result?.message)
    }

    @Test
    fun `parseErrorBodyOrNull with Http and matching code should parse body`() {
        val responseBody = """
            {
                "message": "Bad request",
                "validationErrors": {
                    "Name": ["Name is required"]
                }
            }
        """.trimIndent()
        val error = BitwardenError.Http(
            throwable = HttpException(
                Response.error<Unit>(400, responseBody.toResponseBody()),
            ),
        )

        val result = error.parseErrorBodyOrNull<CreateCipherResponseJson.Invalid>(
            codes = listOf(NetworkErrorCode.BAD_REQUEST),
            json = json,
        )

        assertEquals("Bad request", result?.message)
        assertEquals(
            mapOf("Name" to listOf("Name is required")),
            result?.validationErrors,
        )
    }

    @Test
    fun `parseErrorBodyOrNull with Http and non-matching code should return null`() {
        val responseBody = """
            {
                "message": "Bad request",
                "validationErrors": null
            }
        """.trimIndent()
        val error = BitwardenError.Http(
            throwable = HttpException(
                Response.error<Unit>(400, responseBody.toResponseBody()),
            ),
        )

        val result = error.parseErrorBodyOrNull<CreateCipherResponseJson.Invalid>(
            codes = listOf(NetworkErrorCode.UNAUTHORIZED),
            json = json,
        )

        assertNull(result)
    }

    @Test
    fun `parseErrorBodyOrNull with Network should return null`() {
        val error = BitwardenError.Network(throwable = IOException("timeout"))

        val result = error.parseErrorBodyOrNull<CreateCipherResponseJson.Invalid>(
            codes = listOf(NetworkErrorCode.BAD_REQUEST),
            json = json,
        )

        assertNull(result)
    }
}
