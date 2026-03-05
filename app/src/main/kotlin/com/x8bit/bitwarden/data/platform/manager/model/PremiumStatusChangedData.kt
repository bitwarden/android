package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Data class representing a premium status changed push notification.
 *
 * @property userId The user ID associated with the status change.
 * @property premium Whether premium is now enabled.
 */
data class PremiumStatusChangedData(
    val userId: String,
    val premium: Boolean,
)
