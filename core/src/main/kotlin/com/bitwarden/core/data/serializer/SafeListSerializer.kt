package com.bitwarden.core.data.serializer

import com.bitwarden.core.data.util.decodeFromJsonElementOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray

/**
 * A [KSerializer] for parsing lists of items and allows for the individual items in the array
 * to fail when deserialize without returning an error. If any number of items fail to be parsed,
 * they are removed from the list.
 */
class SafeListSerializer<T>(
    private val innerSerializer: KSerializer<T>,
) : KSerializer<List<T>> {
    private val listSerializer: KSerializer<List<T>> = ListSerializer(innerSerializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: List<T>,
    ): Unit = listSerializer.serialize(encoder, value)

    override fun deserialize(
        decoder: Decoder,
    ): List<T> = with(decoder as JsonDecoder) {
        decodeJsonElement()
            .jsonArray
            .mapNotNull { json.decodeFromJsonElementOrNull(innerSerializer, it) }
    }
}
