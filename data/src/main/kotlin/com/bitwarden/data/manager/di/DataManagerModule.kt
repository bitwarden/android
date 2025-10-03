package com.bitwarden.data.manager.di

import android.content.Context
import com.bitwarden.data.manager.BitwardenPackageManager
import com.bitwarden.data.manager.BitwardenPackageManagerImpl
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.data.manager.NativeLibraryManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides managers in the data module.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataManagerModule {

    @Provides
    @Singleton
    fun provideBitwardenPackageManager(
        @ApplicationContext context: Context,
    ): BitwardenPackageManager = BitwardenPackageManagerImpl(context = context)

    @Provides
    @Singleton
    fun provideNativeLibraryManager(): NativeLibraryManager = NativeLibraryManagerImpl()
}
