package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Result class to share the [loggedOutUserId] of a user
 * that was successfully logged out.
 */
data class LogoutEvent(
    val loggedOutUserId: String,
)
