package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for pre login.
 */
@Serializable
data class PreLoginRequestJson(
    @SerialName("email")
    val email: String,
)
