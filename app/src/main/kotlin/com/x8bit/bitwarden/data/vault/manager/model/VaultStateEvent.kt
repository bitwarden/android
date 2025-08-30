package com.x8bit.bitwarden.data.vault.manager.model

/**
 * Represents an event that indicates if the vault for a particular user is not locked or unlocked.
 */
sealed class VaultStateEvent {
    /**
     * Indicates that the vault has been locked for the given [userId].
     */
    data class Locked(val userId: String) : VaultStateEvent()

    /**
     * Indicates that the vault has been unlocked for the given [userId].
     */
    data class Unlocked(val userId: String) : VaultStateEvent()
}
