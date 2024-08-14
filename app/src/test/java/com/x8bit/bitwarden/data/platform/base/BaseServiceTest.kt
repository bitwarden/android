package com.x8bit.bitwarden.data.platform.base

import com.x8bit.bitwarden.data.platform.datasource.network.core.ResultCallAdapterFactory
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Base class for service tests. Provides common mock web server and retrofit setup.
 */
abstract class BaseServiceTest {

    protected val json = PlatformNetworkModule.providesJson()

    protected val server = MockWebServer().apply { start() }

    protected val url: HttpUrl = server.url("/")

    protected val urlPrefix: String get() = "http://${server.hostName}:${server.port}"

    protected val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(url.toString())
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @AfterEach
    fun after() {
        server.shutdown()
    }
}
