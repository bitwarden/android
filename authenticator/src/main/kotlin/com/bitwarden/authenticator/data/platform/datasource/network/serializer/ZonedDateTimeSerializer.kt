package com.bitwarden.authenticator.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Used to serialize and deserialize [ZonedDateTime].
 */
class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    private val dateTimeFormatterDeserialization = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.][:][SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]X")

    private val dateTimeFormatterSerialization =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName = "ZonedDateTime", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        decoder.decodeString().let { dateString ->
            ZonedDateTime.parse(dateString, dateTimeFormatterDeserialization)
        }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(dateTimeFormatterSerialization.format(value))
    }
}
