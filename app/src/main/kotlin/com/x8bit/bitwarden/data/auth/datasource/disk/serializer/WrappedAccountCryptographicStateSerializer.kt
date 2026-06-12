package com.x8bit.bitwarden.data.auth.datasource.disk.serializer

import com.bitwarden.core.WrappedAccountCryptographicState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom [KSerializer] for [WrappedAccountCryptographicState].
 *
 * Encodes the sealed class with a `"type"` discriminator field:
 * - `"v1"`: [WrappedAccountCryptographicState.V1] — wrapped private key only.
 * - `"v2"`: [WrappedAccountCryptographicState.V2] — wrapped private key, signing key, signed
 *   public key, and signed security state.
 */
internal class WrappedAccountCryptographicStateSerializer :
    KSerializer<WrappedAccountCryptographicState> {

    private val surrogateSerializer = Surrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): WrappedAccountCryptographicState =
        when (val surrogate = decoder.decodeSerializableValue(surrogateSerializer)) {
            is Surrogate.V1 -> {
                WrappedAccountCryptographicState.V1(privateKey = surrogate.privateKey)
            }

            is Surrogate.V2 -> {
                WrappedAccountCryptographicState.V2(
                    privateKey = surrogate.privateKey,
                    signingKey = surrogate.signingKey,
                    signedPublicKey = surrogate.signedPublicKey,
                    securityState = surrogate.securityState,
                )
            }
        }

    override fun serialize(encoder: Encoder, value: WrappedAccountCryptographicState) {
        val surrogate = when (value) {
            is WrappedAccountCryptographicState.V1 -> {
                Surrogate.V1(privateKey = value.privateKey)
            }

            is WrappedAccountCryptographicState.V2 -> {
                Surrogate.V2(
                    privateKey = value.privateKey,
                    signingKey = value.signingKey,
                    signedPublicKey = value.signedPublicKey,
                    securityState = value.securityState,
                )
            }
        }
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }
}

@Serializable
private sealed class Surrogate {
    @Serializable
    @SerialName("v1")
    data class V1(
        @SerialName("privateKey")
        val privateKey: String,
    ) : Surrogate()

    @Serializable
    @SerialName("v2")
    data class V2(
        @SerialName("privateKey")
        val privateKey: String,

        @SerialName("signingKey")
        val signingKey: String,

        @SerialName("signedPublicKey")
        val signedPublicKey: String?,

        @SerialName("securityState")
        val securityState: String,
    ) : Surrogate()
}
