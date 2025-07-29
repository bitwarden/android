package com.x8bit.bitwarden.data.credentials.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.credentials.builder.CredentialEntryBuilder
import com.x8bit.bitwarden.data.credentials.builder.CredentialEntryBuilderImpl
import com.x8bit.bitwarden.data.credentials.datasource.disk.PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManagerImpl
import com.x8bit.bitwarden.data.credentials.manager.OriginManager
import com.x8bit.bitwarden.data.credentials.manager.OriginManagerImpl
import com.x8bit.bitwarden.data.credentials.parser.RelyingPartyParser
import com.x8bit.bitwarden.data.credentials.parser.RelyingPartyParserImpl
import com.x8bit.bitwarden.data.credentials.processor.CredentialProviderProcessor
import com.x8bit.bitwarden.data.credentials.processor.CredentialProviderProcessorImpl
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepositoryImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
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
        clock: Clock,
    ): CredentialProviderProcessor =
        CredentialProviderProcessorImpl(
            context = context,
            authRepository = authRepository,
            bitwardenCredentialManager = bitwardenCredentialManager,
            intentManager = intentManager,
            clock = clock,
            biometricsEncryptionManager = biometricsEncryptionManager,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideBitwardenCredentialManager(
        vaultSdkSource: VaultSdkSource,
        fido2CredentialStore: Fido2CredentialStore,
        json: Json,
        vaultRepository: VaultRepository,
        dispatcherManager: DispatcherManager,
        credentialEntryBuilder: CredentialEntryBuilder,
        cipherMatchingManager: CipherMatchingManager,
    ): BitwardenCredentialManager =
        BitwardenCredentialManagerImpl(
            vaultSdkSource = vaultSdkSource,
            fido2CredentialStore = fido2CredentialStore,
            json = json,
            vaultRepository = vaultRepository,
            dispatcherManager = dispatcherManager,
            credentialEntryBuilder = credentialEntryBuilder,
            cipherMatchingManager = cipherMatchingManager,
        )

    @Provides
    @Singleton
    fun provideOriginManager(
        assetManager: AssetManager,
        digitalAssetLinkService: DigitalAssetLinkService,
        privilegedAppRepository: PrivilegedAppRepository,
        featureFlagManager: FeatureFlagManager,
    ): OriginManager =
        OriginManagerImpl(
            assetManager = assetManager,
            digitalAssetLinkService = digitalAssetLinkService,
            privilegedAppRepository = privilegedAppRepository,
            featureFlagManager = featureFlagManager,
        )

    @Provides
    @Singleton
    fun provideCredentialEntryBuilder(
        @ApplicationContext context: Context,
        intentManager: IntentManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
    ): CredentialEntryBuilder = CredentialEntryBuilderImpl(
        context = context,
        intentManager = intentManager,
        biometricsEncryptionManager = biometricsEncryptionManager,
    )

    @Provides
    @Singleton
    fun providePrivilegedAppRepository(
        privilegedAppDiskSource: PrivilegedAppDiskSource,
        assetManager: AssetManager,
        dispatcherManager: DispatcherManager,
        json: Json,
    ): PrivilegedAppRepository = PrivilegedAppRepositoryImpl(
        privilegedAppDiskSource = privilegedAppDiskSource,
        assetManager = assetManager,
        dispatcherManager = dispatcherManager,
        json = json,
    )

    @Provides
    @Singleton
    fun provideRelyingPartyParser(
        json: Json,
    ): RelyingPartyParser = RelyingPartyParserImpl(json)
}
