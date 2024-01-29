package com.x8bit.bitwarden.data.platform.manager.di

import android.app.Application
import android.content.Context
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManagerImpl
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManagerImpl
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.PushManagerImpl
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManagerImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManagerImpl
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformManagerModule {

    @Provides
    @Singleton
    fun provideAppForegroundManager(): AppForegroundManager =
        AppForegroundManagerImpl()

    @Provides
    @Singleton
    fun providesCipherMatchingManager(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        vaultRepository: VaultRepository,
    ): CipherMatchingManager =
        CipherMatchingManagerImpl(
            context = context,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
        )

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideBiometricsEncryptionManager(
        settingsDiskSource: SettingsDiskSource,
    ): BiometricsEncryptionManager = BiometricsEncryptionManagerImpl(
        settingsDiskSource = settingsDiskSource,
    )

    @Provides
    @Singleton
    fun provideBitwardenClipboardManager(
        @ApplicationContext context: Context,
    ): BitwardenClipboardManager = BitwardenClipboardManagerImpl(context)

    @Provides
    @Singleton
    fun provideBitwardenDispatchers(): DispatcherManager = DispatcherManagerImpl()

    @Provides
    @Singleton
    fun provideSdkClientManager(): SdkClientManager = SdkClientManagerImpl()

    @Provides
    @Singleton
    fun provideNetworkConfigManager(
        authRepository: AuthRepository,
        authTokenInterceptor: AuthTokenInterceptor,
        environmentRepository: EnvironmentRepository,
        baseUrlInterceptors: BaseUrlInterceptors,
        refreshAuthenticator: RefreshAuthenticator,
        dispatcherManager: DispatcherManager,
    ): NetworkConfigManager =
        NetworkConfigManagerImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
            baseUrlInterceptors = baseUrlInterceptors,
            refreshAuthenticator = refreshAuthenticator,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideNetworkConnectionManager(
        application: Application,
    ): NetworkConnectionManager = NetworkConnectionManagerImpl(
        context = application.applicationContext,
    )

    @Provides
    @Singleton
    fun providePushManager(
        authDiskSource: AuthDiskSource,
        pushDiskSource: PushDiskSource,
        pushService: PushService,
        dispatcherManager: DispatcherManager,
        clock: Clock,
        json: Json,
    ): PushManager = PushManagerImpl(
        authDiskSource = authDiskSource,
        pushDiskSource = pushDiskSource,
        pushService = pushService,
        dispatcherManager = dispatcherManager,
        clock = clock,
        json = json,
    )
}
