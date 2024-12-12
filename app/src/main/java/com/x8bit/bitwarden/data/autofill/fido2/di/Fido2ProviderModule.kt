package com.x8bit.bitwarden.data.autofill.fido2.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2OriginManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessor
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessorImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
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
        vaultRepository: VaultRepository,
        fido2CredentialStore: Fido2CredentialStore,
        fido2CredentialManager: Fido2CredentialManager,
        dispatcherManager: DispatcherManager,
        intentManager: IntentManager,
        clock: Clock,
    ): Fido2ProviderProcessor =
        Fido2ProviderProcessorImpl(
            context,
            authRepository,
            vaultRepository,
            fido2CredentialStore,
            fido2CredentialManager,
            intentManager,
            clock,
            dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideFido2CredentialManager(
        vaultSdkSource: VaultSdkSource,
        fido2CredentialStore: Fido2CredentialStore,
        fido2OriginManager: Fido2OriginManager,
        json: Json,
    ): Fido2CredentialManager =
        Fido2CredentialManagerImpl(
            vaultSdkSource = vaultSdkSource,
            fido2CredentialStore = fido2CredentialStore,
            fido2OriginManager = fido2OriginManager,
            json = json,
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
