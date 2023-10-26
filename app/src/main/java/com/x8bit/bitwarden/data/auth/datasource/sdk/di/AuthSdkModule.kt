package com.x8bit.bitwarden.data.auth.datasource.sdk.di

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSourceImpl
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
    ): AuthSdkSource = AuthSdkSourceImpl(clientAuth = client.auth())
}
