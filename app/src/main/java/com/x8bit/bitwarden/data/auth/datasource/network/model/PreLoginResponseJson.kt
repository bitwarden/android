package com.x8bit.bitwarden.data.auth.datasource.network.model

import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseSurrogateSerializer
import kotlinx.serialization.Serializable

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
         * The associated [KdfTypeJson].
         */
        abstract val kdfTypeJson: KdfTypeJson

        /**
         * Models params for the Argon2id algorithm.
         */
        data class Argon2ID(
            val iterations: UInt,
            val memory: UInt,
            val parallelism: UInt,
        ) : KdfParams() {
            override val kdfTypeJson: KdfTypeJson
                get() = KdfTypeJson.ARGON2_ID
        }

        /**
         * Models params for the PBKDF2 algorithm.
         */
        data class Pbkdf2(
            val iterations: UInt,
        ) : KdfParams() {
            override val kdfTypeJson: KdfTypeJson
                get() = KdfTypeJson.PBKDF2_SHA256
        }
    }
}

private class PreLoginResponseSerializer :
    BaseSurrogateSerializer<PreLoginResponseJson, InternalPreLoginResponseJson>() {

    override val surrogateSerializer = InternalPreLoginResponseJson.serializer()

    override fun InternalPreLoginResponseJson.toExternalType(): PreLoginResponseJson =
        PreLoginResponseJson(
            kdfParams = when (this.kdfType) {
                KdfTypeJson.PBKDF2_SHA256 -> {
                    PreLoginResponseJson.KdfParams.Pbkdf2(
                        iterations = this.kdfIterations,
                    )
                }

                KdfTypeJson.ARGON2_ID -> {
                    @Suppress("UnsafeCallOnNullableType")
                    PreLoginResponseJson.KdfParams.Argon2ID(
                        iterations = this.kdfIterations,
                        memory = this.kdfMemory!!,
                        parallelism = this.kdfParallelism!!,
                    )
                }
            },
        )

    override fun PreLoginResponseJson.toSurrogateType(): InternalPreLoginResponseJson =
        when (val params = this.kdfParams) {
            is PreLoginResponseJson.KdfParams.Argon2ID -> {
                InternalPreLoginResponseJson(
                    kdfType = params.kdfTypeJson,
                    kdfIterations = params.iterations,
                    kdfMemory = params.memory,
                    kdfParallelism = params.parallelism,
                )
            }

            is PreLoginResponseJson.KdfParams.Pbkdf2 -> {
                InternalPreLoginResponseJson(
                    kdfType = params.kdfTypeJson,
                    kdfIterations = params.iterations,
                )
            }
        }
}
