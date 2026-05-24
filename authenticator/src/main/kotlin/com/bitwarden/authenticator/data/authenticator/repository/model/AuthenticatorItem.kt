package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Represents all the information required to generate TOTP verification codes, including both
 * local codes and codes shared from the main Bitwarden app.
 *
 * @property cipherId The cipher ID.
 * @property source Distinguishes between local and shared items.
 * @property otpUri OTP URI.
 * @property issuer The issuer of the codes.
 * @property label The label of the item.
 */
data class AuthenticatorItem(
    val cipherId: String,
    val source: Source,
    val otpUri: String,
    val issuer: String?,
    val label: String?,
) {

    /**
     * Contains data about where the source of truth for a [AuthenticatorItem] is.
     */
    sealed class Source {

        /**
         * The item is from the local Authenticator app database.
         *
         * @property isFavorite Whether the user has marked the item as a favorite.
         */
        data class Local(
            val isFavorite: Boolean,
        ) : Source()

        /**
         * The item is shared from the main Bitwarden app.
         *
         * @property userId User ID from the main Bitwarden app. Used to group authenticator items
         * by account.
         * @property nameOfUser Username from the main Bitwarden app.
         * @property email Email of the user.
         * @property environmentLabel Label for the Bitwarden environment, like "bitwaren.com"
         */
        data class Shared(
            val userId: String,
            val nameOfUser: String?,
            val email: String,
            val environmentLabel: String,
        ) : Source()
    }
}
