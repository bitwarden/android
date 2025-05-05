package com.x8bit.bitwarden.data.autofill.fido2.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessor
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessorImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
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
object Fido2ProviderModule {

    @RequiresApi(Build.VERSION_CODES.S)
    @Provides
    @Singleton
    fun provideCredentialProviderProcessor(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
        fido2CredentialManager: Fido2CredentialManager,
        dispatcherManager: DispatcherManager,
        intentManager: IntentManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        featureFlagManager: FeatureFlagManager,
        clock: Clock,
    ): Fido2ProviderProcessor =
        Fido2ProviderProcessorImpl(
            context,
            authRepository,
            fido2CredentialManager,
            intentManager,
            clock,
            biometricsEncryptionManager,
            featureFlagManager,
            dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideFido2CredentialManager(
        @ApplicationContext context: Context,
        intentManager: IntentManager,
        featureFlagManager: FeatureFlagManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        vaultSdkSource: VaultSdkSource,
        fido2CredentialStore: Fido2CredentialStore,
        json: Json,
        environmentRepository: EnvironmentRepository,
        settingsRepository: SettingsRepository,
        vaultRepository: VaultRepository,
        dispatcherManager: DispatcherManager,
    ): Fido2CredentialManager =
        Fido2CredentialManagerImpl(
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
    fun provideFido2OriginManager(
        assetManager: AssetManager,
        digitalAssetLinkService: DigitalAssetLinkService,
    ): Fido2OriginManager =
        Fido2OriginManagerImpl(
            assetManager = assetManager,
            digitalAssetLinkService = digitalAssetLinkService,
        )
}
