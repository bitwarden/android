package com.x8bit.bitwarden.data.autofill.di

import android.content.Context
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilderImpl
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.builder.SaveInfoBuilder
import com.x8bit.bitwarden.data.autofill.builder.SaveInfoBuilderImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillTotpManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillTotpManagerImpl
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.parser.AutofillParserImpl
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessor
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessorImpl
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides dependencies within the autofill package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutofillModule {

    @Singleton
    @Provides
    fun providesAutofillManager(
        @ApplicationContext context: Context,
    ): AutofillManager = context.getSystemService(AutofillManager::class.java)

    @Singleton
    @Provides
    fun providesAutofillEnabledManager(): AutofillEnabledManager =
        AutofillEnabledManagerImpl()

    @Singleton
    @Provides
    fun provideAutofillCompletionManager(
        autofillParser: AutofillParser,
        dispatcherManager: DispatcherManager,
        organizationEventManager: OrganizationEventManager,
        totpManager: AutofillTotpManager,
    ): AutofillCompletionManager =
        AutofillCompletionManagerImpl(
            autofillParser = autofillParser,
            dispatcherManager = dispatcherManager,
            organizationEventManager = organizationEventManager,
            totpManager = totpManager,
        )

    @Singleton
    @Provides
    fun providesAutofillParser(
        settingsRepository: SettingsRepository,
    ): AutofillParser =
        AutofillParserImpl(
            settingsRepository = settingsRepository,
        )

    @Singleton
    @Provides
    fun providesAutofillTotpManager(
        @ApplicationContext context: Context,
        clock: Clock,
        clipboardManager: BitwardenClipboardManager,
        authRepository: AuthRepository,
        settingsRepository: SettingsRepository,
        vaultRepository: VaultRepository,
    ): AutofillTotpManager =
        AutofillTotpManagerImpl(
            context = context,
            clock = clock,
            clipboardManager = clipboardManager,
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
        )

    @Singleton
    @Provides
    fun providesAutofillCipherProvider(
        authRepository: AuthRepository,
        cipherMatchingManager: CipherMatchingManager,
        vaultRepository: VaultRepository,
    ): AutofillCipherProvider =
        AutofillCipherProviderImpl(
            authRepository = authRepository,
            cipherMatchingManager = cipherMatchingManager,
            vaultRepository = vaultRepository,
        )

    @Singleton
    @Provides
    fun providesAutofillProcessor(
        dispatcherManager: DispatcherManager,
        filledDataBuilder: FilledDataBuilder,
        fillResponseBuilder: FillResponseBuilder,
        parser: AutofillParser,
        policyManager: PolicyManager,
        saveInfoBuilder: SaveInfoBuilder,
        settingsRepository: SettingsRepository,
    ): AutofillProcessor =
        AutofillProcessorImpl(
            dispatcherManager = dispatcherManager,
            filledDataBuilder = filledDataBuilder,
            fillResponseBuilder = fillResponseBuilder,
            parser = parser,
            policyManager = policyManager,
            saveInfoBuilder = saveInfoBuilder,
            settingsRepository = settingsRepository,
        )

    @Singleton
    @Provides
    fun providesFillDataBuilder(
        autofillCipherProvider: AutofillCipherProvider,
    ): FilledDataBuilder = FilledDataBuilderImpl(
        autofillCipherProvider = autofillCipherProvider,
    )

    @Singleton
    @Provides
    fun providesFillResponseBuilder(): FillResponseBuilder = FillResponseBuilderImpl()

    @Singleton
    @Provides
    fun providesSaveInfoBuilder(
        settingsRepository: SettingsRepository,
    ): SaveInfoBuilder =
        SaveInfoBuilderImpl(
            settingsRepository = settingsRepository,
        )
}
