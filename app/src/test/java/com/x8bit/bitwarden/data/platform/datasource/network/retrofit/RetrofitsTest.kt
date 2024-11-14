package com.x8bit.bitwarden.data.platform.datasource.network.retrofit

import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.HeadersInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Authenticator
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
    private val authTokenInterceptor = mockk<AuthTokenInterceptor> {
        mockIntercept { isAuthInterceptorCalled = true }
    }
    private val baseUrlInterceptors = mockk<BaseUrlInterceptors> {
        every { apiInterceptor } returns mockk {
            mockIntercept { isApiInterceptorCalled = true }
        }
        every { identityInterceptor } returns mockk {
            mockIntercept { isIdentityInterceptorCalled = true }
        }
        every { eventsInterceptor } returns mockk {
            mockIntercept { isEventsInterceptorCalled = true }
        }
    }
    private val headersInterceptors = mockk<HeadersInterceptor> {
        mockIntercept { isHeadersInterceptorCalled = true }
    }
    private val refreshAuthenticator = mockk<RefreshAuthenticator> {
        mockAuthenticate { isRefreshAuthenticatorCalled = true }
    }
    private val json = Json
    private val server = MockWebServer()

    private val retrofits = RetrofitsImpl(
        authTokenInterceptor = authTokenInterceptor,
        baseUrlInterceptors = baseUrlInterceptors,
        headersInterceptor = headersInterceptors,
        refreshAuthenticator = refreshAuthenticator,
        json = json,
    )

    private var isAuthInterceptorCalled = false
    private var isApiInterceptorCalled = false
    private var isHeadersInterceptorCalled = false
    private var isIdentityInterceptorCalled = false
    private var isEventsInterceptorCalled = false
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
    fun `authenticatedApiRetrofit should not invoke the RefreshAuthenticator on success`() =
        runBlocking {
            val testApi = retrofits
                .authenticatedApiRetrofit
                .createMockRetrofit()
                .create<TestApi>()

            server.enqueue(MockResponse().setBody("""{}"""))

            testApi.test()

            assertFalse(isRefreshAuthenticatorCalled)
        }

    @Test
    fun `authenticatedApiRetrofit should invoke the RefreshAuthenticator on 401`() = runBlocking {
        val testApi = retrofits
            .authenticatedApiRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setResponseCode(401).setBody("""{}"""))

        testApi.test()

        assertTrue(isRefreshAuthenticatorCalled)
    }

    @Test
    fun `authenticatedEventsRetrofit should not invoke the RefreshAuthenticator on success`() =
        runBlocking {
            val testApi = retrofits
                .authenticatedEventsRetrofit
                .createMockRetrofit()
                .create<TestApi>()

            server.enqueue(MockResponse().setBody("""{}"""))

            testApi.test()

            assertFalse(isRefreshAuthenticatorCalled)
        }

    @Test
    fun `authenticatedEventsRetrofit should invoke the RefreshAuthenticator on 401`() =
        runBlocking {
            val testApi = retrofits
                .authenticatedEventsRetrofit
                .createMockRetrofit()
                .create<TestApi>()

            server.enqueue(MockResponse().setResponseCode(401).setBody("""{}"""))

            testApi.test()

            assertTrue(isRefreshAuthenticatorCalled)
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
    fun `authenticatedApiRetrofit should invoke the correct interceptors`() = runBlocking {
        val testApi = retrofits
            .authenticatedApiRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setBody("""{}"""))

        testApi.test()

        assertTrue(isAuthInterceptorCalled)
        assertTrue(isApiInterceptorCalled)
        assertTrue(isHeadersInterceptorCalled)
        assertFalse(isIdentityInterceptorCalled)
        assertFalse(isEventsInterceptorCalled)
    }

    @Test
    fun `authenticatedEventsRetrofit should invoke the correct interceptors`() = runBlocking {
        val testApi = retrofits
            .authenticatedEventsRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setBody("""{}"""))

        testApi.test()

        assertTrue(isAuthInterceptorCalled)
        assertFalse(isApiInterceptorCalled)
        assertTrue(isHeadersInterceptorCalled)
        assertFalse(isIdentityInterceptorCalled)
        assertTrue(isEventsInterceptorCalled)
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
        assertTrue(isHeadersInterceptorCalled)
        assertFalse(isIdentityInterceptorCalled)
        assertFalse(isEventsInterceptorCalled)
    }

    @Test
    fun `unauthenticatedIdentityRetrofit should invoke the correct interceptors`() = runBlocking {
        val testApi = retrofits
            .unauthenticatedIdentityRetrofit
            .createMockRetrofit()
            .create<TestApi>()

        server.enqueue(MockResponse().setBody("""{}"""))

        testApi.test()

        assertFalse(isAuthInterceptorCalled)
        assertFalse(isApiInterceptorCalled)
        assertTrue(isHeadersInterceptorCalled)
        assertTrue(isIdentityInterceptorCalled)
        assertFalse(isEventsInterceptorCalled)
    }

    @Test
    fun `createStaticRetrofit when authenticated should invoke the correct interceptors`() =
        runBlocking {
            val testApi = retrofits
                .createStaticRetrofit(isAuthenticated = true)
                .createMockRetrofit()
                .create<TestApi>()

            server.enqueue(MockResponse().setBody("""{}"""))

            testApi.test()

            assertTrue(isAuthInterceptorCalled)
            assertFalse(isApiInterceptorCalled)
            assertTrue(isHeadersInterceptorCalled)
            assertFalse(isIdentityInterceptorCalled)
            assertFalse(isEventsInterceptorCalled)
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
            assertTrue(isHeadersInterceptorCalled)
            assertFalse(isIdentityInterceptorCalled)
            assertFalse(isEventsInterceptorCalled)
        }

    private fun Retrofit.createMockRetrofit(): Retrofit =
        this
            .newBuilder()
            .baseUrl(server.url("/").toString())
            .build()
}

interface TestApi {
    @GET("/test")
    suspend fun test(): NetworkResult<JsonObject>
}

/**
 * Mocks the given [Authenticator] such that the [Authenticator.authenticate] is a no-op and
 * returns `null` but triggers the [isCalledCallback].
 */
private fun Authenticator.mockAuthenticate(isCalledCallback: () -> Unit) {
    every { authenticate(any(), any()) } answers {
        isCalledCallback()
        null
    }
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
