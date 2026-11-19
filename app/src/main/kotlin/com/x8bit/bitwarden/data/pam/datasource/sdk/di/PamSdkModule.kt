package com.x8bit.bitwarden.data.pam.datasource.sdk.di

import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSourceImpl
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SDK-related dependencies for the PAM package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PamSdkModule {

    @Provides
    @Singleton
    fun providesPamSdkSource(
        sdkClientManager: SdkClientManager,
    ): PamSdkSource = PamSdkSourceImpl(sdkClientManager = sdkClientManager)
}
