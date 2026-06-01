package com.x8bit.bitwarden.data.auth.repository.model

/**
 * The mechanism by which the user's vault may be unlocked.
 */
enum class VaultUnlockType {
    /**
     * The vault must be unlocked using a master password.
     */
    MASTER_PASSWORD,

    /**
     * The vault must be unlocked using a PIN.
     */
    PIN,
}
