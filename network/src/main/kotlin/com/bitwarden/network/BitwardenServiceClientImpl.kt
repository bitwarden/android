package com.bitwarden.network

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.serializer.ZonedDateTimeSerializer
import com.bitwarden.network.interceptor.AuthTokenManager
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.model.BitwardenServiceClientConfig
import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.provider.TokenProvider
import com.bitwarden.network.retrofit.Retrofits
import com.bitwarden.network.retrofit.RetrofitsImpl
import com.bitwarden.network.service.AccountsServiceImpl
import com.bitwarden.network.service.AuthRequestsService
import com.bitwarden.network.service.AuthRequestsServiceImpl
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.CiphersServiceImpl
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.ConfigServiceImpl
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.DevicesServiceImpl
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.network.service.DigitalAssetLinkServiceImpl
import com.bitwarden.network.service.DownloadService
import com.bitwarden.network.service.DownloadServiceImpl
import com.bitwarden.network.service.EventService
import com.bitwarden.network.service.EventServiceImpl
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.FolderServiceImpl
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.HaveIBeenPwnedServiceImpl
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.IdentityServiceImpl
import com.bitwarden.network.service.NewAuthRequestService
import com.bitwarden.network.service.NewAuthRequestServiceImpl
import com.bitwarden.network.service.OrganizationService
import com.bitwarden.network.service.OrganizationServiceImpl
import com.bitwarden.network.service.PushService
import com.bitwarden.network.service.PushServiceImpl
import com.bitwarden.network.service.SendsServiceImpl
import com.bitwarden.network.service.SyncServiceImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import retrofit2.create

/**
 * Primary implementation of [BitwardenServiceClient].
 */
@OmitFromCoverage
internal class BitwardenServiceClientImpl(
    private val bitwardenServiceClientConfig: BitwardenServiceClientConfig,
) : BitwardenServiceClient {

    private val authTokenManager: AuthTokenManager = AuthTokenManager(
        clock = bitwardenServiceClientConfig.clock,
        authTokenProvider = bitwardenServiceClientConfig.authTokenProvider,
    )
    override val tokenProvider: TokenProvider = authTokenManager
    private val clientJson = Json {

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
    private val retrofits: Retrofits by lazy {
        RetrofitsImpl(
            authTokenManager = authTokenManager,
            baseUrlInterceptors = BaseUrlInterceptors(
                baseUrlsProvider = bitwardenServiceClientConfig.baseUrlsProvider,
            ),
            headersInterceptor = HeadersInterceptor(
                userAgent = bitwardenServiceClientConfig.clientData.userAgent,
                clientName = bitwardenServiceClientConfig.clientData.clientName,
                clientVersion = bitwardenServiceClientConfig.clientData.clientVersion,
            ),
            logHttpBody = bitwardenServiceClientConfig.enableHttpBodyLogging,
            certificateProvider = bitwardenServiceClientConfig.certificateProvider,
            json = clientJson,
        )
    }

    override val accountsService by lazy {
        AccountsServiceImpl(
            unauthenticatedAccountsApi = retrofits.unauthenticatedApiRetrofit.create(),
            authenticatedAccountsApi = retrofits.authenticatedApiRetrofit.create(),
            unauthenticatedKeyConnectorApi = retrofits.createStaticRetrofit().create(),
            authenticatedKeyConnectorApi = retrofits
                .createStaticRetrofit(isAuthenticated = true)
                .create(),
            json = clientJson,
        )
    }

    override val authRequestsService: AuthRequestsService by lazy {
        AuthRequestsServiceImpl(
            authenticatedAuthRequestsApi = retrofits.authenticatedApiRetrofit.create(),
        )
    }

    override val ciphersService: CiphersService by lazy {
        CiphersServiceImpl(
            azureApi = retrofits.createStaticRetrofit().create(),
            ciphersApi = retrofits.authenticatedApiRetrofit.create(),
            json = clientJson,
            clock = bitwardenServiceClientConfig.clock,
        )
    }

    override val configService: ConfigService by lazy {
        ConfigServiceImpl(
            configApi = retrofits.unauthenticatedApiRetrofit.create(),
        )
    }

    override val devicesService: DevicesService by lazy {
        DevicesServiceImpl(
            authenticatedDevicesApi = retrofits.authenticatedApiRetrofit.create(),
            unauthenticatedDevicesApi = retrofits.unauthenticatedApiRetrofit.create(),
        )
    }

    override val digitalAssetLinkService: DigitalAssetLinkService by lazy {
        DigitalAssetLinkServiceImpl(
            digitalAssetLinkApi = retrofits
                .createStaticRetrofit(baseUrl = "https://digitalassetlinks.googleapis.com/")
                .create(),
        )
    }

    override val downloadService: DownloadService by lazy {
        DownloadServiceImpl(downloadApi = retrofits.createStaticRetrofit().create())
    }

    override val eventService: EventService by lazy {
        EventServiceImpl(eventApi = retrofits.authenticatedEventsRetrofit.create())
    }

    override val folderService: FolderService by lazy {
        FolderServiceImpl(
            foldersApi = retrofits.authenticatedApiRetrofit.create(),
            json = clientJson,
        )
    }

    override val haveIBeenPwnedService: HaveIBeenPwnedService by lazy {
        HaveIBeenPwnedServiceImpl(
            api = retrofits
                .createStaticRetrofit(baseUrl = "https://api.pwnedpasswords.com")
                .create(),
        )
    }

    override val identityService: IdentityService by lazy {
        IdentityServiceImpl(
            unauthenticatedIdentityApi = retrofits.unauthenticatedIdentityRetrofit.create(),
            json = clientJson,
        )
    }

    override val newAuthRequestService: NewAuthRequestService by lazy {
        NewAuthRequestServiceImpl(
            authenticatedAuthRequestsApi = retrofits.authenticatedApiRetrofit.create(),
            unauthenticatedAuthRequestsApi = retrofits.unauthenticatedApiRetrofit.create(),
        )
    }

    override val organizationService: OrganizationService by lazy {
        OrganizationServiceImpl(
            authenticatedOrganizationApi = retrofits.authenticatedApiRetrofit.create(),
            unauthenticatedOrganizationApi = retrofits.unauthenticatedApiRetrofit.create(),
        )
    }

    override val pushService: PushService by lazy {
        PushServiceImpl(
            pushApi = retrofits.authenticatedApiRetrofit.create(),
            appId = bitwardenServiceClientConfig.appIdProvider.uniqueAppId,
        )
    }

    override val sendsService by lazy {
        SendsServiceImpl(
            sendsApi = retrofits.authenticatedApiRetrofit.create(),
            azureApi = retrofits.createStaticRetrofit().create(),
            json = clientJson,
            clock = bitwardenServiceClientConfig.clock,
        )
    }

    override val syncService by lazy {
        SyncServiceImpl(
            syncApi = retrofits.authenticatedApiRetrofit.create(),
        )
    }

    override fun setRefreshTokenProvider(refreshTokenProvider: RefreshTokenProvider?) {
        authTokenManager.refreshTokenProvider = refreshTokenProvider
    }
}
