package com.bitwarden.core.data.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.core.data.manager.UuidManagerImpl
import com.bitwarden.core.data.manager.biometrics.BiometricsSupportManager
import com.bitwarden.core.data.manager.biometrics.BiometricsSupportManagerImpl
import com.bitwarden.core.data.manager.encryption.EncryptionManager
import com.bitwarden.core.data.manager.encryption.EncryptionManagerImpl
import com.bitwarden.core.data.manager.encryption.KeystoreManager
import com.bitwarden.core.data.manager.encryption.KeystoreManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideBiometricsSupportManager(
        @ApplicationContext context: Context,
    ): BiometricsSupportManager = BiometricsSupportManagerImpl(
        context = context,
    )

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

    @Provides
    @Singleton
    fun provideUuidManager(): UuidManager = UuidManagerImpl()
}
