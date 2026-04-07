package com.x8bit.bitwarden.ui.platform.glide

import android.content.Context
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.network.ssl.createMtlsOkHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkCookieManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.InputStream

/**
 * Custom Glide module for the Bitwarden app that configures Glide to use an OkHttpClient
 * with mTLS (mutual TLS) support.
 *
 * This ensures that all icon/image loading requests through Glide present the client certificate
 * for mutual TLS authentication, allowing them to pass through Cloudflare's mTLS checks.
 *
 * The configuration mirrors the SSL setup used in RetrofitsImpl for API calls.
 */
@OmitFromCoverage
@GlideModule
class BitwardenAppGlideModule : AppGlideModule() {

    /**
     * Entry point to access Hilt-provided dependencies from non-Hilt managed classes.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BitwardenGlideEntryPoint {
        /**
         * Provides access to the [CertificateManager] for mTLS certificate management.
         */
        fun certificateManager(): CertificateManager

        /**
         * Provides access to the [NetworkCookieManager] for cookie-based authentication.
         */
        fun networkCookieManager(): NetworkCookieManager
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Get CertificateManager from Hilt
        val entryPoint = EntryPointAccessors.fromApplication(
            context = context.applicationContext,
            entryPoint = BitwardenGlideEntryPoint::class.java,
        )
        val certificateManager = entryPoint.certificateManager()
        val networkCookieManager = entryPoint.networkCookieManager()

        // Build OkHttpClient with mTLS and cookie support
        val client = certificateManager
            .createMtlsOkHttpClient()
            .newBuilder()
            .addNetworkInterceptor(GlideCookieInterceptor(networkCookieManager))
            .build()

        // Register OkHttpUrlLoader that uses our mTLS + cookie OkHttpClient
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client),
        )
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
