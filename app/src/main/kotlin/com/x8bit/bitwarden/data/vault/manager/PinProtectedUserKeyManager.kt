package com.x8bit.bitwarden.data.vault.manager

/**
 * Manager class to manage the pin-protected user key.
 */
interface PinProtectedUserKeyManager {
    /**
     * Checks if the given [userId] has an associated encrypted PIN key but not a pin-protected
     * user key. This indicates a scenario in which a user has requested PIN unlocking but requires
     * master-password unlocking on app restart. This function may then be called after such an
     * unlock to derive a pin-protected user key and store it in memory for use for any subsequent
     * unlocks during this current app session.
     *
     * If the user's vault has not yet been unlocked, this call will do nothing.
     *
     * @param userId The ID of the user to check.
     */
    suspend fun deriveTemporaryPinProtectedUserKeyIfNecessary(userId: String)

    /**
     * Migrates the PIN-protected user key for the given user if needed.
     *
     * If an encrypted PIN exists and no PIN-protected user key envelope is present, enrolls the
     * PIN with the encrypted PIN and stores the resulting envelope.
     * Optionally marks the envelope as in-memory only if the PIN-protected user key is not present.
     *
     * @param userId The ID of the user for whom to migrate the PIN-protected user key.
     */
    suspend fun migratePinProtectedUserKeyIfNeeded(userId: String)
}
