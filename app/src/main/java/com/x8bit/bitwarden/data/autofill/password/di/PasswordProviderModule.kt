package com.x8bit.bitwarden.data.autofill.password.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessor
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessorImpl
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides dependencies within the password package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PasswordProviderModule {

    @RequiresApi(Build.VERSION_CODES.S)
    @Provides
    @Singleton
    fun providePasswordCredentialProviderProcessor(
        @ApplicationContext context: Context,
        autofillCipherProvider: AutofillCipherProvider,
        intentManager: IntentManager,
        clock: Clock,
    ): PasswordProviderProcessor =
        PasswordProviderProcessorImpl(
            context,
            autofillCipherProvider,
            intentManager,
            clock,
        )

}
