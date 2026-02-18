package com.bitwarden.core.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Used to serialize and deserialize [Instant].
 */
class InstantSerializer : KSerializer<Instant> {
    private val dateTimeFormatterDeserialization = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.][:][SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]XXX")

    private val dateTimeFormatterSerialization: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        .withZone(ZoneOffset.UTC)

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName = "Instant", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        ZonedDateTime
            .parse(decoder.decodeString(), dateTimeFormatterDeserialization)
            .toInstant()

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(dateTimeFormatterSerialization.format(value))
    }
}
