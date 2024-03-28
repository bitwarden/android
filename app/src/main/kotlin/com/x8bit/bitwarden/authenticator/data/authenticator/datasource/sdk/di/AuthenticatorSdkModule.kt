package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk.di

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSourceImpl
import com.x8bit.bitwarden.authenticator.data.platform.manager.SdkClientManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SDK-related dependencies for the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorSdkModule {

    @Provides
    @Singleton
    fun provideAuthenticatorSdkSource(
        sdkClientManager: SdkClientManager,
    ): AuthenticatorSdkSource = AuthenticatorSdkSourceImpl(sdkClientManager)

}
