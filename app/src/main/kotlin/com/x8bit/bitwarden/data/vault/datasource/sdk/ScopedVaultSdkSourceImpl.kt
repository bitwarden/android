package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.manager.NativeLibraryManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkPlatformApiFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.log.SdkLoggerFactory

/**
 * The default instance of the [ScopedVaultSdkSource]. This uses its own instance of the
 * [SdkClientManagerImpl] to keep it separate from the rest of the app.
 */
@OmitFromCoverage
class ScopedVaultSdkSourceImpl(
    dispatcherManager: DispatcherManager,
    featureFlagManager: FeatureFlagManager,
    sdkRepositoryFactory: SdkRepositoryFactory,
    sdkPlatformApiFactory: SdkPlatformApiFactory,
    sdkLoggerFactory: SdkLoggerFactory,
    vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        sdkClientManager = SdkClientManagerImpl(
            dispatcherManager = dispatcherManager,
            // We do not want to have the real NativeLibraryManager used here to avoid
            // initializing the library twice.
            nativeLibraryManager = object : NativeLibraryManager {
                override fun loadLibrary(libraryName: String): Result<Unit> = Unit.asSuccess()
            },
            sdkRepoFactory = sdkRepositoryFactory,
            featureFlagManager = featureFlagManager,
            sdkPlatformApiFactory = sdkPlatformApiFactory,
            sdkLoggerFactory = sdkLoggerFactory,
        ),
        dispatcherManager = dispatcherManager,
    ),
) : ScopedVaultSdkSource, VaultSdkSource by vaultSdkSource
