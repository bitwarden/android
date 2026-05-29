package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of encryption used for an SSO user organization.
 */
@Serializable(with = MemberDecryptionTypeSerializer::class)
enum class MemberDecryptionType {
    /**
     * Decryption using the user's master password.
     */
    @SerialName("0")
    MASTER_PASSWORD,

    /**
     * Decryption via Key Connector.
     */
    @SerialName("1")
    KEY_CONNECTOR,

    /**
     * Decryption via Trusted Device Encryption.
     */
    @SerialName("2")
    TRUSTED_DEVICE_ENCRYPTION,
}

@Keep
private class MemberDecryptionTypeSerializer : BaseEnumeratedIntSerializer<MemberDecryptionType>(
    className = "MemberDecryptionType",
    values = MemberDecryptionType.entries.toTypedArray(),
)
