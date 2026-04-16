package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.core.ClientSettings
import com.bitwarden.core.DeviceType
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.network.model.BitwardenServiceClientConfig
import com.bitwarden.sdk.Repositories
import com.bitwarden.sdk.ServerCommunicationConfigRepository
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkCipherRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkLocalUserDataKeyStateRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkTokenRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.ServerCommunicationConfigRepositoryImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource

/**
 * The default implementation for the [SdkRepositoryFactory].
 */
class SdkRepositoryFactoryImpl(
    private val vaultDiskSource: VaultDiskSource,
    private val cookieDiskSource: CookieDiskSource,
    private val configDiskSource: ConfigDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val serviceClientConfig: BitwardenServiceClientConfig,
) : SdkRepositoryFactory {
    override fun getRepositories(userId: String?): Repositories =
        Repositories(
            cipher = getSdkCipherRepository(userId = userId),
            folder = null,
            userKeyState = null,
            localUserDataKeyState = SdkLocalUserDataKeyStateRepository(
                authDiskSource = authDiskSource,
            ),
            ephemeralPinEnvelopeState = null,
        )

    override fun getClientManagedTokens(
        userId: String?,
        accessToken: String?,
    ): ClientManagedTokens =
        SdkTokenRepository(
            userId = userId,
            accessToken = accessToken,
            authDiskSource = authDiskSource,
        )

    override fun getClientSettings(): ClientSettings =
        ClientSettings(
            identityUrl = serviceClientConfig.baseUrlsProvider.getBaseIdentityUrl(),
            apiUrl = serviceClientConfig.baseUrlsProvider.getBaseApiUrl(),
            userAgent = serviceClientConfig.clientData.userAgent,
            deviceType = DeviceType.ANDROID,
            deviceIdentifier = serviceClientConfig.appIdProvider.uniqueAppId,
            bitwardenClientVersion = serviceClientConfig.clientData.clientVersion,
            bitwardenPackageType = null,
        )

    override fun getServerCommunicationConfigRepository(): ServerCommunicationConfigRepository =
        ServerCommunicationConfigRepositoryImpl(
            cookieDiskSource = cookieDiskSource,
            configDiskSource = configDiskSource,
        )

    private fun getSdkCipherRepository(
        userId: String?,
    ): SdkCipherRepository? = userId?.let {
        SdkCipherRepository(userId = it, vaultDiskSource = vaultDiskSource)
    }
}
