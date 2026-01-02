package com.bitwarden.authenticator.data.platform.datasource.network.di

import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_NAME
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_VERSION
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_USER_AGENT
import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.bitwardenServiceClient
import com.bitwarden.network.interceptor.AuthTokenProvider
import com.bitwarden.network.interceptor.BaseUrlsProvider
import com.bitwarden.network.model.AuthTokenData
import com.bitwarden.network.model.BitwardenServiceClientConfig
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.DownloadService
import com.bitwarden.network.ssl.CertificateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
object PlatformNetworkModule {
    @Provides
    @Singleton
    fun providesConfigService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): ConfigService = bitwardenServiceClient.configService

    @Provides
    @Singleton
    fun provideBitwardenServiceClient(
        baseUrlsProvider: BaseUrlsProvider,
        authDiskSource: AuthDiskSource,
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
            baseUrlsProvider = baseUrlsProvider,
            enableHttpBodyLogging = BuildConfig.DEBUG,
            authTokenProvider = object : AuthTokenProvider {
                override fun getAuthTokenDataOrNull(): AuthTokenData? = null
                override fun getAuthTokenDataOrNull(userId: String): AuthTokenData? = null
            },
            certificateProvider = object : CertificateProvider {
                override fun chooseClientAlias(
                    keyType: Array<out String>?,
                    issuers: Array<out Principal>?,
                    socket: Socket?,
                ) = ""

                override fun getCertificateChain(alias: String?): Array<X509Certificate>? = null

                override fun getPrivateKey(alias: String?): PrivateKey? = null
            },
        ),
    )

    @Provides
    @Singleton
    fun provideDownloadService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): DownloadService = bitwardenServiceClient.downloadService
}
