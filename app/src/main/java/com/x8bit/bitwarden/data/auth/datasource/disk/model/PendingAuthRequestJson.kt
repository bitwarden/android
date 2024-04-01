package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container for the user's API tokens.
 *
 * @property requestId The ID of the pending Auth Request.
 * @property requestPrivateKey The private of the pending Auth Request.
 */
@Serializable
data class PendingAuthRequestJson(
    @SerialName("Id")
    val requestId: String,

    @SerialName("PrivateKey")
    val requestPrivateKey: String,
)
