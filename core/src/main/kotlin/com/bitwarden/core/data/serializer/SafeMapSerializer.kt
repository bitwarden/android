package com.bitwarden.core.data.serializer

import com.bitwarden.core.data.util.decodeFromJsonElementOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * A [KSerializer] for parsing a map of items and allows for the individual items in the object
 * to fail when deserialized without returning an error. If any number of items fail to be parsed,
 * they are removed from the map.
 */
class SafeMapSerializer<K, V>(
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
) : KSerializer<Map<K, V>> {
    private val innerSerializer: KSerializer<Map<K, V>> = MapSerializer(
        keySerializer = keySerializer,
        valueSerializer = valueSerializer,
    )
    override val descriptor: SerialDescriptor = innerSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Map<K, V>) {
        innerSerializer.serialize(encoder, value)
    }

    override fun deserialize(
        decoder: Decoder,
    ): Map<K, V> = with(decoder as JsonDecoder) {
        decodeJsonElement()
            .jsonObject
            .mapNotNull { (keyString, valueElement) ->
                val key = json
                    .decodeFromJsonElementOrNull(
                        deserializer = keySerializer,
                        element = JsonPrimitive(value = keyString),
                    )
                    ?: return@mapNotNull null
                val value = json
                    .decodeFromJsonElementOrNull(
                        deserializer = valueSerializer,
                        element = valueElement,
                    )
                    ?: return@mapNotNull null
                key to value
            }
            .toMap()
    }
}
