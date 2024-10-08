package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Represents all the information required to generate TOTP verification codes, including both
 * local codes and codes shared from the main Bitwarden app.
 *
 * @param source Distinguishes between local and shared items.
 * @param otpUri OTP URI.
 * @param issuer The issuer of the codes.
 * @param label The label of the item.
 */
data class AuthenticatorItem(
    val source: Source,
    val otpUri: String,
    val issuer: String,
    val label: String?,
) {

    /**
     * Contains data about where the source of truth for a [AuthenticatorItem] is.
     */
    sealed class Source {

        /**
         * The item is from the local Authenticator app database.
         *
         * @param cipherId Local cipher ID.
         * @param isFavorite Whether or not the user has marked the item as a favorite.
         */
        data class Local(
            val cipherId: String,
            val isFavorite: Boolean,
        ) : Source()

        /**
         * The item is shared from the main Bitwarden app.
         *
         * @param userId User ID from the main Bitwarden app. Used to group authenticator items
         * by account.
         * @param nameOfUser Username from the main Bitwarden app.
         * @param email Email of the user.
         * @param environmentLabel Label for the Bitwarden environment, like "bitwaren.com"
         */
        data class Shared(
            val userId: String,
            val nameOfUser: String?,
            val email: String,
            val environmentLabel: String,
        ) : Source()
    }
}
