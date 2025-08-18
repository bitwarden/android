package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.bitwardenServiceClient
import com.bitwarden.network.interceptor.BaseUrlsProvider
import com.bitwarden.network.model.BitwardenServiceClientConfig
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.EventService
import com.bitwarden.network.service.PushService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.AuthTokenManager
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_NAME
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_VERSION
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_USER_AGENT
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
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
        bitwardenServiceClient: BitwardenServiceClient,
    ): ConfigService = bitwardenServiceClient.configService

    @Provides
    @Singleton
    fun providesEventService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): EventService = bitwardenServiceClient.eventService

    @Provides
    @Singleton
    fun providePushService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): PushService = bitwardenServiceClient.pushService

    @Provides
    @Singleton
    fun provideBitwardenServiceClient(
        authTokenManager: AuthTokenManager,
        baseUrlsProvider: BaseUrlsProvider,
        authDiskSource: AuthDiskSource,
        certificateManager: CertificateManager,
        buildInfoManager: BuildInfoManager,
        clock: Clock,
    ): BitwardenServiceClient = bitwardenServiceClient(
        BitwardenServiceClientConfig(
            clock = clock,
            appIdProvider = authDiskSource,
            clientData = BitwardenServiceClientConfig.ClientData(
                userAgent = HEADER_VALUE_USER_AGENT,
                clientName = HEADER_VALUE_CLIENT_NAME,
                clientVersion = HEADER_VALUE_CLIENT_VERSION,
            ),
            authTokenProvider = authTokenManager,
            baseUrlsProvider = baseUrlsProvider,
            certificateProvider = certificateManager,
            enableHttpBodyLogging = buildInfoManager.isDevBuild,
        ),
    )
}
