package com.bitwarden.authenticator.data.authenticator.manager.di

import android.content.Context
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.FileManagerImpl
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManagerImpl
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorManagerModule {

    @Provides
    @Singleton
    fun providerFileManager(
        @ApplicationContext context: Context,
        dispatcherManager: DispatcherManager,
    ): FileManager = FileManagerImpl(
        context = context,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideTotpCodeManager(
        authenticatorSdkSource: AuthenticatorSdkSource,
        dispatcherManager: DispatcherManager,
        clock: Clock,
    ): TotpCodeManager = TotpCodeManagerImpl(
        authenticatorSdkSource = authenticatorSdkSource,
        dispatcherManager = dispatcherManager,
        clock = clock,
    )
}
