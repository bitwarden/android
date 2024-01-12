package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * The processed, relevant data from an autofill view node.
 */
sealed class AutofillView {

    /**
     * The data important to a given [AutofillView].
     *
     * @param autofillId The [AutofillId] associated with this view.
     * @param idPackage The package id for this view, if there is one.
     * @param isFocused Whether the view is currently focused.
     * @param webDomain The web domain for this view, if there is one. (example: m.facebook.com)
     * @param webScheme The web scheme for this view, if there is one. (example: https)
     */
    data class Data(
        val autofillId: AutofillId,
        val idPackage: String?,
        val isFocused: Boolean,
        val webDomain: String?,
        val webScheme: String?,
    )

    /**
     * The core data that describes this [AutofillView].
     */
    abstract val data: Data

    /**
     * A view that corresponds to the card data partition for autofill fields.
     */
    sealed class Card : AutofillView() {

        /**
         * The expiration month [AutofillView] for the [Card] data partition.
         */
        data class ExpirationMonth(
            override val data: Data,
        ) : Card()

        /**
         * The expiration year [AutofillView] for the [Card] data partition.
         */
        data class ExpirationYear(
            override val data: Data,
        ) : Card()

        /**
         * The number [AutofillView] for the [Card] data partition.
         */
        data class Number(
            override val data: Data,
        ) : Card()

        /**
         * The security code [AutofillView] for the [Card] data partition.
         */
        data class SecurityCode(
            override val data: Data,
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
            override val data: Data,
        ) : Login()

        /**
         * The password [AutofillView] for the [Login] data partition.
         */
        data class Password(
            override val data: Data,
        ) : Login()

        /**
         * The username [AutofillView] for the [Login] data partition.
         */
        data class Username(
            override val data: Data,
        ) : Login()
    }
}
