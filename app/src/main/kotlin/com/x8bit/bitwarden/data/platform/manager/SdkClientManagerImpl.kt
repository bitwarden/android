package com.x8bit.bitwarden.data.platform.manager

import android.os.Build
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.concurrentMapOf
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkPlatformApiFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Primary implementation of [SdkClientManager].
 */
internal class SdkClientManagerImpl(
    nativeLibraryManager: NativeLibraryManager,
    dispatcherManager: DispatcherManager,
    sdkRepoFactory: SdkRepositoryFactory,
    sdkPlatformApiFactory: SdkPlatformApiFactory,
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
}
