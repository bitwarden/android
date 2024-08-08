package com.x8bit.bitwarden.data.platform.datasource.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.GeneralSecurityException
import java.security.KeyStore
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
        @Suppress("TooGenericExceptionCaught")
        try {
            getEncryptedSharedPreferences(application = application)
        } catch (e: GeneralSecurityException) {
            // Handle when a bad master key or key-set has been attempted
            destroyEncryptedSharedPreferencesAndRebuild(application = application)
        } catch (e: RuntimeException) {
            // Handle KeystoreExceptions that get wrapped up in a RuntimeException
            destroyEncryptedSharedPreferencesAndRebuild(application = application)
        }

    /**
     * Completely destroys the keystore master key and encrypted shared preferences file. This will
     * cause all users to be logged out since the access and refresh tokens will be removed.
     *
     * This is not desirable and should only be called if we have completely failed to access our
     * encrypted shared preferences instance.
     */
    private fun destroyEncryptedSharedPreferencesAndRebuild(
        application: Application,
    ): SharedPreferences {
        // Delete the master key
        KeyStore.getInstance(KeyStore.getDefaultType()).run {
            load(null)
            deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        // Deletes the encrypted shared preferences file
        application.deleteSharedPreferences(application.encryptedSharedPreferencesName)
        // Attempts to create the encrypted shared preferences instance
        return getEncryptedSharedPreferences(application = application)
    }

    /**
     * Helper method to get the app's encrypted shared preferences instance.
     */
    private fun getEncryptedSharedPreferences(
        application: Application,
    ): SharedPreferences =
        EncryptedSharedPreferences.create(
            application,
            application.encryptedSharedPreferencesName,
            MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    /**
     * Helper method to get the app's encrypted shared preferences name.
     */
    private val Application.encryptedSharedPreferencesName: String
        get() = "${packageName}_encrypted_preferences"
}
