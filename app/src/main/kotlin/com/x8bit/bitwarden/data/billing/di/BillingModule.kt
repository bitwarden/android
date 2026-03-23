package com.x8bit.bitwarden.data.billing.di

import android.content.Context
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManagerImpl
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManager
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManagerImpl
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.BillingRepositoryImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides billing-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun providePlayBillingManager(
        @ApplicationContext context: Context,
        dispatcherManager: DispatcherManager,
    ): PlayBillingManager = PlayBillingManagerImpl(
        context = context,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideBillingRepository(
        playBillingManager: PlayBillingManager,
        billingService: BillingService,
    ): BillingRepository = BillingRepositoryImpl(
        playBillingManager = playBillingManager,
        billingService = billingService,
    )

    @Provides
    @Singleton
    fun providePremiumStateManager(
        authDiskSource: AuthDiskSource,
        authRepository: AuthRepository,
        billingRepository: BillingRepository,
        settingsDiskSource: SettingsDiskSource,
        vaultRepository: VaultRepository,
        featureFlagManager: FeatureFlagManager,
        clock: Clock,
        dispatcherManager: DispatcherManager,
    ): PremiumStateManager = PremiumStateManagerImpl(
        authDiskSource = authDiskSource,
        authRepository = authRepository,
        billingRepository = billingRepository,
        settingsDiskSource = settingsDiskSource,
        vaultRepository = vaultRepository,
        featureFlagManager = featureFlagManager,
        clock = clock,
        dispatcherManager = dispatcherManager,
    )
}
