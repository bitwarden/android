package com.x8bit.bitwarden.data.autofill.di

import android.content.Context
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilderImpl
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.parser.AutofillParserImpl
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessor
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessorImpl
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
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

    @Provides
    fun providesAutofillParser(
        settingsRepository: SettingsRepository,
    ): AutofillParser =
        AutofillParserImpl(
            settingsRepository = settingsRepository,
        )

    @Provides
    fun providesAutofillCipherProvider(): AutofillCipherProvider = AutofillCipherProviderImpl()

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
