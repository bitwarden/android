package com.bitwarden.core.data.manager.di

import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.encryption.EncryptionManager
import com.bitwarden.core.data.manager.encryption.EncryptionManagerImpl
import com.bitwarden.core.data.manager.encryption.KeystoreManager
import com.bitwarden.core.data.manager.encryption.KeystoreManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides managers in the core module.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreManagerModule {

    @Provides
    @Singleton
    fun provideEncryptionManager(
        keystoreManager: KeystoreManager,
    ): EncryptionManager = EncryptionManagerImpl(
        keystoreManager = keystoreManager,
    )

    @Provides
    @Singleton
    fun provideKeystoreManager(
        buildInfoManager: BuildInfoManager,
    ): KeystoreManager = KeystoreManagerImpl(
        buildInfoManager = buildInfoManager,
    )
}
