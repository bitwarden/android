package com.x8bit.bitwarden.data.platform.manager.di

import android.app.Application
import android.content.Context
import androidx.core.content.getSystemService
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.manager.toast.ToastManagerImpl
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.data.manager.DispatcherManagerImpl
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.service.EventService
import com.bitwarden.network.service.PushService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManager
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.manager.AppResumeManager
import com.x8bit.bitwarden.data.platform.manager.AppResumeManagerImpl
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.AppStateManagerImpl
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.AssetManagerImpl
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import com.x8bit.bitwarden.data.platform.manager.CertificateManagerImpl
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManagerImpl
import com.x8bit.bitwarden.data.platform.manager.DebugMenuFeatureFlagManagerImpl
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManagerImpl
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.LogsManagerImpl
import com.x8bit.bitwarden.data.platform.manager.NativeLibraryManager
import com.x8bit.bitwarden.data.platform.manager.NativeLibraryManagerImpl
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManagerImpl
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.PushManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManagerImpl
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManagerImpl
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManagerImpl
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManagerImpl
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderManager
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderManagerImpl
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderWriter
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderWriterImpl
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConfigManagerImpl
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManager
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManagerImpl
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactoryImpl
import com.x8bit.bitwarden.data.platform.processor.AuthenticatorBridgeProcessor
import com.x8bit.bitwarden.data.platform.processor.AuthenticatorBridgeProcessorImpl
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepository
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
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
    fun provideAppStateManager(
        application: Application,
    ): AppStateManager = AppStateManagerImpl(application = application)

    @Provides
    @Singleton
    fun provideFlightRecorderWriter(
        clock: Clock,
        fileManager: FileManager,
        dispatcherManager: DispatcherManager,
    ): FlightRecorderWriter = FlightRecorderWriterImpl(
        clock = clock,
        fileManager = fileManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideFlightRecorderManager(
        @ApplicationContext context: Context,
        clock: Clock,
        dispatcherManager: DispatcherManager,
        settingsDiskSource: SettingsDiskSource,
        flightRecorderWriter: FlightRecorderWriter,
    ): FlightRecorderManager = FlightRecorderManagerImpl(
        context = context,
        clock = clock,
        dispatcherManager = dispatcherManager,
        settingsDiskSource = settingsDiskSource,
        flightRecorderWriter = flightRecorderWriter,
    )

    @Provides
    @Singleton
    fun provideAuthenticatorBridgeProcessor(
        authenticatorBridgeRepository: AuthenticatorBridgeRepository,
        addTotpItemFromAuthenticatorManager: AddTotpItemFromAuthenticatorManager,
        @ApplicationContext context: Context,
        dispatcherManager: DispatcherManager,
    ): AuthenticatorBridgeProcessor = AuthenticatorBridgeProcessorImpl(
        authenticatorBridgeRepository = authenticatorBridgeRepository,
        addTotpItemFromAuthenticatorManager = addTotpItemFromAuthenticatorManager,
        context = context,
        dispatcherManager = dispatcherManager,
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
    fun provideBiometricsEncryptionManager(
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
    ): BiometricsEncryptionManager = BiometricsEncryptionManagerImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
    )

    @Provides
    @Singleton
    fun provideBitwardenClipboardManager(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        toastManager: ToastManager,
    ): BitwardenClipboardManager = BitwardenClipboardManagerImpl(
        context = context,
        settingsRepository = settingsRepository,
        toastManager = toastManager,
    )

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
    fun provideNativeLibraryManager(): NativeLibraryManager = NativeLibraryManagerImpl()

    @Provides
    @Singleton
    fun provideSdkClientManager(
        featureFlagManager: FeatureFlagManager,
        nativeLibraryManager: NativeLibraryManager,
        sdkRepositoryFactory: SdkRepositoryFactory,
    ): SdkClientManager = SdkClientManagerImpl(
        featureFlagManager = featureFlagManager,
        nativeLibraryManager = nativeLibraryManager,
        sdkRepoFactory = sdkRepositoryFactory,
    )

    @Provides
    @Singleton
    fun provideNetworkConfigManager(
        authRepository: AuthRepository,
        environmentRepository: EnvironmentRepository,
        serverConfigRepository: ServerConfigRepository,
        bitwardenServiceClient: BitwardenServiceClient,
        dispatcherManager: DispatcherManager,
    ): NetworkConfigManager =
        NetworkConfigManagerImpl(
            authRepository = authRepository,
            environmentRepository = environmentRepository,
            serverConfigRepository = serverConfigRepository,
            bitwardenServiceClient = bitwardenServiceClient,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideNetworkConnectionManager(
        application: Application,
        dispatcherManager: DispatcherManager,
    ): NetworkConnectionManager = NetworkConnectionManagerImpl(
        context = application.applicationContext,
        dispatcherManager = dispatcherManager,
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
    fun provideLogsManager(
        legacyAppCenterMigrator: LegacyAppCenterMigrator,
        settingsRepository: SettingsRepository,
    ): LogsManager = LogsManagerImpl(
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
        appStateManager: AppStateManager,
        dispatcherManager: DispatcherManager,
        environmentRepository: EnvironmentRepository,
    ): RestrictionManager = RestrictionManagerImpl(
        appStateManager = appStateManager,
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

    @Provides
    @Singleton
    fun provideFirstTimeActionManager(
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultDiskSource: VaultDiskSource,
        dispatcherManager: DispatcherManager,
        featureFlagManager: FeatureFlagManager,
        autofillEnabledManager: AutofillEnabledManager,
    ): FirstTimeActionManager = FirstTimeActionManagerImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        vaultDiskSource = vaultDiskSource,
        dispatcherManager = dispatcherManager,
        featureFlagManager = featureFlagManager,
        autofillEnabledManager = autofillEnabledManager,
    )

    @Provides
    @Singleton
    fun provideDatabaseSchemeManager(
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
    ): DatabaseSchemeManager = DatabaseSchemeManagerImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
    )

    @Provides
    @Singleton
    fun provideReviewPromptManager(
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        autofillEnabledManager: AutofillEnabledManager,
        accessibilityEnabledManager: AccessibilityEnabledManager,
    ): ReviewPromptManager = ReviewPromptManagerImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        autofillEnabledManager = autofillEnabledManager,
        accessibilityEnabledManager = accessibilityEnabledManager,
    )

    @Provides
    @Singleton
    fun provideSdkRepositoryFactory(
        vaultDiskSource: VaultDiskSource,
    ): SdkRepositoryFactory = SdkRepositoryFactoryImpl(
        vaultDiskSource = vaultDiskSource,
    )

    @Provides
    @Singleton
    fun provideKeyManager(
        @ApplicationContext context: Context,
        environmentRepository: EnvironmentRepository,
    ): CertificateManager = CertificateManagerImpl(
        context = context,
        environmentRepository = environmentRepository,
    )

    @Provides
    @Singleton
    fun provideAppResumeManager(
        settingsDiskSource: SettingsDiskSource,
        authDiskSource: AuthDiskSource,
        authRepository: AuthRepository,
        vaultLockManager: VaultLockManager,
        clock: Clock,
    ): AppResumeManager {
        return AppResumeManagerImpl(
            settingsDiskSource = settingsDiskSource,
            authDiskSource = authDiskSource,
            authRepository = authRepository,
            vaultLockManager = vaultLockManager,
            clock = clock,
        )
    }
}
