package com.x8bit.bitwarden.data.autofill.di

import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilderImpl
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.parser.AutofillParserImpl
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessor
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessorImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides dependencies within the autofill package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutofillModule {
    @Provides
    fun providesAutofillParser(): AutofillParser = AutofillParserImpl()

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
    fun providesFillDataBuilder(): FilledDataBuilder = FilledDataBuilderImpl()

    @Provides
    fun providesFillResponseBuilder(): FillResponseBuilder = FillResponseBuilderImpl()
}
