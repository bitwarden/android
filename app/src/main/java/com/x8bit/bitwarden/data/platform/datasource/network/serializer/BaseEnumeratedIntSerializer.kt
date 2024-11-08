package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base [KSerializer] for mapping an [Enum] with possible values given by [values] to/from integer
 * values, which should be specified using [SerialName]. If a [default] value is provided, it will
 * be used when an unknown value is provided.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEnumeratedIntSerializer<T : Enum<T>>(
    private val values: Array<T>,
    private val default: T? = null,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            serialName = this::class.java.simpleName,
            kind = PrimitiveKind.INT,
        )

    override fun deserialize(decoder: Decoder): T {
        val decodedValue = decoder.decodeInt().toString()
        return values.firstOrNull { it.serialNameAnnotation?.value == decodedValue }
            ?: default
            ?: throw IllegalArgumentException("Unknown value $decodedValue")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(
            requireNotNull(
                value.serialNameAnnotation?.value?.toInt(),
            ),
        )
    }

    private val Enum<*>.serialNameAnnotation: SerialName?
        get() = javaClass.getDeclaredField(name).getAnnotation(SerialName::class.java)
}
