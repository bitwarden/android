package com.bitwarden.authenticator.data.platform.datasource.network.di

import com.bitwarden.authenticator.data.platform.datasource.network.serializer.ZonedDateTimeSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
object PlatformNetworkModule {
    @Provides
    @Singleton
    fun providesJson(): Json = Json {

        // If there are keys returned by the server not modeled by a serializable class,
        // ignore them.
        // This makes additive server changes non-breaking.
        ignoreUnknownKeys = true

        // We allow for nullable values to have keys missing in the JSON response.
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(ZonedDateTimeSerializer())
        }

        // Respect model default property values.
        coerceInputValues = true
    }
}
