package com.bitwarden.authenticator.data.authenticator.datasource.disk.di

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSourceImpl
import com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemAlgorithmConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemTypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.dao.ItemDao
import com.bitwarden.authenticator.data.authenticator.datasource.disk.database.AuthenticatorDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides persistence related dependencies in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorDiskModule {

    @Provides
    @Singleton
    fun provideAuthenticatorDatabase(app: Application): AuthenticatorDatabase =
        Room
            .databaseBuilder<AuthenticatorDatabase>(context = app, name = "authenticator_database")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addColumnTypeConverter(typeConverter = AuthenticatorItemTypeConverter())
            .addColumnTypeConverter(typeConverter = AuthenticatorItemAlgorithmConverter())
            .setDriver(driver = AndroidSQLiteDriver())
            .build()

    @Provides
    @Singleton
    fun provideItemDao(database: AuthenticatorDatabase) = database.itemDao()

    @Provides
    @Singleton
    fun provideAuthenticatorDiskSource(itemDao: ItemDao): AuthenticatorDiskSource =
        AuthenticatorDiskSourceImpl(itemDao = itemDao)
}
