package com.x8bit.bitwarden.data.autofill.credential.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.credential.processor.BitwardenCredentialProcessor
import com.x8bit.bitwarden.data.autofill.credential.processor.BitwardenCredentialProcessorImpl
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManagerImpl
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessor
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessorImpl
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessor
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.autofill.credential.manager.CredentialCompletionManager
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
 * Provides dependencies within the credential package.
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
        intentManager: IntentManager,
        fido2ProviderProcessor: Fido2ProviderProcessor,
        passwordProviderProcessor: PasswordProviderProcessor,
        dispatcherManager: DispatcherManager,
    ): BitwardenCredentialProcessor =
        BitwardenCredentialProcessorImpl(
            context,
            authRepository,
            intentManager,
            fido2ProviderProcessor,
            passwordProviderProcessor,
            dispatcherManager,
        )

}
