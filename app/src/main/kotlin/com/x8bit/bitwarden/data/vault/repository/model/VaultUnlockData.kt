package com.x8bit.bitwarden.data.vault.repository.model

/**
 * The vault state for a given user ID.
 *
 * @property userId The user ID.
 * @property status The lock status of the user's vault.
 */
data class VaultUnlockData(
    val userId: String,
    val status: Status,
) {
    /**
     * The lock status of a user's vault.
     */
    enum class Status {
        UNLOCKED,
        UNLOCKING,
    }
}
