package com.bitwarden.authenticator.data.platform.manager.di

import android.content.Context
import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManagerImpl
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManager
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManagerImpl
import com.bitwarden.authenticator.data.platform.manager.CrashLogsManager
import com.bitwarden.authenticator.data.platform.manager.CrashLogsManagerImpl
import com.bitwarden.authenticator.data.platform.manager.DebugMenuFeatureFlagManagerImpl
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManagerImpl
import com.bitwarden.authenticator.data.platform.manager.SdkClientManager
import com.bitwarden.authenticator.data.platform.manager.SdkClientManagerImpl
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManagerImpl
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManagerImpl
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManagerImpl
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.core.data.manager.realtime.RealtimeManagerImpl
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.manager.toast.ToastManagerImpl
import com.bitwarden.data.repository.ServerConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformManagerModule {

    @Provides
    @Singleton
    fun provideBitwardenClipboardManager(
        @ApplicationContext context: Context,
        toastManager: ToastManager,
    ): BitwardenClipboardManager = BitwardenClipboardManagerImpl(
        context = context,
        toastManager = toastManager,
    )

    @Provides
    @Singleton
    fun provideRealtimeManager(): RealtimeManager = RealtimeManagerImpl()

    @Provides
    @Singleton
    fun provideToastManager(
        @ApplicationContext context: Context,
    ): ToastManager = ToastManagerImpl(
        context = context,
    )

    @Provides
    @Singleton
    fun provideBitwardenDispatchers(): DispatcherManager = DispatcherManagerImpl()

    @Provides
    @Singleton
    fun provideSdkClientManager(): SdkClientManager = SdkClientManagerImpl()

    @Provides
    @Singleton
    fun provideBiometricsEncryptionManager(
        settingsDiskSource: SettingsDiskSource,
    ): BiometricsEncryptionManager = BiometricsEncryptionManagerImpl(settingsDiskSource)

    @Provides
    @Singleton
    fun provideCrashLogsManager(settingsRepository: SettingsRepository): CrashLogsManager =
        CrashLogsManagerImpl(
            settingsRepository = settingsRepository,
        )

    @Provides
    @Singleton
    fun provideImportManager(
        authenticatorDiskSource: AuthenticatorDiskSource,
    ): ImportManager = ImportManagerImpl(authenticatorDiskSource)

    @Provides
    @Singleton
    fun provideEncodingManager(): BitwardenEncodingManager = BitwardenEncodingManagerImpl()

    @Provides
    @Singleton
    fun providesFeatureFlagManager(
        debugMenuRepository: DebugMenuRepository,
        serverConfigRepository: ServerConfigRepository,
    ): FeatureFlagManager = if (debugMenuRepository.isDebugMenuEnabled) {
        DebugMenuFeatureFlagManagerImpl(
            debugMenuRepository = debugMenuRepository,
            defaultFeatureFlagManager = FeatureFlagManagerImpl(
                serverConfigRepository = serverConfigRepository,
            ),
        )
    } else {
        FeatureFlagManagerImpl(
            serverConfigRepository = serverConfigRepository,
        )
    }
}
