package com.x8bit.bitwarden.data.billing.di

import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.BillingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideBillingRepository(
        billingService: BillingService,
    ): BillingRepository = BillingRepositoryImpl(
        billingService = billingService,
    )
}
