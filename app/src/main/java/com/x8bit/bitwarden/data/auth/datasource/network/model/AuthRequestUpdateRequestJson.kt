package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for updating an auth request.
 */
@Serializable
data class AuthRequestUpdateRequestJson(
    @SerialName("key")
    val key: String,

    @SerialName("masterPasswordHash")
    val masterPasswordHash: String?,

    @SerialName("deviceIdentifier")
    val deviceId: String,

    @SerialName("requestApproved")
    val isApproved: Boolean,
)
