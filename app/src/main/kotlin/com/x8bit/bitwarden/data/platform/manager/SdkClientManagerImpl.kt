package com.x8bit.bitwarden.data.platform.manager

import android.os.Build
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.ValidateClientSettingsException
import com.bitwarden.core.data.util.concurrentMapOf
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.ManagedSettingsBindingClient
import com.bitwarden.sdk.initLogger
import com.bitwarden.sdk.validateClientSettings as sdkValidateClientSettings
import com.x8bit.bitwarden.data.platform.manager.model.ValidateClientSettingsResult
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkPlatformApiFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.log.SdkLoggerFactory
import com.x8bit.bitwarden.data.platform.manager.util.toClientCertificateOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Primary implementation of [SdkClientManager].
 */
@Suppress("LongParameterList")
internal class SdkClientManagerImpl(
    nativeLibraryManager: NativeLibraryManager,
    private val dispatcherManager: DispatcherManager,
    private val sdkRepoFactory: SdkRepositoryFactory,
    sdkPlatformApiFactory: SdkPlatformApiFactory,
    sdkLoggerFactory: SdkLoggerFactory,
    private val certificateManager: CertificateManager,
    private val featureFlagManager: FeatureFlagManager,
    private val clientProvider: suspend (
        userId: String?,
        accessToken: String?,
    ) -> Client = { userId, accessToken ->
        Client(
            tokenProvider = sdkRepoFactory.getClientManagedTokens(
                userId = userId,
                accessToken = accessToken,
            ),
            settings = sdkRepoFactory.getClientSettings(),
            // There is no unified endpoint management source on Android, so a fresh handle is
            // passed here and no setting reads as administrator-forced.
            managedSettings = ManagedSettingsBindingClient(),
            // Resolved per client, like the base URLs above, so each client carries the
            // certificate of the environment it was built for.
            clientCertificate = withContext(dispatcherManager.io) {
                certificateManager.mutualTlsCertificate?.toClientCertificateOrNull()
            },
        )
            .apply {
                platform().loadFlags(featureFlagManager.sdkFeatureFlags)
                platform().serverCommunicationConfig(
                    repository = sdkRepoFactory.getServerCommunicationConfigRepository(),
                    platformApi = sdkPlatformApiFactory.getServerCommunicationConfigPlatformApi(),
                )
                platform().state().registerClientManagedRepositories(
                    repositories = sdkRepoFactory.getRepositories(userId = userId),
                )
                userId?.let {
                    kmStateBridge().registerBridgeImpl(sdkRepoFactory.getStateBridge(userId = it))
                }
            }
    },
) : SdkClientManager {
    private val userIdToClientMap = concurrentMapOf<String, Client>()
    private val ioScope = CoroutineScope(context = dispatcherManager.io)
    private val globalClientDeferred: Deferred<Client>

    init {
        // The SDK requires access to Android APIs that were not made public until API 31. In order
        // to work around this limitation the SDK must be manually loaded prior to initializing any
        // [Client] instance.
        if (!isBuildVersionAtLeast(Build.VERSION_CODES.S)) {
            nativeLibraryManager.loadLibrary("bitwarden_uniffi")
        }
        initLogger(
            callback = sdkLoggerFactory.getLogCallback(),
            level = sdkLoggerFactory.logLevel,
            platformLogger = false,
        )
        // Initialize this now, so that we can access it synchronously later on.
        globalClientDeferred = ioScope.async { clientProvider(null, null) }
    }

    override val globalClient: Client
        get() = runBlocking { globalClientDeferred.await() }

    override suspend fun getOrCreateClient(
        userId: String,
    ): Client = userIdToClientMap.getOrPut(key = userId) { clientProvider(userId, null) }

    override suspend fun <T> singleUseClient(
        userId: String?,
        accessToken: String?,
        block: suspend Client.() -> T,
    ): T = clientProvider(userId, accessToken).use { it.block() }

    override fun destroyClient(
        userId: String?,
    ) {
        userIdToClientMap
            .remove(key = userId)
            ?.close()
    }

    override suspend fun validateClientSettings(
        environmentUrlData: EnvironmentUrlDataJson,
    ): ValidateClientSettingsResult = withContext(dispatcherManager.io) {
        val clientCertificate = environmentUrlData.keyUri?.let { keyUri ->
            certificateManager
                .getMutualTlsCertificate(keyUri = keyUri)
                ?.toClientCertificateOrNull()
                ?: return@withContext ValidateClientSettingsResult.Error.CertificateUnavailable
        }
        val settings = sdkRepoFactory.getClientSettings(environmentUrlData = environmentUrlData)
        try {
            sdkValidateClientSettings(
                settings = settings,
                clientCertificate = clientCertificate,
            )
            ValidateClientSettingsResult.Success
        } catch (e: BitwardenException.ValidateClientSettings) {
            e.v1.toValidateClientSettingsResult()
        }
    }
}

private fun ValidateClientSettingsException.toValidateClientSettingsResult():
    ValidateClientSettingsResult = when (this) {
        is ValidateClientSettingsException.IdentityUrl,
        is ValidateClientSettingsException.ApiUrl,
            -> ValidateClientSettingsResult.Error.InvalidUrls(throwable = this)

        is ValidateClientSettingsException.ClientCertificate,
            -> ValidateClientSettingsResult.Error.UnsupportedCertificate(throwable = this)

        // Proxy and timeout settings have no UI on Android.
        else -> ValidateClientSettingsResult.Error.Other(throwable = this)
    }
