package com.x8bit.bitwarden.ui.platform.image

import com.bitwarden.network.interceptor.FakeInterceptorChain
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImageNotFoundCacheInterceptorTest {

    private val interceptor = ImageNotFoundCacheInterceptor()

    @Test
    fun `intercept should mark successful responses as no-store`() {
        val response = interceptor.intercept(chainRespondingWith(code = 200))

        assertEquals("no-store", response.header("Cache-Control"))
    }

    @Test
    fun `intercept should keep the server cache control on not found responses`() {
        val response = interceptor.intercept(chainRespondingWith(code = 404))

        assertEquals("public, max-age=604800", response.header("Cache-Control"))
    }

    private fun chainRespondingWith(code: Int) = FakeInterceptorChain(
        request = Request.Builder().url("https://icons.bitwarden.net/example.com/icon.png").build(),
        responseProvider = { request ->
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .header("Cache-Control", "public, max-age=604800")
                .build()
        },
    )
}
