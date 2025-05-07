package com.x8bit.bitwarden.data.credentials.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManagerImpl
import com.x8bit.bitwarden.data.credentials.manager.OriginManager
import com.x8bit.bitwarden.data.credentials.manager.OriginManagerImpl
import com.x8bit.bitwarden.data.credentials.processor.CredentialProviderProcessor
import com.x8bit.bitwarden.data.credentials.processor.CredentialProviderProcessorImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides dependencies within the fido2 package.
 */
@Module
@InstallIn(SingletonComponent::class)
object CredentialProviderModule {

    @RequiresApi(Build.VERSION_CODES.S)
    @Provides
    @Singleton
    fun provideCredentialProviderProcessor(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
        bitwardenCredentialManager: BitwardenCredentialManager,
        dispatcherManager: DispatcherManager,
        intentManager: IntentManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        featureFlagManager: FeatureFlagManager,
        clock: Clock,
    ): CredentialProviderProcessor =
        CredentialProviderProcessorImpl(
            context,
            authRepository,
            bitwardenCredentialManager,
            intentManager,
            clock,
            biometricsEncryptionManager,
            featureFlagManager,
            dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideBitwardenCredentialManager(
        @ApplicationContext context: Context,
        intentManager: IntentManager,
        featureFlagManager: FeatureFlagManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        vaultSdkSource: VaultSdkSource,
        fido2CredentialStore: Fido2CredentialStore,
        json: Json,
        environmentRepository: EnvironmentRepository,
        vaultRepository: VaultRepository,
        dispatcherManager: DispatcherManager,
    ): BitwardenCredentialManager =
        BitwardenCredentialManagerImpl(
            context = context,
            vaultSdkSource = vaultSdkSource,
            fido2CredentialStore = fido2CredentialStore,
            intentManager = intentManager,
            featureFlagManager = featureFlagManager,
            biometricsEncryptionManager = biometricsEncryptionManager,
            json = json,
            environmentRepository = environmentRepository,
            vaultRepository = vaultRepository,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideOriginManager(
        assetManager: AssetManager,
        digitalAssetLinkService: DigitalAssetLinkService,
    ): OriginManager =
        OriginManagerImpl(
            assetManager = assetManager,
            digitalAssetLinkService = digitalAssetLinkService,
        )
}
