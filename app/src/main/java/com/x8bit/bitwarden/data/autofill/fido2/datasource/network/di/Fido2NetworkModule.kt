package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.di

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.create
import javax.inject.Singleton

/**
 * Provides network dependencies in the fido2 package.
 */
@Module
@InstallIn(SingletonComponent::class)
object Fido2NetworkModule {

    @Provides
    @Singleton
    fun provideDigitalAssetLinkService(
        retrofits: Retrofits,
    ): DigitalAssetLinkService =
        DigitalAssetLinkServiceImpl(
            digitalAssetLinkApi = retrofits
                .createStaticRetrofit()
                .create(),
        )
}
