package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.di

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.Fido2PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.Fido2PrivilegedAppDiskSourceImpl
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.dao.Fido2PrivilegedAppInfoDao
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.database.Fido2PrivilegedAppDatabase
import com.x8bit.bitwarden.data.platform.datasource.di.UnencryptedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the FIDO2 package.
 */
@Module
@InstallIn(SingletonComponent::class)
object Fido2DiskModule {

    @Provides
    @Singleton
    fun provideFido2PrivilegedAppDatabase(
        app: Application,
    ): Fido2PrivilegedAppDatabase =
        Room
            .databaseBuilder(
                context = app,
                klass = Fido2PrivilegedAppDatabase::class.java,
                name = "fido2_privileged_apps_database",
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFido2PrivilegedAppDao(
        database: Fido2PrivilegedAppDatabase,
    ): Fido2PrivilegedAppInfoDao = database.fido2PrivilegedAppInfoDao()

    @Provides
    @Singleton
    fun provideFido2PrivilegedAppDiskSource(
        fido2PrivilegedAppDao: Fido2PrivilegedAppInfoDao,
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): Fido2PrivilegedAppDiskSource =
        Fido2PrivilegedAppDiskSourceImpl(
            privilegedAppDao = fido2PrivilegedAppDao,
            sharedPreferences = sharedPreferences,
        )
}
