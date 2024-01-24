package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body from the SSO prevalidate request.
 */
@Serializable
data class PrevalidateSsoResponseJson(
    @SerialName("token") val token: String?,
)
