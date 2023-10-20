package com.x8bit.bitwarden.data.auth.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.AuthRepositoryImpl
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Provides repositories in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun bindsAuthRepository(
        accountsService: AccountsService,
        identityService: IdentityService,
        authSdkSource: AuthSdkSource,
        authDiskSource: AuthDiskSource,
        authTokenInterceptor: AuthTokenInterceptor,
    ): AuthRepository = AuthRepositoryImpl(
        accountsService = accountsService,
        identityService = identityService,
        authSdkSource = authSdkSource,
        authDiskSource = authDiskSource,
        authTokenInterceptor = authTokenInterceptor,
        dispatcher = Dispatchers.IO,
    )
}
