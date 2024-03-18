package com.x8bit.bitwarden.data.auth.datasource.sdk.di

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSourceImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.BitwardenFeatureFlagManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SDK-related dependencies for the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthSdkModule {

    @Provides
    @Singleton
    fun provideAuthSdkSource(
        client: Client,
        featureFlagManager: BitwardenFeatureFlagManager,
        dispatcherManager: DispatcherManager,
    ): AuthSdkSource = AuthSdkSourceImpl(
        clientAuth = client.auth(),
        clientPlatform = client.platform(),
        featureFlagManager = featureFlagManager,
        dispatcherManager = dispatcherManager,
    )
}
