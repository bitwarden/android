package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Required data for passwordless requests.
 *
 * @property loginRequestId The login request ID.
 * @property userId The user ID.
 */
data class PasswordlessRequestData(
    val loginRequestId: String,
    val userId: String,
)
