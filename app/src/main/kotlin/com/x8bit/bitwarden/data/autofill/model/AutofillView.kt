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
     * @param website website associated with this view.
     */
    data class Data(
        val autofillId: AutofillId,
        val autofillOptions: List<String>,
        val autofillType: Int,
        val isFocused: Boolean,
        val textValue: String?,
        val hasPasswordTerms: Boolean,
        val website: String?,
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
         * The expiration year [AutofillView] for the [Card] data partition. This implementation
         * also has its own [yearValue] because it can be present in lists, in which case there
         * is specialized logic for determining its [yearValue]. The [Data.textValue] is very
         * likely going to be a very different value.
         */
        data class ExpirationYear(
            override val data: Data,
            val yearValue: String?,
        ) : Card()

        /**
         * The expiration date [AutofillView] for the [Card] data partition.
         */
        data class ExpirationDate(
            override val data: Data,
        ) : Card()

        /**
         * The cardholder name [AutofillView] for the [Card] data partition.
         */
        data class CardholderName(
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

        /**
         * The brand [AutofillView] for the [Card] data partition. This implementation also has its
         * own [brandValue] because it can be present in lists, in which case there is specialized
         * logic for determining its [brandValue]. The [Data.textValue] is very likely going to be
         * a very different value.
         */
        data class Brand(
            override val data: Data,
            val brandValue: String?,
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

        /**
         * The email [AutofillView] for the [Login] data partition. Filled only when the cipher's
         * username is a valid email address.
         */
        data class Email(
            override val data: Data,
        ) : Login()
    }

    /**
     * A view that corresponds to the identity data partition for autofill fields.
     */
    sealed class Identity : AutofillView() {

        /**
         * The full name [AutofillView] for the [Identity] data partition, used for forms with a
         * single combined name field.
         */
        data class PersonNameFull(
            override val data: Data,
        ) : Identity()

        /**
         * The name prefix (e.g. "Mr.", "Dr.") [AutofillView] for the [Identity] data partition.
         */
        data class PersonNamePrefix(
            override val data: Data,
        ) : Identity()

        /**
         * The given (first) name [AutofillView] for the [Identity] data partition.
         */
        data class PersonNameGiven(
            override val data: Data,
        ) : Identity()

        /**
         * The middle name [AutofillView] for the [Identity] data partition.
         */
        data class PersonNameMiddle(
            override val data: Data,
        ) : Identity()

        /**
         * The family (last) name [AutofillView] for the [Identity] data partition.
         */
        data class PersonNameFamily(
            override val data: Data,
        ) : Identity()

        /**
         * The full postal address [AutofillView] for the [Identity] data partition, used for forms
         * with a single combined address field.
         */
        data class PostalAddressFull(
            override val data: Data,
        ) : Identity()

        /**
         * The street address [AutofillView] for the [Identity] data partition.
         */
        data class AddressStreet(
            override val data: Data,
        ) : Identity()

        /**
         * The locality (city) [AutofillView] for the [Identity] data partition.
         */
        data class AddressLocality(
            override val data: Data,
        ) : Identity()

        /**
         * The region (state/province) [AutofillView] for the [Identity] data partition.
         */
        data class AddressRegion(
            override val data: Data,
        ) : Identity()

        /**
         * The country [AutofillView] for the [Identity] data partition.
         */
        data class AddressCountry(
            override val data: Data,
        ) : Identity()

        /**
         * The postal code [AutofillView] for the [Identity] data partition.
         */
        data class PostalCode(
            override val data: Data,
        ) : Identity()

        /**
         * The full phone number [AutofillView] for the [Identity] data partition.
         */
        data class PhoneFull(
            override val data: Data,
        ) : Identity()

        /**
         * The company [AutofillView] for the [Identity] data partition.
         */
        data class Company(
            override val data: Data,
        ) : Identity()

        /**
         * The email [AutofillView] for the [Identity] data partition. This is distinct from
         * [Login.Email] and may be offered alongside it when a field is ambiguous between the
         * login and identity partitions.
         */
        data class Email(
            override val data: Data,
        ) : Identity()

        /**
         * The social security number [AutofillView] for the [Identity] data partition.
         */
        data class Ssn(
            override val data: Data,
        ) : Identity()

        /**
         * The passport number [AutofillView] for the [Identity] data partition.
         */
        data class PassportNumber(
            override val data: Data,
        ) : Identity()

        /**
         * The license number [AutofillView] for the [Identity] data partition.
         */
        data class LicenseNumber(
            override val data: Data,
        ) : Identity()
    }

    /**
     * A view that is an input field but does not correspond to any known autofill field.
     */
    data class Unused(
        override val data: Data,
    ) : AutofillView()
}
