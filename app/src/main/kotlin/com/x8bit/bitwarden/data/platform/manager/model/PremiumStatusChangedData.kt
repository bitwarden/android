package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Data class representing a Premium status changed push notification.
 *
 * @property userId The user ID associated with the status change.
 * @property isPremium Whether Premium is now enabled.
 */
data class PremiumStatusChangedData(
    val userId: String,
    val isPremium: Boolean,
)
