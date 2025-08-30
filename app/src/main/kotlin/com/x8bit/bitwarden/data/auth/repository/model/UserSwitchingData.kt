package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Contains the values of the previous and new active user IDs when switching active users.
 */
data class UserSwitchingData(
    val previousActiveUserId: String?,
    val currentActiveUserId: String?,
)
