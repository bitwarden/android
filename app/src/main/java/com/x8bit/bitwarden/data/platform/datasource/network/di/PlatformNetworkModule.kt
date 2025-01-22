package com.x8bit.bitwarden.data.platform.datasource.network.di

import android.content.Context
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.HeadersInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.RetrofitsImpl
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.ZonedDateTimeSerializer
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.service.EventService
import com.x8bit.bitwarden.data.platform.datasource.network.service.EventServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.util.TLSHelper
import com.x8bit.bitwarden.data.platform.repository.KeyChainRepository
import com.x8bit.bitwarden.data.platform.repository.KeyChainRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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
        authDiskSource: AuthDiskSource,
    ): AuthTokenInterceptor = AuthTokenInterceptor(authDiskSource = authDiskSource)

    @Provides
    @Singleton
    fun providesHeadersInterceptor(): HeadersInterceptor = HeadersInterceptor()

    @Provides
    @Singleton
    fun providesRefreshAuthenticator(): RefreshAuthenticator = RefreshAuthenticator()

    @Provides
    @Singleton
    fun providesKeyChainRepository(
        environmentDiskSource: EnvironmentDiskSource,
        @ApplicationContext context: Context,
    ): KeyChainRepository =
        KeyChainRepositoryImpl(environmentDiskSource = environmentDiskSource, context = context)

    @Provides
    @Singleton
    fun providesTlsHelper(keyChainRepository: KeyChainRepository): TLSHelper =
        TLSHelper(keyChainRepository = keyChainRepository)

    @Provides
    @Singleton
    fun provideRetrofits(
        authTokenInterceptor: AuthTokenInterceptor,
        baseUrlInterceptors: BaseUrlInterceptors,
        headersInterceptor: HeadersInterceptor,
        refreshAuthenticator: RefreshAuthenticator,
        json: Json,
        tlsHelper: TLSHelper,
    ): Retrofits =
        RetrofitsImpl(
            authTokenInterceptor = authTokenInterceptor,
            baseUrlInterceptors = baseUrlInterceptors,
            headersInterceptor = headersInterceptor,
            refreshAuthenticator = refreshAuthenticator,
            json = json,
            tlsHelper = tlsHelper,
        )

    @Provides
    @Singleton
    fun providesJson(): Json = Json {

        // If there are keys returned by the server not modeled by a serializable class,
        // ignore them.
        // This makes additive server changes non-breaking.
        ignoreUnknownKeys = true

        // We allow for nullable values to have keys missing in the JSON response.
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(ZonedDateTimeSerializer())
        }

        // Respect model default property values.
        coerceInputValues = true
    }
}
