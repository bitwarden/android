package com.x8bit.bitwarden.data.auth.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.AuthRequestsService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.NewAuthRequestService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManager
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManagerImpl
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManagerImpl
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManager
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManagerImpl
import com.x8bit.bitwarden.data.auth.manager.AuthTokenManager
import com.x8bit.bitwarden.data.auth.manager.AuthTokenManagerImpl
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.KdfManagerImpl
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManagerImpl
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManagerImpl
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManagerImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.CredentialExchangeRegistryManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthManagerModule {

    @Provides
    @Singleton
    fun provideAuthRequestNotificationManager(
        @ApplicationContext context: Context,
        authDiskSource: AuthDiskSource,
        pushManager: PushManager,
        dispatcherManager: DispatcherManager,
    ): AuthRequestNotificationManager =
        AuthRequestNotificationManagerImpl(
            context = context,
            authDiskSource = authDiskSource,
            pushManager = pushManager,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideAuthRequestManager(
        clock: Clock,
        authRequestsService: AuthRequestsService,
        newAuthRequestService: NewAuthRequestService,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
    ): AuthRequestManager =
        AuthRequestManagerImpl(
            clock = clock,
            authRequestsService = authRequestsService,
            newAuthRequestService = newAuthRequestService,
            authSdkSource = authSdkSource,
            vaultSdkSource = vaultSdkSource,
            authDiskSource = authDiskSource,
        )

    @Provides
    @Singleton
    fun provideKeyConnectorManager(
        accountsService: AccountsService,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
    ): KeyConnectorManager =
        KeyConnectorManagerImpl(
            accountsService = accountsService,
            authSdkSource = authSdkSource,
            vaultSdkSource = vaultSdkSource,
        )

    @Provides
    @Singleton
    fun provideTrustedDeviceManager(
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
        devicesService: DevicesService,
    ): TrustedDeviceManager =
        TrustedDeviceManagerImpl(
            authDiskSource = authDiskSource,
            vaultSdkSource = vaultSdkSource,
            devicesService = devicesService,
        )

    @Provides
    @Singleton
    fun provideUserLogoutManager(
        authDiskSource: AuthDiskSource,
        generatorDiskSource: GeneratorDiskSource,
        passwordHistoryDiskSource: PasswordHistoryDiskSource,
        pushDiskSource: PushDiskSource,
        settingsDiskSource: SettingsDiskSource,
        toastManager: ToastManager,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
        credentialExchangeRegistryManager: CredentialExchangeRegistryManager,
    ): UserLogoutManager =
        UserLogoutManagerImpl(
            authDiskSource = authDiskSource,
            generatorDiskSource = generatorDiskSource,
            passwordHistoryDiskSource = passwordHistoryDiskSource,
            pushDiskSource = pushDiskSource,
            settingsDiskSource = settingsDiskSource,
            toastManager = toastManager,
            vaultDiskSource = vaultDiskSource,
            vaultSdkSource = vaultSdkSource,
            dispatcherManager = dispatcherManager,
            credentialExchangeRegistryManager = credentialExchangeRegistryManager,
        )

    @Provides
    @Singleton
    fun providesAddTotpItemFromAuthenticatorManager(): AddTotpItemFromAuthenticatorManager =
        AddTotpItemFromAuthenticatorManagerImpl()

    @Provides
    @Singleton
    fun providesAuthTokenManager(
        authDiskSource: AuthDiskSource,
    ): AuthTokenManager = AuthTokenManagerImpl(authDiskSource = authDiskSource)

    @Provides
    @Singleton
    fun providesKdfManager(
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
        accountsService: AccountsService,
        featureFlagManager: FeatureFlagManager,
    ): KdfManager = KdfManagerImpl(
        authDiskSource = authDiskSource,
        vaultSdkSource = vaultSdkSource,
        accountsService = accountsService,
        featureFlagManager = featureFlagManager,
    )
}
