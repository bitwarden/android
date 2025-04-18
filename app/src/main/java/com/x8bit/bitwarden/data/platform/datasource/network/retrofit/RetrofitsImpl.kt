package com.x8bit.bitwarden.data.platform.datasource.network.retrofit

import com.bitwarden.network.core.NetworkResultCallAdapterFactory
import com.bitwarden.network.interceptor.AuthTokenInterceptor
import com.bitwarden.network.interceptor.BaseUrlInterceptor
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.ssl.CertificateProvider
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.ssl.BitwardenX509ExtendedKeyManager
import com.x8bit.bitwarden.data.platform.util.isDevBuild
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

/**
 * Primary implementation of [Retrofits].
 */
class RetrofitsImpl(
    authTokenInterceptor: AuthTokenInterceptor,
    baseUrlInterceptors: BaseUrlInterceptors,
    private val headersInterceptor: HeadersInterceptor,
    refreshAuthenticator: RefreshAuthenticator,
    json: Json,
    private val certificateProvider: CertificateProvider,
) : Retrofits {
    //region Authenticated Retrofits

    override val authenticatedApiRetrofit: Retrofit by lazy {
        createAuthenticatedRetrofit(
            baseUrlInterceptor = baseUrlInterceptors.apiInterceptor,
        )
    }

    override val authenticatedEventsRetrofit: Retrofit by lazy {
        createAuthenticatedRetrofit(
            baseUrlInterceptor = baseUrlInterceptors.eventsInterceptor,
        )
    }

    //endregion Authenticated Retrofits

    //region Unauthenticated Retrofits

    override val unauthenticatedApiRetrofit: Retrofit by lazy {
        createUnauthenticatedRetrofit(
            baseUrlInterceptor = baseUrlInterceptors.apiInterceptor,
        )
    }

    override val unauthenticatedIdentityRetrofit: Retrofit by lazy {
        createUnauthenticatedRetrofit(
            baseUrlInterceptor = baseUrlInterceptors.identityInterceptor,
        )
    }

    //endregion Unauthenticated Retrofits

    //region Static Retrofit

    override fun createStaticRetrofit(isAuthenticated: Boolean, baseUrl: String): Retrofit {
        val baseClient = if (isAuthenticated) authenticatedOkHttpClient else baseOkHttpClient
        return baseRetrofitBuilder
            .baseUrl(baseUrl)
            .client(
                baseClient
                    .newBuilder()
                    .addInterceptor(loggingInterceptor)
                    .build(),
            )
            .build()
    }

    //endregion Static Retrofit

    //region Helper properties and functions
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message -> Timber.tag("BitwardenNetworkClient").d(message) }
            .apply {
                redactHeader(name = HEADER_KEY_AUTHORIZATION)
                setLevel(
                    level = HttpLoggingInterceptor.Level.BODY
                        .takeIf { isDevBuild }
                        ?: HttpLoggingInterceptor.Level.BASIC,
                )
            }
    }

    private val baseOkHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .setSslSocketFactory()
            .build()

    private val authenticatedOkHttpClient: OkHttpClient by lazy {
        baseOkHttpClient
            .newBuilder()
            .authenticator(refreshAuthenticator)
            .addInterceptor(authTokenInterceptor)
            .build()
    }

    private val baseRetrofit: Retrofit by lazy {
        baseRetrofitBuilder
            .baseUrl("https://api.bitwarden.com")
            .build()
    }

    private val baseRetrofitBuilder: Retrofit.Builder by lazy {
        Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(NetworkResultCallAdapterFactory())
            .client(baseOkHttpClient)
    }

    private fun createAuthenticatedRetrofit(
        baseUrlInterceptor: BaseUrlInterceptor,
    ): Retrofit =
        baseRetrofit
            .newBuilder()
            .client(
                authenticatedOkHttpClient
                    .newBuilder()
                    .addInterceptor(baseUrlInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build(),
            )
            .build()

    private fun createUnauthenticatedRetrofit(
        baseUrlInterceptor: BaseUrlInterceptor,
    ): Retrofit =
        baseRetrofit
            .newBuilder()
            .client(
                baseOkHttpClient
                    .newBuilder()
                    .addInterceptor(baseUrlInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build(),
            )
            .build()

    private fun createSslTrustManagers(): Array<TrustManager> =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }.trustManagers

    private fun createX509KeyManager(): X509ExtendedKeyManager =
        BitwardenX509ExtendedKeyManager(certificateProvider = certificateProvider)

    private fun createSslContext(): SSLContext = SSLContext
        .getInstance("TLS").apply {
            init(
                arrayOf(createX509KeyManager()),
                createSslTrustManagers(),
                null,
            )
        }

    private fun OkHttpClient.Builder.setSslSocketFactory(): OkHttpClient.Builder =
        sslSocketFactory(
            createSslContext().socketFactory,
            createSslTrustManagers().first() as X509TrustManager,
        )

    //endregion Helper properties and functions
}
