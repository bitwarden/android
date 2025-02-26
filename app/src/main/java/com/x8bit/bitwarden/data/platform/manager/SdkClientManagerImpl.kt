package com.x8bit.bitwarden.data.platform.manager

import android.os.Build
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import timber.log.Timber

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val featureFlagManager: FeatureFlagManager,
    private val clientProvider: suspend () -> Client = {
        Client(settings = null).apply {
            platform().loadFlags(featureFlagManager.sdkFeatureFlags)
        }
    },
) : SdkClientManager {
    private val userIdToClientMap = mutableMapOf<String?, Client>()

    init {
        // The SDK requires access to Android APIs that were not made public until API 31. In order
        // to work around this limitation the SDK must be manually loaded prior to initializing any
        // [Client] instance.
        if (isBuildVersionBelow(Build.VERSION_CODES.S)) {
            @Suppress("TooGenericExceptionCaught")
            try {
                System.loadLibrary("bitwarden_uniffi")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load bitwarden_uniffi library.")
            }
        }
    }

    override suspend fun getOrCreateClient(
        userId: String?,
    ): Client = userIdToClientMap.getOrPut(key = userId) { clientProvider() }

    override fun destroyClient(
        userId: String?,
    ) {
        userIdToClientMap
            .remove(key = userId)
            ?.close()
    }
}
