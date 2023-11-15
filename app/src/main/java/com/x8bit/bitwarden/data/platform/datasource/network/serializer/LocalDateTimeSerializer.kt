package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Used to serialize and deserialize [LocalDateTime].
 */
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val dateTimeFormatterDeserialization = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.[SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]'Z'")
    private val dateTimeFormatterSerialization =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'")
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName = "LocalDateTime", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        decoder.decodeString().let { dateString ->
            LocalDateTime.parse(dateString, dateTimeFormatterDeserialization)
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(dateTimeFormatterSerialization.format(value))
    }
}
