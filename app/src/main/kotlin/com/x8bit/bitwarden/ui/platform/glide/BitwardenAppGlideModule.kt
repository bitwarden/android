package com.x8bit.bitwarden.ui.platform.glide

import android.content.Context
import com.bitwarden.network.ssl.createMtlsOkHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.x8bit.bitwarden.data.platform.manager.CertificateManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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
@GlideModule
class BitwardenAppGlideModule : AppGlideModule() {

    /**
     * Entry point to access Hilt-provided dependencies from non-Hilt managed classes.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BitwardenGlideEntryPoint {
        /**
         * Provides access to [CertificateManager] for mTLS certificate management.
         */
        fun certificateManager(): CertificateManager
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Get CertificateManager from Hilt
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BitwardenGlideEntryPoint::class.java,
        )
        val certificateManager = entryPoint.certificateManager()

        val okHttpClient = certificateManager.createMtlsOkHttpClient()

        // Register custom ModelLoader that uses our mTLS OkHttpClient
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpModelLoaderFactory(okHttpClient),
        )
    }

    /**
     * Custom ModelLoaderFactory for Glide 5.x that uses our mTLS-configured OkHttpClient.
     */
    private class OkHttpModelLoaderFactory(
        private val client: OkHttpClient,
    ) : ModelLoaderFactory<GlideUrl, InputStream> {

        override fun build(
            multiFactory: MultiModelLoaderFactory,
        ): ModelLoader<GlideUrl, InputStream> = OkHttpModelLoader(client)

        override fun teardown() {
            // No-op
        }
    }

    /**
     * Custom ModelLoader that uses OkHttpClient to load images.
     */
    private class OkHttpModelLoader(
        private val client: OkHttpClient,
    ) : ModelLoader<GlideUrl, InputStream> {

        override fun buildLoadData(
            model: GlideUrl,
            width: Int,
            height: Int,
            options: Options,
        ): ModelLoader.LoadData<InputStream> {
            return ModelLoader.LoadData(model, OkHttpDataFetcher(client, model))
        }

        override fun handles(model: GlideUrl): Boolean = true
    }

    /**
     * DataFetcher that uses OkHttpClient to execute HTTP requests.
     */
    private class OkHttpDataFetcher(
        private val client: OkHttpClient,
        private val url: GlideUrl,
    ) : DataFetcher<InputStream> {

        private var call: Call? = null

        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in InputStream>,
        ) {
            val request = Request.Builder()
                .url(url.toStringUrl())
                .build()

            call = client.newCall(request)

            val localCall = client.newCall(request).also { call = it }

            val response = try {
                localCall.execute()
            } catch (e: IOException) {
                callback.onLoadFailed(e)
                return
            }

            if (response.isSuccessful) {
                callback.onDataReady(response.body.byteStream())
            } else {
                callback.onLoadFailed(HttpException(response.message, response.code))
            }
        }

        override fun cleanup() {
            // Response body cleanup is handled by Glide
        }

        override fun cancel() {
            call?.cancel()
        }

        override fun getDataClass(): Class<InputStream> = InputStream::class.java

        override fun getDataSource(): DataSource = DataSource.REMOTE
    }
}
