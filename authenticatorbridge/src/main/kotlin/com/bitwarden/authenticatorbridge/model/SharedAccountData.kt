package com.bitwarden.authenticatorbridge.model

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
     * @param cipherData list of ciphers containing totp URIs associated with the account.
     */
    data class Account(
        val userId: String,
        val name: String?,
        val email: String,
        val environmentLabel: String,
        val cipherData: List<CipherData>,
    )

    /**
     * Models a single shared cipher containing a totp.
     *
     * @param uri the totp URI.
     * @param legacyUri the legacy totp URI.
     * @param id unique ID for this item.
     * @param name the name of the cipher.
     * @param username the username of the item.
     * @param isFavorite indicates that this item is a favorite.
     */
    data class CipherData constructor(
        val uri: String,
        // TODO: PM-34085 Remove the legacyUri.
        val legacyUri: String?,
        val id: String,
        val name: String,
        val username: String?,
        val isFavorite: Boolean,
    )
}
