package com.bitwarden.core.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal

/**
 * Used to serialize and deserialize [BigDecimal] as a JSON number literal without
 * round-tripping through [Double]. Preserving the raw numeric string guarantees no
 * precision loss for currency values.
 */
class BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "BigDecimal", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException(
                "BigDecimalSerializer only supports JSON formats.",
            )
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException(
                "Expected a JSON number literal for BigDecimal.",
            )
        return primitive.content.toBigDecimal()
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException(
                "BigDecimalSerializer only supports JSON formats.",
            )
        jsonEncoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
}
