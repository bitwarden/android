package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Associates [isUsingKeyConnector] with the given [userId].
 */
data class UserKeyConnectorState(
    val userId: String,
    val isUsingKeyConnector: Boolean?,
)
