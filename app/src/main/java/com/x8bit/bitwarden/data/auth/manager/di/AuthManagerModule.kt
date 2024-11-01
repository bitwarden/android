package com.x8bit.bitwarden.data.auth.manager.di

import android.content.Context
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.AuthRequestsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.datasource.network.service.NewAuthRequestService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManager
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManagerImpl
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManagerImpl
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManager
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManagerImpl
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManagerImpl
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManagerImpl
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManagerImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
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
        @ApplicationContext context: Context,
        authDiskSource: AuthDiskSource,
        generatorDiskSource: GeneratorDiskSource,
        passwordHistoryDiskSource: PasswordHistoryDiskSource,
        pushDiskSource: PushDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
    ): UserLogoutManager =
        UserLogoutManagerImpl(
            context = context,
            authDiskSource = authDiskSource,
            generatorDiskSource = generatorDiskSource,
            passwordHistoryDiskSource = passwordHistoryDiskSource,
            pushDiskSource = pushDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultDiskSource = vaultDiskSource,
            vaultSdkSource = vaultSdkSource,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun providesAddTotpItemFromAuthenticatorManager(): AddTotpItemFromAuthenticatorManager =
        AddTotpItemFromAuthenticatorManagerImpl()
}
