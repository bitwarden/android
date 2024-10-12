package com.bitwarden.authenticatorbridge.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Models a serializable list of shared accounts to be shared with other applications.
 *
 * For domain level model, see [SharedAccountData].
 *
 * @param accounts The list of shared accounts.
 */
@Serializable
internal data class SharedAccountDataJson(
    @SerialName("accounts")
    val accounts: List<AccountJson>,
) {

    /**
     * Models a single shared account in a serializable format.
     *
     * @param userId user ID tied to the account.
     * @param name name associated with the account.
     * @param email email associated with the account.
     * @param environmentLabel environment associated with the account.
     * @param totpUris list of totp URIs associated with the account.
     * @param lastSyncTime the last time the account was synced by the main Bitwarden app.
     */
    @Serializable
    data class AccountJson(
        @SerialName("userId")
        val userId: String,

        @SerialName("name")
        val name: String?,

        @SerialName("email")
        val email: String,

        @SerialName("environmentLabel")
        val environmentLabel: String,

        @SerialName("totpUris")
        val totpUris: List<String>,
    )
}


