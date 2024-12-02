package com.x8bit.bitwarden.data.tools.generator.datasource.disk.di

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import com.x8bit.bitwarden.data.platform.datasource.di.UnencryptedPreferences
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSourceImpl
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSourceImpl
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.dao.PasswordHistoryDao
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.database.PasswordHistoryDatabase
import com.x8bit.bitwarden.data.vault.datasource.disk.callback.DatabaseSchemeCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies for the generator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorDiskModule {

    @Provides
    @Singleton
    fun provideGeneratorDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): GeneratorDiskSource =
        GeneratorDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun providePasswordHistoryDiskSource(
        passwordHistoryDao: PasswordHistoryDao,
    ): PasswordHistoryDiskSource = PasswordHistoryDiskSourceImpl(
        passwordHistoryDao = passwordHistoryDao,
    )

    @Provides
    @Singleton
    fun providePasswordHistoryDatabase(
        app: Application,
        databaseSchemeManager: DatabaseSchemeManager,
    ): PasswordHistoryDatabase {
        return Room
            .databaseBuilder(
                context = app,
                klass = PasswordHistoryDatabase::class.java,
                name = "passcode_history_database",
            )
            .addCallback(DatabaseSchemeCallback(databaseSchemeManager = databaseSchemeManager))
            .build()
    }

    @Provides
    @Singleton
    fun providePasswordHistoryDao(database: PasswordHistoryDatabase): PasswordHistoryDao {
        return database.passwordHistoryDao()
    }
}
