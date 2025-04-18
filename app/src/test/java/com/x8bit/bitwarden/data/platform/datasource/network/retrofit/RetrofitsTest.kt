package com.x8bit.bitwarden.data.platform.datasource.network.retrofit

import com.bitwarden.network.interceptor.AuthTokenInterceptor
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.model.NetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.ssl.SslManager
import com.x8bit.bitwarden.data.util.mockBuilder
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
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    private val mockSslManager = mockk<SslManager> {
        every { sslContext } returns mockk(relaxed = true)
        every { trustManagers } returns arrayOf(mockk<X509TrustManager>(relaxed = true))
    }

    private val retrofits = RetrofitsImpl(
        authTokenInterceptor = authTokenInterceptor,
        baseUrlInterceptors = baseUrlInterceptors,
        headersInterceptor = headersInterceptors,
        refreshAuthenticator = refreshAuthenticator,
        sslManager = mockSslManager,
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

    @Suppress("MaxLineLength")
    @Test
    fun `createStaticRetrofit should set sslSocketFactory`() =
        runTest {
            val mockTrustManager = mockk<X509TrustManager>(relaxed = true)
            val mockSocketFactory = mockk<SSLSocketFactory>()
            val mockSslContext = mockk<SSLContext> {
                every { socketFactory } returns mockSocketFactory
            }
            setupMockOkHttpClientBuilder(
                sslContext = mockSslContext,
                trustManagers = arrayOf(mockTrustManager),
            )

            retrofits.createStaticRetrofit(isAuthenticated = false)

            verify {
                anyConstructed<OkHttpClient.Builder>()
                    .sslSocketFactory(
                        sslSocketFactory = mockSocketFactory,
                        trustManager = mockTrustManager,
                    )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticatedOkHttpClient should set sslSocketFactory`() =
        runTest {
            val mockTrustManager = mockk<X509TrustManager>(relaxed = true)
            val mockSocketFactory = mockk<SSLSocketFactory>()
            val mockSslContext = mockk<SSLContext> {
                every { socketFactory } returns mockSocketFactory
            }
            setupMockOkHttpClientBuilder(
                sslContext = mockSslContext,
                trustManagers = arrayOf(mockTrustManager),
            )

            retrofits.authenticatedApiRetrofit

            verify {
                anyConstructed<OkHttpClient.Builder>()
                    .sslSocketFactory(
                        sslSocketFactory = mockSocketFactory,
                        trustManager = mockTrustManager,
                    )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unauthenticatedOkHttpClient should set sslSocketFactory`() =
        runTest {
            val mockTrustManager = mockk<X509TrustManager>(relaxed = true)
            val mockSocketFactory = mockk<SSLSocketFactory>()
            val mockSslContext = mockk<SSLContext> {
                every { socketFactory } returns mockSocketFactory
            }
            setupMockOkHttpClientBuilder(
                sslContext = mockSslContext,
                trustManagers = arrayOf(mockTrustManager),
            )

            retrofits.unauthenticatedApiRetrofit

            verify {
                anyConstructed<OkHttpClient.Builder>()
                    .sslSocketFactory(
                        sslSocketFactory = mockSocketFactory,
                        trustManager = mockTrustManager,
                    )
            }
        }

    private fun setupMockOkHttpClientBuilder(
        sslContext: SSLContext = mockk<SSLContext>(),
        trustManagers: Array<TrustManager> = emptyArray(),
    ) {
        mockkConstructor(OkHttpClient.Builder::class)
        every { mockSslManager.sslContext } returns sslContext
        every { mockSslManager.trustManagers } returns trustManagers
        mockBuilder<OkHttpClient.Builder> {
            it.sslSocketFactory(any(), any())
        }
        every { anyConstructed<OkHttpClient.Builder>().build() } returns mockk(relaxed = true)
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
