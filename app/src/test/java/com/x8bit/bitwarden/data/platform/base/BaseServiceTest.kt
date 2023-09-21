package com.x8bit.bitwarden.data.platform.base

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.x8bit.bitwarden.data.platform.datasource.network.core.ResultCallAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import retrofit2.Retrofit

/**
 * Base class for service tests. Provides common mock web server and retrofit setup.
 */
abstract class BaseServiceTest {

    protected val server = MockWebServer().apply { start() }

    protected val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    @After
    fun after() {
        server.shutdown()
    }
}
