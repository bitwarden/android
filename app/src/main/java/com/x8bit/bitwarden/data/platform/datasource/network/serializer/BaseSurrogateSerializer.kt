package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A helper class that simplifies the process of providing a "surrogate" [KSerializer]. These are
 * used to provide mappings between an "internal" type [R] (the "surrogate") to an external type
 * [T].
 *
 * See the [official surrogate documentation](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#composite-serializer-via-surrogate)
 * for details.
 */
abstract class BaseSurrogateSerializer<T, R> : KSerializer<T> {

    /**
     * The [KSerializer] naturally associated with the type [R], which is a typical class annotated
     * with [Serializable].
     */
    abstract val surrogateSerializer: KSerializer<R>

    /**
     * A conversion from the internal/surrogate type [R] to external type [T].
     */
    abstract fun R.toExternalType(): T

    /**
     * A conversion from the external type [T] to the internal/surrogate type [R].
     */
    abstract fun T.toSurrogateType(): R

    //region KSerializer overrides

    override val descriptor: SerialDescriptor
        get() = surrogateSerializer.descriptor

    final override fun deserialize(decoder: Decoder): T =
        decoder
            .decodeSerializableValue(surrogateSerializer)
            .toExternalType()

    final override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(
            surrogateSerializer,
            value.toSurrogateType(),
        )
    }

    //endregion KSerializer overrides
}
