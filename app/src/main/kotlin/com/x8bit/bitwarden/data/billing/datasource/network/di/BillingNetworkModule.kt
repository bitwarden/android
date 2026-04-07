package com.x8bit.bitwarden.data.billing.datasource.network.di

import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.service.BillingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides network dependencies in the billing package.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingNetworkModule {

    @Provides
    @Singleton
    fun provideBillingService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): BillingService = bitwardenServiceClient.billingService
}
