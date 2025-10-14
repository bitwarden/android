@file:OmitFromCoverage

package com.bitwarden.network

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.network.model.BitwardenServiceClientConfig
import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.provider.TokenProvider
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.AuthRequestsService
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.network.service.DownloadService
import com.bitwarden.network.service.EventService
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.NewAuthRequestService
import com.bitwarden.network.service.OrganizationService
import com.bitwarden.network.service.PushService
import com.bitwarden.network.service.SendsService
import com.bitwarden.network.service.SyncService

/**
 * Provides access to Bitwarden services.
 *
 * New instances of this class should be created using the [bitwardenServiceClient] factory
 * function.
 *
 * Example initialization:
 * ```
 * val bitwardenServiceClient = bitwardenServiceClient(
 *     BitwardenServiceClientConfig(
 *         clock = clock,
 *         json = json,
 *         appIdProvider = appIdProvider,
 *         clientData = BitwardenServiceClientConfig.ClientData(
 *             userAgent = "my-user-agent-string",
 *             clientName = "my-application",
 *             clientVersion = "versionName",
 *         ),
 *         authTokenProvider = authTokenProvider,
 *         baseUrlsProvider = baseUrlsProvider,
 *         certificateProvider = certificateProvider,
 *     ),
 * )
 * ```
 */
interface BitwardenServiceClient {
    /**
     * Provides access to the token provider.
     */
    val tokenProvider: TokenProvider

    /**
     * Provides access to the Accounts service.
     */
    val accountsService: AccountsService

    /**
     * Provides access to the Authentication Requests service.
     */
    val authRequestsService: AuthRequestsService

    /**
     * Provides access to the Ciphers service.
     */
    val ciphersService: CiphersService

    /**
     * Provides access to the Configuration service.
     */
    val configService: ConfigService

    /**
     * Provides access to the Digital Asset Link service.
     */
    val digitalAssetLinkService: DigitalAssetLinkService

    /**
     * Provides access to the Devices service.
     */
    val devicesService: DevicesService

    /**
     * Provides access to the Download service.
     */
    val downloadService: DownloadService

    /**
     * Provides access to the Event service.
     */
    val eventService: EventService

    /**
     * Provides access to the Folder service.
     */
    val folderService: FolderService

    /**
     * Provides access to the Have I Been Pwned service.
     */
    val haveIBeenPwnedService: HaveIBeenPwnedService

    /**
     * Provides access to the Identity service.
     */
    val identityService: IdentityService

    /**
     * Provides access to the New Authentication Request service.
     */
    val newAuthRequestService: NewAuthRequestService

    /**
     * Provides access to the Organization service.
     */
    val organizationService: OrganizationService

    /**
     * Provides access to the Push service.
     */
    val pushService: PushService

    /**
     * Provides access to the Sync service.
     */
    val syncService: SyncService

    /**
     * Provides access to the Sends service.
     */
    val sendsService: SendsService

    /**
     * Sets the [refreshTokenProvider] to be used for refreshing access tokens.
     */
    fun setRefreshTokenProvider(refreshTokenProvider: RefreshTokenProvider?)
}

/**
 * Creates a [BitwardenServiceClient] with the given [config].
 *
 * Example initialization:
 * ```
 * val bitwardenServiceClient = bitwardenServiceClient(
 *     BitwardenServiceClientConfig(
 *         clock = clock,
 *         json = json,
 *         appIdProvider = appIdProvider,
 *         clientData = BitwardenServiceClientConfig.ClientData(
 *             userAgent = "my-user-agent-string",
 *             clientName = "my-application",
 *             clientVersion = "versionName",
 *         ),
 *         authTokenProvider = authTokenProvider,
 *         baseUrlsProvider = baseUrlsProvider,
 *         certificateProvider = certificateProvider,
 *     ),
 * )
 * ```
 */
fun bitwardenServiceClient(
    config: BitwardenServiceClientConfig,
): BitwardenServiceClient = BitwardenServiceClientImpl(config)
