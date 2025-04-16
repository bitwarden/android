package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.bitwarden.network.interceptor.AuthTokenInterceptor
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.BaseUrlsProvider
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.ConfigServiceImpl
import com.bitwarden.network.service.EventService
import com.bitwarden.network.service.EventServiceImpl
import com.bitwarden.network.service.PushService
import com.bitwarden.network.service.PushServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.AuthTokenManager
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.RetrofitsImpl
import com.x8bit.bitwarden.data.platform.datasource.network.ssl.SslManager
import com.x8bit.bitwarden.data.platform.datasource.network.ssl.SslManagerImpl
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_NAME
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_VERSION
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_USER_AGENT
import com.x8bit.bitwarden.data.platform.manager.KeyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.create
import javax.inject.Singleton

/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformNetworkModule {

    @Provides
    @Singleton
    fun providesConfigService(
        retrofits: Retrofits,
    ): ConfigService = ConfigServiceImpl(retrofits.unauthenticatedApiRetrofit.create())

    @Provides
    @Singleton
    fun providesEventService(
        retrofits: Retrofits,
    ): EventService = EventServiceImpl(
        eventApi = retrofits.authenticatedEventsRetrofit.create(),
    )

    @Provides
    @Singleton
    fun providePushService(
        retrofits: Retrofits,
        authDiskSource: AuthDiskSource,
    ): PushService = PushServiceImpl(
        pushApi = retrofits.authenticatedApiRetrofit.create(),
        appId = authDiskSource.uniqueAppId,
    )

    @Provides
    @Singleton
    fun providesAuthTokenInterceptor(
        authTokenManager: AuthTokenManager,
    ): AuthTokenInterceptor = AuthTokenInterceptor(
        authTokenProvider = authTokenManager,
    )

    @Provides
    @Singleton
    fun providesHeadersInterceptor(): HeadersInterceptor = HeadersInterceptor(
        userAgent = HEADER_VALUE_USER_AGENT,
        clientName = HEADER_VALUE_CLIENT_NAME,
        clientVersion = HEADER_VALUE_CLIENT_VERSION,
    )

    @Provides
    @Singleton
    fun providesRefreshAuthenticator(): RefreshAuthenticator = RefreshAuthenticator()

    @Provides
    @Singleton
    fun provideSslManager(
        keyManager: KeyManager,
        environmentRepository: EnvironmentRepository,
    ): SslManager =
        SslManagerImpl(
            keyManager = keyManager,
            environmentRepository = environmentRepository,
        )

    @Provides
    @Singleton
    fun providesBaseUrlInterceptors(
        baseUrlsProvider: BaseUrlsProvider,
    ): BaseUrlInterceptors =
        BaseUrlInterceptors(baseUrlsProvider = baseUrlsProvider)

    @Provides
    @Singleton
    fun provideRetrofits(
        authTokenInterceptor: AuthTokenInterceptor,
        baseUrlInterceptors: BaseUrlInterceptors,
        headersInterceptor: HeadersInterceptor,
        refreshAuthenticator: RefreshAuthenticator,
        sslManager: SslManager,
        json: Json,
    ): Retrofits =
        RetrofitsImpl(
            authTokenInterceptor = authTokenInterceptor,
            baseUrlInterceptors = baseUrlInterceptors,
            headersInterceptor = headersInterceptor,
            refreshAuthenticator = refreshAuthenticator,
            sslManager = sslManager,
            json = json,
        )
}
