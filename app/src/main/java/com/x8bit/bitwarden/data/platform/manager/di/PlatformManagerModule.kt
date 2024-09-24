package com.x8bit.bitwarden.data.platform.manager.di

import android.app.Application
import android.content.Context
import androidx.core.content.getSystemService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.datasource.network.service.EventService
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManagerImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.AssetManagerImpl
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManagerImpl
import com.x8bit.bitwarden.data.platform.processor.AuthenticatorBridgeProcessor
import com.x8bit.bitwarden.data.platform.processor.AuthenticatorBridgeProcessorImpl
import com.x8bit.bitwarden.data.platform.manager.CrashLogsManager
import com.x8bit.bitwarden.data.platform.manager.CrashLogsManagerImpl
import com.x8bit.bitwarden.data.platform.manager.DebugMenuFeatureFlagManagerImpl
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManagerImpl
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManagerImpl
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManagerImpl
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.PushManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManagerImpl
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManagerImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManagerImpl
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManagerImpl
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManager
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManagerImpl
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepository
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
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
    fun provideAuthenticatorBridgeServiceProcessor(
        authenticatorBridgeRepository: AuthenticatorBridgeRepository,
        dispatcherManager: DispatcherManager,
        featureFlagManager: FeatureFlagManager,
    ): AuthenticatorBridgeProcessor = AuthenticatorBridgeProcessorImpl(
        authenticatorBridgeRepository = authenticatorBridgeRepository,
        dispatcherManager = dispatcherManager,
        featureFlagManager = featureFlagManager,
    )

    @Provides
    @Singleton
    fun provideOrganizationEventManager(
        authRepository: AuthRepository,
        vaultRepository: VaultRepository,
        clock: Clock,
        dispatcherManager: DispatcherManager,
        eventDiskSource: EventDiskSource,
        eventService: EventService,
    ): OrganizationEventManager = OrganizationEventManagerImpl(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        clock = clock,
        dispatcherManager = dispatcherManager,
        eventDiskSource = eventDiskSource,
        eventService = eventService,
    )

    @Provides
    @Singleton
    fun providesCipherMatchingManager(
        resourceCacheManager: ResourceCacheManager,
        settingsRepository: SettingsRepository,
        vaultRepository: VaultRepository,
    ): CipherMatchingManager =
        CipherMatchingManagerImpl(
            resourceCacheManager = resourceCacheManager,
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
        settingsRepository: SettingsRepository,
    ): BitwardenClipboardManager = BitwardenClipboardManagerImpl(
        context,
        settingsRepository,
    )

    @Provides
    @Singleton
    fun provideBitwardenDispatchers(): DispatcherManager = DispatcherManagerImpl()

    @Provides
    @Singleton
    fun provideGarbageCollectionManager(
        dispatcherManager: DispatcherManager,
    ): GarbageCollectionManager = GarbageCollectionManagerImpl(
        dispatcherManager = dispatcherManager,
    )

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

    @Provides
    @Singleton
    fun provideSdkClientManager(
        featureFlagManager: FeatureFlagManager,
    ): SdkClientManager = SdkClientManagerImpl(
        featureFlagManager = featureFlagManager,
    )

    @Provides
    @Singleton
    fun provideNetworkConfigManager(
        authRepository: AuthRepository,
        authTokenInterceptor: AuthTokenInterceptor,
        environmentRepository: EnvironmentRepository,
        serverConfigRepository: ServerConfigRepository,
        baseUrlInterceptors: BaseUrlInterceptors,
        refreshAuthenticator: RefreshAuthenticator,
        dispatcherManager: DispatcherManager,
    ): NetworkConfigManager =
        NetworkConfigManagerImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
            serverConfigRepository = serverConfigRepository,
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
    fun providePolicyManager(
        authDiskSource: AuthDiskSource,
    ): PolicyManager = PolicyManagerImpl(
        authDiskSource = authDiskSource,
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

    @Provides
    @Singleton
    fun provideCrashLogsManager(
        legacyAppCenterMigrator: LegacyAppCenterMigrator,
        settingsRepository: SettingsRepository,
    ): CrashLogsManager = CrashLogsManagerImpl(
        settingsRepository = settingsRepository,
        legacyAppCenterMigrator = legacyAppCenterMigrator,
    )

    @Provides
    @Singleton
    fun provideAssetManager(
        @ApplicationContext context: Context,
        dispatcherManager: DispatcherManager,
    ): AssetManager = AssetManagerImpl(
        context = context,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideRestrictionManager(
        @ApplicationContext context: Context,
        appForegroundManager: AppForegroundManager,
        dispatcherManager: DispatcherManager,
        environmentRepository: EnvironmentRepository,
    ): RestrictionManager = RestrictionManagerImpl(
        appForegroundManager = appForegroundManager,
        dispatcherManager = dispatcherManager,
        context = context,
        environmentRepository = environmentRepository,
        restrictionsManager = requireNotNull(context.getSystemService()),
    )

    @Provides
    @Singleton
    fun provideResourceCacheManager(
        @ApplicationContext context: Context,
    ): ResourceCacheManager = ResourceCacheManagerImpl(context = context)
}
