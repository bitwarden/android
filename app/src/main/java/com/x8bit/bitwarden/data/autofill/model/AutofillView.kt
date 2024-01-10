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
     * Whether the view is currently focused.
     */
    abstract val isFocused: Boolean

    /**
     * A view that corresponds to the card data partition for autofill fields.
     */
    sealed class Card : AutofillView() {

        /**
         * The expiration month [AutofillView] for the [Card] data partition.
         */
        data class ExpirationMonth(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
        ) : Card()

        /**
         * The expiration year [AutofillView] for the [Card] data partition.
         */
        data class ExpirationYear(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
        ) : Card()

        /**
         * The number [AutofillView] for the [Card] data partition.
         */
        data class Number(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
        ) : Card()

        /**
         * The security code [AutofillView] for the [Card] data partition.
         */
        data class SecurityCode(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
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
            override val isFocused: Boolean,
        ) : Login()

        /**
         * The password [AutofillView] for the [Login] data partition.
         */
        data class Password(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
        ) : Login()

        /**
         * The username [AutofillView] for the [Login] data partition.
         */
        data class Username(
            override val autofillId: AutofillId,
            override val isFocused: Boolean,
        ) : Login()
    }
}
