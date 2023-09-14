package com.x8bit.bitwarden.data.platform.datasource.di

import com.bitwarden.sdk.Client
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides dependencies related to encryption / decryption / secure generation.
 */
@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    @Provides
    @Singleton
    fun provideBitwardenClient(): Client {
        return Client(null)
    }
}
