package com.bitwarden.testharness.data.manager.di

import android.app.Application
import androidx.credentials.CredentialManager
import com.bitwarden.testharness.data.manager.CredentialTestManager
import com.bitwarden.testharness.data.manager.CredentialTestManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides dependencies for credential test managers.
 */
@Module
@InstallIn(SingletonComponent::class)
object CredentialTestManagerModule {

    @Provides
    @Singleton
    fun provideCredentialManager(
        application: Application,
    ): CredentialManager = CredentialManager.create(application.applicationContext)

    @Provides
    @Singleton
    fun provideCredentialTestManager(
        application: Application,
        credentialManager: CredentialManager,
    ): CredentialTestManager = CredentialTestManagerImpl(
        application = application,
        credentialManager = credentialManager,
    )
}
