package com.bitwarden.bridge.util

import com.bitwarden.bridge.BuildConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.Instant

/**
 * Version of the native bridge sdk.
 */
const val NATIVE_BRIDGE_SDK_VERSION = BuildConfig.VERSION

/**
 * Common instance of [Json] that should be used throughout the app.
 */
internal val JSON = Json {
    // If there are keys returned by the server not modeled by a serializable class,
    // ignore them.
    // This makes additive server changes non-breaking.
    ignoreUnknownKeys = true

    // We allow for nullable values to have keys missing in the JSON response.
    explicitNulls = false

    // Add serializer for Instant serialization.
    serializersModule = SerializersModule {
        contextual(InstantSerializer)
    }

    // Respect model default property values.
    coerceInputValues = true
}

/**
 * A simple serializer for serializing [Instant].
 */
private object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
