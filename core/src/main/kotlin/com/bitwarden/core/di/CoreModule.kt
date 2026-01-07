package com.bitwarden.core.di

import com.bitwarden.core.data.serializer.ZonedDateTimeSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides dependencies from the core module.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

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

        // Allow trailing commas in JSON objects and arrays.
        @OptIn(ExperimentalSerializationApi::class)
        allowTrailingComma = true
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
