package com.bitwarden.authenticator.data.platform.datasource.network.retrofit

import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.HeadersInterceptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

class RetrofitsTest {
    private val baseUrlInterceptors = mockk<BaseUrlInterceptors> {
        every { apiInterceptor } returns mockk {
            mockIntercept { isApiInterceptorCalled = true }
        }
    }
    private val headersInterceptors = mockk<HeadersInterceptor> {
        mockIntercept { isheadersInterceptorCalled = true }
    }
    private val json = Json
    private val server = MockWebServer()

    private val retrofits = RetrofitsImpl(
        baseUrlInterceptors = baseUrlInterceptors,
        headersInterceptor = headersInterceptors,
        json = json,
    )

    private var isAuthInterceptorCalled = false
    private var isApiInterceptorCalled = false
    private var isheadersInterceptorCalled = false
    private var isRefreshAuthenticatorCalled = false

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `unauthenticatedApiRetrofit should not invoke the RefreshAuthenticator`() = runBlocking {
        val testApi = retrofits
            .unauthenticatedApiRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setResponseCode(401).setBody("""{}"""))

        testApi.test()

        assertFalse(isRefreshAuthenticatorCalled)
    }

    @Test
    fun `unauthenticatedApiRetrofit should invoke the correct interceptors`() = runBlocking {
        val testApi = retrofits
            .unauthenticatedApiRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setBody("""{}"""))

        testApi.test()

        assertFalse(isAuthInterceptorCalled)
        assertTrue(isApiInterceptorCalled)
        assertTrue(isheadersInterceptorCalled)
    }

    @Test
    fun `createStaticRetrofit when unauthenticated should invoke the correct interceptors`() =
        runBlocking {
            val testApi = retrofits
                .createStaticRetrofit(isAuthenticated = false)
                .createMockRetrofit()
                .create<TestApi>()

            server.enqueue(MockResponse().setBody("""{}"""))

            testApi.test()

            assertFalse(isAuthInterceptorCalled)
            assertFalse(isApiInterceptorCalled)
            assertTrue(isheadersInterceptorCalled)
        }

    private fun Retrofit.createMockRetrofit(): Retrofit =
        this
            .newBuilder()
            .baseUrl(server.url("/").toString())
            .build()
}

interface TestApi {
    @GET("/test")
    suspend fun test(): Result<JsonObject>
}

/**
 * Mocks the given [Interceptor] such that the [Interceptor.intercept] is a no-op but triggers the
 * [isCalledCallback].
 */
private fun Interceptor.mockIntercept(isCalledCallback: () -> Unit) {
    val chainSlot = slot<Interceptor.Chain>()
    every { intercept(capture(chainSlot)) } answers {
        isCalledCallback()
        val chain = chainSlot.captured
        chain.proceed(chain.request())
    }
}
