package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container for the user's API tokens.
 *
 * @property requestId The ID of the pending Auth Request.
 * @property requestPrivateKey The private key of the pending Auth Request.
 * @property requestAccessCode The access code of the pending Auth Request.
 * @property requestFingerprint The fingerprint of the pending Auth Request.
 */
@Serializable
data class PendingAuthRequestJson(
    @SerialName("id")
    val requestId: String,

    @SerialName("privateKey")
    val requestPrivateKey: String,

    @SerialName("accessCode")
    val requestAccessCode: String,

    @SerialName("fingerprint")
    val requestFingerprint: String,
)
