package com.bitwarden.authenticator.data.platform.datasource.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
object PreferenceModule {

    @Provides
    @Singleton
    @UnencryptedPreferences
    fun provideUnencryptedSharedPreferences(
        application: Application,
    ): SharedPreferences =
        application.getSharedPreferences(
            "${application.packageName}_preferences",
            Context.MODE_PRIVATE,
        )

    @Provides
    @Singleton
    @EncryptedPreferences
    fun provideEncryptedSharedPreferences(
        application: Application,
    ): SharedPreferences =
        EncryptedSharedPreferences
            .create(
                application,
                "${application.packageName}_encrypted_preferences",
                MasterKey.Builder(application)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
}
