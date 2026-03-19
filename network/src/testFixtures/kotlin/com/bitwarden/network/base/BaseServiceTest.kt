package com.bitwarden.network.base

import com.bitwarden.core.di.CoreModule
import com.bitwarden.network.core.NetworkResultCallAdapterFactory
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Base class for service tests. Provides common mock web server and retrofit setup.
 */
abstract class BaseServiceTest {

    protected val json = CoreModule.providesJson()

    protected val server = MockWebServer().apply { start() }

    protected val url: HttpUrl = server.url("/")

    protected val urlPrefix: String get() = "http://${server.hostName}:${server.port}"

    protected val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(url.toString())
        .addCallAdapterFactory(NetworkResultCallAdapterFactory())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @AfterEach
    fun after() {
        server.shutdown()
    }
}
