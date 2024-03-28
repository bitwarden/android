package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.di

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides persistence related dependencies in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorDiskModule {

    @Provides
    @Singleton
    fun provideAuthenticatorDiskSource(): AuthenticatorDiskSource = AuthenticatorDiskSourceImpl()

}
