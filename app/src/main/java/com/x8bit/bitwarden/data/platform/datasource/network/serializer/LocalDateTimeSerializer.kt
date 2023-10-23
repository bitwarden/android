package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Used to serialize and deserialize [LocalDateTime].
 */
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val localDateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
    private val localDateTimeFormatterNanoSeconds =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'")
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName = "LocalDateTime", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        decoder.decodeString().let { dateString ->
            try {
                LocalDateTime
                    .parse(dateString, localDateTimeFormatter)
            } catch (exception: DateTimeParseException) {
                LocalDateTime
                    .parse(dateString, localDateTimeFormatterNanoSeconds)
            }
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(localDateTimeFormatter.format(value))
    }
}
