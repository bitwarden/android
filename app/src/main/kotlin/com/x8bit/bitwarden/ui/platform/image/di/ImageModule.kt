package com.x8bit.bitwarden.ui.platform.image.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.bitwarden.network.ssl.createMtlsOkHttpClient
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkCookieManager
import com.x8bit.bitwarden.ui.platform.image.ImageCookieInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides dependencies for loading images.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    /**
     * Provides the [ImageLoader] used by Coil, configured to use an OkHttpClient with (mutual TLS)
     * and cookie support.
     *
     * This ensures that all icon/image loading requests through Coil present the client
     * certificate for mutual TLS authentication, allowing them to pass through Cloudflare's mTLS
     * checks.
     *
     * The configuration mirrors the SSL setup used for non-image API calls.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        certificateManager: CertificateManager,
        networkCookieManager: NetworkCookieManager,
    ): ImageLoader = ImageLoader
        .Builder(context = context)
        .components {
            this.add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        certificateManager
                            .createMtlsOkHttpClient()
                            .newBuilder()
                            .addNetworkInterceptor(ImageCookieInterceptor(networkCookieManager))
                            .build()
                    },
                ),
            )
        }
        .build()
}
