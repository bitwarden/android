package com.bitwarden.authenticator.data.auth.datasource.disk.di

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSourceImpl
import com.bitwarden.authenticator.data.platform.datasource.di.EncryptedPreferences
import com.bitwarden.authenticator.data.platform.datasource.di.UnencryptedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthDiskModule {

    @Provides
    @Singleton
    fun provideAuthDiskSource(
        @EncryptedPreferences encryptedSharedPreferences: SharedPreferences,
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): AuthDiskSource =
        AuthDiskSourceImpl(
            encryptedSharedPreferences = encryptedSharedPreferences,
            sharedPreferences = sharedPreferences,
        )
}
