package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.util.parseToJsonElementOrNull
import com.x8bit.bitwarden.data.util.assertJsonEquals
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResponseJsonKeyNameInterceptorTest {

    private val interceptor = ResponseJsonKeyTransformerInterceptor(
        json = PlatformNetworkModule.providesJson(),
    )
    private val request = Request.Builder()
        .url("http://localhost")
        .build()

    @AfterEach
    fun tearDown() {
        unmockkStatic(Json::parseToJsonElementOrNull)
    }

    @Test
    fun `intercept should return original response when response body is null`() {
        val originalResponse = Response.Builder()
            .request(request)
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val response = interceptor.intercept(
            chain = FakeInterceptorChain(
                request = request,
                responseProvider = { originalResponse },
            ),
        )
        assertEquals(
            originalResponse,
            response,
        )
    }

    @Test
    fun `intercept should return original response when parseToJsonElementOrNull is null`() {
        mockkStatic(Json::parseToJsonElementOrNull)
        every { Json.parseToJsonElementOrNull(any()) } returns null

        val originalResponse = Response.Builder()
            .request(request)
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .body("".toResponseBody())
            .build()
        val response = interceptor.intercept(
            chain = FakeInterceptorChain(
                request = request,
                responseProvider = { originalResponse },
            ),
        )
        assertEquals(
            originalResponse,
            response,
        )
    }

    @Test
    fun `intercept should return transformed response`() {
        val response = interceptor.intercept(
            chain = FakeInterceptorChain(
                request = request,
                responseProvider = {
                    Response.Builder()
                        .request(it)
                        .code(200)
                        .message("OK")
                        .protocol(Protocol.HTTP_1_1)
                        .body(
                            """[{"PascalArray":[{"PascalCase":0}]}]""".toResponseBody(),
                        )
                        .build()
                },
            ),
        )
        assertJsonEquals(
            """[{"pascalArray":[{"pascalCase":0}]}]""",
            response.body!!.string(),
        )
    }
}
