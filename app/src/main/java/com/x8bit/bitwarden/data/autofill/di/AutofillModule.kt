package com.x8bit.bitwarden.data.autofill.di

import android.content.Context
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilderImpl
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.parser.AutofillParserImpl
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessor
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessorImpl
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    ): AutofillCompletionManager =
        AutofillCompletionManagerImpl(
            autofillParser = autofillParser,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    fun providesAutofillParser(
        settingsRepository: SettingsRepository,
    ): AutofillParser =
        AutofillParserImpl(
            settingsRepository = settingsRepository,
        )

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

    @Provides
    fun providesAutofillProcessor(
        dispatcherManager: DispatcherManager,
        filledDataBuilder: FilledDataBuilder,
        fillResponseBuilder: FillResponseBuilder,
        parser: AutofillParser,
    ): AutofillProcessor =
        AutofillProcessorImpl(
            dispatcherManager = dispatcherManager,
            filledDataBuilder = filledDataBuilder,
            fillResponseBuilder = fillResponseBuilder,
            parser = parser,
        )

    @Provides
    fun providesFillDataBuilder(
        autofillCipherProvider: AutofillCipherProvider,
    ): FilledDataBuilder = FilledDataBuilderImpl(
        autofillCipherProvider = autofillCipherProvider,
    )

    @Provides
    fun providesFillResponseBuilder(): FillResponseBuilder = FillResponseBuilderImpl()
}
