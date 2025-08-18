package com.bitwarden.network.retrofit

import com.bitwarden.network.interceptor.AuthTokenManager
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.ssl.CertificateProvider
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
    private val authTokenManager = mockk<AuthTokenManager> {
        mockAuthenticate { isRefreshAuthenticatorCalled = true }
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
    private val json = Json
    private val server = MockWebServer()
    private val certificateProvider = mockk<CertificateProvider> {
        every { chooseClientAlias(any(), any(), any()) } returns ""
        every { getCertificateChain(any()) } returns emptyArray()
        every { getPrivateKey(any()) } returns null
    }

    private val retrofits = RetrofitsImpl(
        authTokenManager = authTokenManager,
        baseUrlInterceptors = baseUrlInterceptors,
        headersInterceptor = headersInterceptors,
        certificateProvider = certificateProvider,
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
        unmockkConstructor(OkHttpClient.Builder::class)
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

    @Test
    fun `createStaticRetrofit should set sslSocketFactory when certificateProvider is not null`() =
        runTest {
            mockkConstructor(OkHttpClient.Builder::class)
            mockBuilder<OkHttpClient.Builder> {
                it.addInterceptor(baseUrlInterceptors.apiInterceptor)
            }
            every {
                anyConstructed<OkHttpClient.Builder>().sslSocketFactory(any(), any())
            } returns mockk(relaxed = true)
            val retrofits = RetrofitsImpl(
                authTokenManager = authTokenManager,
                baseUrlInterceptors = baseUrlInterceptors,
                headersInterceptor = headersInterceptors,
                certificateProvider = certificateProvider,
                json = json,
            )

            retrofits.createStaticRetrofit()

            verify(exactly = 1) {
                anyConstructed<OkHttpClient.Builder>().sslSocketFactory(any(), any())
            }
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

/**
 * Helper method for mocking pipeline operations within the builder pattern. This saves a lot of
 * boiler plate. In order to use this, the builder's constructor must be mockked.
 *
 * Example:
 * ```
 *     // Setup
 *     mockkConstructor(FillResponse.Builder::class)
 *     mockBuilder<FillResponse.Builder> { it.setIgnoredIds() }
 *     every { anyConstructed<FillResponse.Builder>().build() } returns mockk()
 *
 *     // Test
 *     ...
 *
 *     // Verify
 *     verify(exactly = 1) {
 *         anyConstructed<FillResponse.Builder>().setIgnoredIds()
 *         anyConstructed<FillResponse.Builder>().build()
 *     }
 *     unmockkConstructor(FillResponse.Builder::class)
 * ```
 */
inline fun <reified T : Any> mockBuilder(crossinline block: MockKMatcherScope.(T) -> T) {
    every { block(anyConstructed<T>()) } answers {
        this.self as T
    }
}
