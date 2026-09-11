package com.x8bit.bitwarden.data.autofill.model

/**
 * Autofill hints used to determine what data an input field is associated with, grouped by the
 * [AutofillView] partition they belong to.
 */
sealed interface AutofillHint {
    /**
     * Hints for the [AutofillView.Card] partition.
     */
    enum class Card : AutofillHint {
        BRAND,
        CARDHOLDER,
        EXPIRATION_DATE,
        EXPIRATION_MONTH,
        EXPIRATION_YEAR,
        NUMBER,
        SECURITY_CODE,
    }

    /**
     * Hints for the [AutofillView.Login] partition.
     */
    enum class Login : AutofillHint {
        PASSWORD,
        USERNAME,
    }

    /**
     * Hints for the [AutofillView.Identity] partition.
     */
    enum class Identity : AutofillHint {
        ADDRESS_COUNTRY,
        ADDRESS_EXTENDED,
        ADDRESS_LOCALITY,
        ADDRESS_REGION,
        ADDRESS_STREET,
        COMPANY,
        EMAIL,
        LICENSE_NUMBER,
        PASSPORT_NUMBER,
        PERSON_NAME_FAMILY,
        PERSON_NAME_FULL,
        PERSON_NAME_GIVEN,
        PERSON_NAME_MIDDLE,
        PERSON_NAME_PREFIX,
        POSTAL_ADDRESS_FULL,
        POSTAL_CODE,
        PHONE_FULL,
        SSN,
    }
}
