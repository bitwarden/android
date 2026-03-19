package com.x8bit.bitwarden.data.billing.di

import android.content.Context
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManagerImpl
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.BillingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
}
