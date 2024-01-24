package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for password hint.
 */
@Serializable
data class PasswordHintRequestJson(
    @SerialName("email")
    val email: String,
)
