package com.x8bit.bitwarden.data.pam.manager.di

import android.content.Context
import androidx.work.WorkManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManager
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManagerImpl
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
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
}
