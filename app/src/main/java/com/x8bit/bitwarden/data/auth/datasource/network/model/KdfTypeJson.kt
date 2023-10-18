package com.x8bit.bitwarden.data.auth.datasource.network.model

import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different key derivation functions (KDFs).
 */
@Serializable(KdfTypeSerializer::class)
enum class KdfTypeJson {
    @SerialName("1")
    ARGON2_ID,

    @SerialName("0")
    PBKDF2_SHA256,
}

private class KdfTypeSerializer :
    BaseEnumeratedIntSerializer<KdfTypeJson>(KdfTypeJson.values())
