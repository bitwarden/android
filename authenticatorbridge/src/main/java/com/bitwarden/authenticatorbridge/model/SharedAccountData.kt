package com.bitwarden.authenticatorbridge.model

import java.time.Instant

/**
 * Domain level model representing shared account data.
 *
 * @param accounts The list of shared accounts.
 */
data class SharedAccountData(
    val accounts: List<Account>,
) {

    /**
     * Models a single shared account.
     *
     * @param userId user ID tied to the account.
     * @param name name associated with the account.
     * @param email email associated with the account.
     * @param environmentLabel environment associated with the account.
     * @param totpUris list of totp URIs associated with the account.
     * @param lastSyncTime the last time the account was synced by the main Bitwarden app.
     */
    data class Account(
        val userId: String,
        val name: String?,
        val email: String,
        val environmentLabel: String,
        val totpUris: List<String>,
    )
}
