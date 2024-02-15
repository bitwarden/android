package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Required data for notification logout operation.
 *
 * @property userId The ID of the user being logged out.
 */
data class NotificationLogoutData(
    val userId: String,
)
