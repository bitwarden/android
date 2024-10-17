package com.x8bit.bitwarden.data.autofill.password.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessor
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides dependencies within the fido2 package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PasswordProviderModule {

    @RequiresApi(Build.VERSION_CODES.S)
    @Provides
    @Singleton
    fun providePasswordCredentialProviderProcessor(
    ): PasswordProviderProcessor =
        PasswordProviderProcessorImpl()
}
