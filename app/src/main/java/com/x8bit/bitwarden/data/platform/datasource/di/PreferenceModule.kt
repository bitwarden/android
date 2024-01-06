package com.x8bit.bitwarden.data.platform.datasource.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
    fun provideDefaultSharedPreferences(
        application: Application,
    ): SharedPreferences =
        application.getSharedPreferences(
            "${application.packageName}_preferences",
            Context.MODE_PRIVATE,
        )
}
