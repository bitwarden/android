package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val KDF_TYPE_ARGON2_ID = 1
private const val KDF_TYPE_PBKDF2_SHA256 = 0

/**
 * Response body for pre login.
 */
@Serializable(PreLoginResponseSerializer::class)
data class PreLoginResponseJson(
    val kdfParams: KdfParams,
) {

    /**
     * Models different kdf types.
     *
     * See https://bitwarden.com/help/kdf-algorithms/.
     */
    sealed class KdfParams {

        /**
         * Models params for the Argon2id algorithm.
         */
        data class Argon2ID(
            val iterations: UInt,
            val memory: UInt,
            val parallelism: UInt,
        ) : KdfParams()

        /**
         * Models params for the PBKDF2 algorithm.
         */
        data class Pbkdf2(val iterations: UInt) : KdfParams()
    }
}

private class PreLoginResponseSerializer : KSerializer<PreLoginResponseJson> {

    private val surrogateSerializer = InternalPreLoginResponseJson.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): PreLoginResponseJson {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        val kdfParams = when (surrogate.kdfType) {
            KDF_TYPE_PBKDF2_SHA256 -> {
                PreLoginResponseJson.KdfParams.Pbkdf2(
                    iterations = surrogate.kdfIterations,
                )
            }

            KDF_TYPE_ARGON2_ID -> {
                PreLoginResponseJson.KdfParams.Argon2ID(
                    iterations = surrogate.kdfIterations,
                    memory = surrogate.kdfMemory!!,
                    parallelism = surrogate.kdfParallelism!!,
                )
            }

            else -> throw IllegalStateException(
                "Unable to parse KDF params for unknown kdfType: ${surrogate.kdfType}",
            )
        }
        return PreLoginResponseJson(kdfParams = kdfParams)
    }

    override fun serialize(encoder: Encoder, value: PreLoginResponseJson) {
        val surrogate = when (val params = value.kdfParams) {
            is PreLoginResponseJson.KdfParams.Argon2ID -> {
                InternalPreLoginResponseJson(
                    kdfType = KDF_TYPE_ARGON2_ID,
                    kdfIterations = params.iterations,
                    kdfMemory = params.memory,
                    kdfParallelism = params.parallelism,
                )
            }

            is PreLoginResponseJson.KdfParams.Pbkdf2 -> {
                InternalPreLoginResponseJson(
                    kdfType = KDF_TYPE_PBKDF2_SHA256,
                    kdfIterations = params.iterations,
                )
            }
        }
        encoder.encodeSerializableValue(
            surrogateSerializer,
            surrogate,
        )
    }
}
