package com.x8bit.bitwarden.data.auth.datasource.network.model

import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseSurrogateSerializer
import kotlinx.serialization.Serializable

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

private class PreLoginResponseSerializer :
    BaseSurrogateSerializer<PreLoginResponseJson, InternalPreLoginResponseJson>() {

    override val surrogateSerializer = InternalPreLoginResponseJson.serializer()

    override fun InternalPreLoginResponseJson.toExternalType(): PreLoginResponseJson =
        PreLoginResponseJson(
            kdfParams = when (this.kdfType) {
                KDF_TYPE_PBKDF2_SHA256 -> {
                    PreLoginResponseJson.KdfParams.Pbkdf2(
                        iterations = this.kdfIterations,
                    )
                }

                KDF_TYPE_ARGON2_ID -> {
                    @Suppress("UnsafeCallOnNullableType")
                    PreLoginResponseJson.KdfParams.Argon2ID(
                        iterations = this.kdfIterations,
                        memory = this.kdfMemory!!,
                        parallelism = this.kdfParallelism!!,
                    )
                }

                else -> throw IllegalStateException(
                    "Unable to parse KDF params for unknown kdfType: ${this.kdfType}",
                )
            },
        )

    override fun PreLoginResponseJson.toSurrogateType(): InternalPreLoginResponseJson =
        when (val params = this.kdfParams) {
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
}
