package com.bitwarden.network.model

import com.bitwarden.network.exception.CookieRedirectException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BitwardenErrorTest {

    @Test
    fun `toBitwardenError with CookieRedirectException should return Http with status 400`() {
        val exception = CookieRedirectException(hostname = "example.com")

        val result = exception.toBitwardenError()

        assertTrue(result is BitwardenError.Http)
        val httpError = result as BitwardenError.Http
        assertEquals(400, httpError.code)
    }

    @Test
    fun `toBitwardenError with CookieRedirectException should include message in body`() {
        val exception = CookieRedirectException(hostname = "example.com")

        val result = exception.toBitwardenError()

        val httpError = result as BitwardenError.Http
        val body = httpError.responseBodyString
        assertTrue(body?.contains(exception.message.orEmpty()) == true)
    }

    @Test
    fun `toBitwardenError with IOException should return Network`() {
        val exception = IOException("network failure")

        val result = exception.toBitwardenError()

        assertTrue(result is BitwardenError.Network)
        assertEquals(exception, result.throwable)
    }

    @Test
    fun `toBitwardenError with HttpException should return Http`() {
        val exception = HttpException(
            Response.error<Unit>(400, "error".toResponseBody()),
        )

        val result = exception.toBitwardenError()

        assertTrue(result is BitwardenError.Http)
        assertEquals(exception, result.throwable)
    }

    @Test
    fun `toBitwardenError with RuntimeException should return Other`() {
        val exception = RuntimeException("unexpected")

        val result = exception.toBitwardenError()

        assertTrue(result is BitwardenError.Other)
        assertEquals(exception, result.throwable)
    }
}
