package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * The processed, relevant data from an autofill view node.
 */
sealed class AutofillView {
    /**
     * The [AutofillId] associated with this view.
     */
    abstract val autofillId: AutofillId

    /**
     * The package id for this view, if there is one. (ex: "com.x8bit.bitwarden")
     */
    abstract val idPackage: String?

    /**
     * Whether the view is currently focused.
     */
    abstract val isFocused: Boolean

    /**
     * The web domain for this view, if there is one. (ex: "m.facebook.com")
     */
    abstract val webDomain: String?

    /**
     * The web scheme for this view, if there is one. (ex: "https")
     */
    abstract val webScheme: String?

    /**
     * A view that corresponds to the card data partition for autofill fields.
     */
    sealed class Card : AutofillView() {

        /**
         * The expiration month [AutofillView] for the [Card] data partition.
         */
        data class ExpirationMonth(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Card()

        /**
         * The expiration year [AutofillView] for the [Card] data partition.
         */
        data class ExpirationYear(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Card()

        /**
         * The number [AutofillView] for the [Card] data partition.
         */
        data class Number(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Card()

        /**
         * The security code [AutofillView] for the [Card] data partition.
         */
        data class SecurityCode(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Card()
    }

    /**
     * A view that corresponds to the login data partition for autofill fields.
     */
    sealed class Login : AutofillView() {

        /**
         * The email address [AutofillView] for the [Login] data partition.
         */
        data class EmailAddress(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Login()

        /**
         * The password [AutofillView] for the [Login] data partition.
         */
        data class Password(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Login()

        /**
         * The username [AutofillView] for the [Login] data partition.
         */
        data class Username(
            override val autofillId: AutofillId,
            override val idPackage: String?,
            override val isFocused: Boolean,
            override val webDomain: String?,
            override val webScheme: String?,
        ) : Login()
    }
}
