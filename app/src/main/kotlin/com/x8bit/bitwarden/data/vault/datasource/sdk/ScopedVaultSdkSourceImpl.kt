package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.manager.NativeLibraryManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory

/**
 * The default instance of the [ScopedVaultSdkSource]. This uses its own instance of the
 * [SdkClientManagerImpl] to keep it separate from the rest of the app.
 */
@OmitFromCoverage
class ScopedVaultSdkSourceImpl(
    dispatcherManager: DispatcherManager,
    featureFlagManager: FeatureFlagManager,
    sdkRepositoryFactory: SdkRepositoryFactory,
    vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        sdkClientManager = SdkClientManagerImpl(
            // We do not want to have the real NativeLibraryManager used here to avoid
            // initializing the library twice.
            nativeLibraryManager = object : NativeLibraryManager {
                override fun loadLibrary(libraryName: String): Result<Unit> = Unit.asSuccess()
            },
            sdkRepoFactory = sdkRepositoryFactory,
            featureFlagManager = featureFlagManager,
        ),
        dispatcherManager = dispatcherManager,
    ),
) : ScopedVaultSdkSource, VaultSdkSource by vaultSdkSource
