package com.bitwarden.authenticatorbridge.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a serializable list of shared accounts to be shared with other applications.
 *
 * For domain level model, see [SharedAccountData].
 *
 * @property accounts The list of shared accounts.
 */
@Serializable
internal data class SharedAccountDataJson(
    @SerialName("accounts")
    val accounts: List<AccountJson>,
) {

    /**
     * Models a single shared account in a serializable format.
     *
     * @property userId user ID tied to the account.
     * @property name name associated with the account.
     * @property email email associated with the account.
     * @property environmentLabel environment associated with the account.
     * @property totpUris list of totp URIs associated with the account. This is for legacy use
     * only.
     * @property cipherData list of ciphers containing totp URIs associated with the account.
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

        // TODO: PM-34085 Remove totpUris.
        @SerialName("totpUris")
        val totpUris: List<String>,

        // TODO: PM-34085 Make cipherData nonnull.
        @SerialName("cipherData")
        val cipherData: List<CipherJson>?,
    )

    /**
     * Models a single shared cipher in a serializable format.
     *
     * @property uri the totp URI associated with this cipher.
     * @property id the ID of this cipher.
     * @property name the name of this cipher.
     * @property username the username for this cipher.
     * @property isFavorite indicates if this cipher is favorited.
     */
    @Serializable
    data class CipherJson(
        @SerialName("uri")
        val uri: String,

        @SerialName("id")
        val id: String,

        @SerialName("cipherName")
        val name: String,

        @SerialName("username")
        val username: String?,

        @SerialName("isFavorite")
        val isFavorite: Boolean,
    )
}
