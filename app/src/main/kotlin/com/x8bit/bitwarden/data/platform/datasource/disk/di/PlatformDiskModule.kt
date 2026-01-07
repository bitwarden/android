package com.x8bit.bitwarden.data.platform.datasource.disk.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.FlightRecorderDiskSource
import com.bitwarden.data.datasource.disk.di.EncryptedPreferences
import com.bitwarden.data.datasource.disk.di.UnencryptedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.dao.OrganizationEventDao
import com.x8bit.bitwarden.data.platform.datasource.disk.database.PlatformDatabase
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigratorImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorage
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigrator
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigratorImpl
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.callback.DatabaseSchemeCallback
import com.x8bit.bitwarden.data.vault.datasource.disk.convertor.ZonedDateTimeTypeConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformDiskModule {

    @Provides
    @Singleton
    fun provideEnvironmentDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): EnvironmentDiskSource =
        EnvironmentDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun provideEventDatabase(
        app: Application,
        databaseSchemeManager: DatabaseSchemeManager,
    ): PlatformDatabase =
        Room
            .databaseBuilder(
                context = app,
                klass = PlatformDatabase::class.java,
                name = "platform_database",
            )
            .fallbackToDestructiveMigration(dropAllTables = false)
            .addTypeConverter(ZonedDateTimeTypeConverter())
            .addCallback(DatabaseSchemeCallback(databaseSchemeManager = databaseSchemeManager))
            .build()

    @Provides
    @Singleton
    fun provideOrganizationEventDao(
        database: PlatformDatabase,
    ): OrganizationEventDao = database.organizationEventDao()

    @Provides
    @Singleton
    fun provideEventDiskSource(
        organizationEventDao: OrganizationEventDao,
        dispatcherManager: DispatcherManager,
        json: Json,
    ): EventDiskSource =
        EventDiskSourceImpl(
            organizationEventDao = organizationEventDao,
            dispatcherManager = dispatcherManager,
            json = json,
        )

    @Provides
    @Singleton
    fun provideLegacySecureStorage(
        @ApplicationContext context: Context,
    ): LegacySecureStorage =
        LegacySecureStorageImpl(
            context = context,
        )

    @Provides
    @Singleton
    fun provideLegacySecureStorageMigrator(
        legacySecureStorage: LegacySecureStorage,
        @EncryptedPreferences encryptedSharedPreferences: SharedPreferences,
    ): LegacySecureStorageMigrator =
        LegacySecureStorageMigratorImpl(
            legacySecureStorage = legacySecureStorage,
            encryptedSharedPreferences = encryptedSharedPreferences,
        )

    @Provides
    @Singleton
    fun provideLegacyAppCenterMigrator(
        application: Application,
        settingsRepository: SettingsRepository,
    ): LegacyAppCenterMigrator =
        LegacyAppCenterMigratorImpl(
            settingsRepository = settingsRepository,
            appCenterPreferences = application.getSharedPreferences(
                "AppCenter",
                Context.MODE_PRIVATE,
            ),
        )

    @Provides
    @Singleton
    fun providePushDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): PushDiskSource =
        PushDiskSourceImpl(
            sharedPreferences = sharedPreferences,
        )

    @Provides
    @Singleton
    fun provideSettingsDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
        flightRecorderDiskSource: FlightRecorderDiskSource,
    ): SettingsDiskSource =
        SettingsDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
            flightRecorderDiskSource = flightRecorderDiskSource,
        )

    @Provides
    @Singleton
    fun provideFeatureFlagOverrideDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): FeatureFlagOverrideDiskSource = FeatureFlagOverrideDiskSourceImpl(
        sharedPreferences = sharedPreferences,
    )
}
