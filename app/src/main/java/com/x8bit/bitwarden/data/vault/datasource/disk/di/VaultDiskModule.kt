package com.x8bit.bitwarden.data.vault.datasource.disk.di

import android.app.Application
import androidx.room.Room
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSourceImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.convertor.ZonedDateTimeTypeConverter
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FoldersDao
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
    fun provideVaultDatabase(app: Application): VaultDatabase =
        Room
            .databaseBuilder(
                context = app,
                klass = VaultDatabase::class.java,
                name = "vault_database",
            )
            .addTypeConverter(ZonedDateTimeTypeConverter)
            .build()

    @Provides
    @Singleton
    fun provideCipherDao(database: VaultDatabase): CiphersDao = database.cipherDao()

    @Provides
    @Singleton
    fun provideCollectionDao(database: VaultDatabase): CollectionsDao = database.collectionDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: VaultDatabase): FoldersDao = database.folderDao()

    @Provides
    @Singleton
    fun provideVaultDiskSource(
        ciphersDao: CiphersDao,
        collectionsDao: CollectionsDao,
        foldersDao: FoldersDao,
        json: Json,
    ): VaultDiskSource = VaultDiskSourceImpl(
        ciphersDao = ciphersDao,
        collectionsDao = collectionsDao,
        foldersDao = foldersDao,
        json = json,
    )
}
