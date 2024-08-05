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
     * @param autofillOptions A list of autofill options that can be used to fill this view.
     * @param autofillType The autofill field type. (ex: View.AUTOFILL_TYPE_TEXT)
     * @param isFocused Whether the view is currently focused.
     * @param textValue A text value that represents the input present in the field.
     * @param hasPasswordTerms Indicates that the field includes password terms.
     */
    data class Data(
        val autofillId: AutofillId,
        val autofillOptions: List<String>,
        val autofillType: Int,
        val isFocused: Boolean,
        val textValue: String?,
        val hasPasswordTerms: Boolean,
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
         * The expiration month [AutofillView] for the [Card] data partition. This implementation
         * also has its own [monthValue] because it can be present in lists, in which case there
         * is specialized logic for determining its [monthValue]. The [Data.textValue] is very
         * likely going to be a very different value.
         */
        data class ExpirationMonth(
            override val data: Data,
            val monthValue: String?,
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

    /**
     * A view that is an input field but does not correspond to any known autofill field.
     */
    data class Unused(
        override val data: Data,
    ) : AutofillView()
}
