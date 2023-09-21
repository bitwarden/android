package com.x8bit.bitwarden.data.auth.datasource.network.di

import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides network dependencies in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesAccountService(
        @Named(NetworkModule.UNAUTHORIZED) retrofit: Retrofit,
    ): AccountsService = AccountsServiceImpl(retrofit.create())

    @Provides
    @Singleton
    fun providesIdentityService(
        @Named(NetworkModule.UNAUTHORIZED) retrofit: Retrofit,
        json: Json,
    ): IdentityService = IdentityServiceImpl(retrofit.create(), json)
}
