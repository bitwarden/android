package com.x8bit.bitwarden.data.pam.manager.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.pam.manager.PamAlertNotificationFactory
import com.x8bit.bitwarden.data.pam.manager.PamAlertNotificationFactoryImpl
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManager
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManagerImpl
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManager
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManagerImpl
import com.x8bit.bitwarden.data.pam.manager.PamNotificationManager
import com.x8bit.bitwarden.data.pam.manager.PamNotificationManagerImpl
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the PAM package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PamManagerModule {

    @Provides
    @Singleton
    fun providesPamLeaseReminderManager(
        @ApplicationContext context: Context,
        clock: Clock,
        authDiskSource: AuthDiskSource,
        featureFlagManager: FeatureFlagManager,
        dispatcherManager: DispatcherManager,
    ): PamLeaseReminderManager = PamLeaseReminderManagerImpl(
        workManager = WorkManager.getInstance(context),
        clock = clock,
        authDiskSource = authDiskSource,
        featureFlagManager = featureFlagManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun providesPamAlertNotificationFactory(
        @ApplicationContext context: Context,
    ): PamAlertNotificationFactory = PamAlertNotificationFactoryImpl(context = context)

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun providesPamNotificationManager(
        @ApplicationContext context: Context,
        notificationFactory: PamAlertNotificationFactory,
        featureFlagManager: FeatureFlagManager,
        pamRepository: PamRepository,
        pushManager: PushManager,
        dispatcherManager: DispatcherManager,
    ): PamNotificationManager = PamNotificationManagerImpl(
        notificationManager = NotificationManagerCompat.from(context),
        notificationFactory = notificationFactory,
        featureFlagManager = featureFlagManager,
        pamRepository = pamRepository,
        pushManager = pushManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun providesPamApprovalManager(
        authRepository: AuthRepository,
        vaultSyncManager: VaultSyncManager,
        featureFlagManager: FeatureFlagManager,
        pamRepository: PamRepository,
        pushManager: PushManager,
        dispatcherManager: DispatcherManager,
    ): PamApprovalManager = PamApprovalManagerImpl(
        authRepository = authRepository,
        vaultSyncManager = vaultSyncManager,
        featureFlagManager = featureFlagManager,
        pamRepository = pamRepository,
        pushManager = pushManager,
        dispatcherManager = dispatcherManager,
    )
}
