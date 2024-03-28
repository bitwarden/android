package com.x8bit.bitwarden.authenticator.data.authenticator.manager.di

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.TotpCodeManagerImpl
import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideTotpCodeManager(
        authenticatorSdkSource: AuthenticatorSdkSource,
        dispatcherManager: DispatcherManager,
        clock: Clock,
    ): TotpCodeManager = TotpCodeManagerImpl(
        authenticatorSdkSource = authenticatorSdkSource,
        dispatcherManager = dispatcherManager,
        clock = clock
    )

}
