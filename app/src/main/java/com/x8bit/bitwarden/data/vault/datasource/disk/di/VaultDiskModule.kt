package com.x8bit.bitwarden.data.vault.datasource.disk.di

import android.app.Application
import androidx.room.Room
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSourceImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.callback.DatabaseSchemeCallback
import com.x8bit.bitwarden.data.vault.datasource.disk.convertor.ZonedDateTimeTypeConverter
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.DomainsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.SendsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.database.VaultDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides database dependencies in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
class VaultDiskModule {

    @Provides
    @Singleton
    fun provideVaultDatabase(
        app: Application,
        databaseSchemeManager: DatabaseSchemeManager,
    ): VaultDatabase =
        Room
            .databaseBuilder(
                context = app,
                klass = VaultDatabase::class.java,
                name = "vault_database",
            )
            .fallbackToDestructiveMigration()
            .addCallback(DatabaseSchemeCallback(databaseSchemeManager = databaseSchemeManager))
            .addTypeConverter(ZonedDateTimeTypeConverter())
            .build()

    @Provides
    @Singleton
    fun provideCipherDao(database: VaultDatabase): CiphersDao = database.cipherDao()

    @Provides
    @Singleton
    fun provideCollectionDao(database: VaultDatabase): CollectionsDao = database.collectionDao()

    @Provides
    @Singleton
    fun provideDomainsDao(database: VaultDatabase): DomainsDao = database.domainsDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: VaultDatabase): FoldersDao = database.folderDao()

    @Provides
    @Singleton
    fun provideSendDao(database: VaultDatabase): SendsDao = database.sendsDao()

    @Provides
    @Singleton
    fun provideVaultDiskSource(
        ciphersDao: CiphersDao,
        collectionsDao: CollectionsDao,
        domainsDao: DomainsDao,
        foldersDao: FoldersDao,
        sendsDao: SendsDao,
        json: Json,
        dispatcherManager: DispatcherManager,
    ): VaultDiskSource = VaultDiskSourceImpl(
        ciphersDao = ciphersDao,
        collectionsDao = collectionsDao,
        domainsDao = domainsDao,
        foldersDao = foldersDao,
        sendsDao = sendsDao,
        json = json,
        dispatcherManager = dispatcherManager,
    )
}
