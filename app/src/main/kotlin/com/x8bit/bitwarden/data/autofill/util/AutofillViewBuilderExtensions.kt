package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * Builds an [AutofillView.Card] for the given card-related [autofillHint], or null if
 * [autofillHint] does not belong to the card category (never happens given
 * [AssistStructure.ViewNode.toAutofillView]'s exhaustive routing, but kept nullable rather than
 * throwing since this is the data layer).
 */
internal fun AssistStructure.ViewNode.buildCardView(
    autofillOptions: List<String>,
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint,
): AutofillView.Card? = when (autofillHint) {
    AutofillHint.CARD_EXPIRATION_MONTH -> {
        val monthValue = this
            .autofillValue
            ?.extractMonthValue(
                autofillOptions = autofillOptions,
            )

        AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = monthValue,
        )
    }

    AutofillHint.CARD_EXPIRATION_YEAR -> {
        val yearValue = this
            .autofillValue
            ?.extractYearValue(
                autofillOptions = autofillOptions,
            )

        AutofillView.Card.ExpirationYear(
            data = autofillViewData,
            yearValue = yearValue,
        )
    }

    AutofillHint.CARD_EXPIRATION_DATE -> {
        AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_NUMBER -> {
        AutofillView.Card.Number(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_CARDHOLDER -> {
        AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_BRAND -> {
        val brandValue = this.autofillValue
            ?.extractCardBrandValue(
                autofillOptions = autofillOptions,
            )
        AutofillView.Card.Brand(
            data = autofillViewData,
            brandValue = brandValue,
        )
    }

    else -> null
}

/**
 * Builds an [AutofillView.Login] for the given login-related [autofillHint], or null if
 * [autofillHint] does not belong to the login category (never happens given
 * [AssistStructure.ViewNode.toAutofillView]'s exhaustive routing, but kept nullable rather than
 * throwing since this is the data layer).
 */
internal fun buildLoginView(
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint,
): AutofillView.Login? = when (autofillHint) {
    AutofillHint.PASSWORD -> {
        AutofillView.Login.Password(
            data = autofillViewData,
        )
    }

    AutofillHint.USERNAME -> {
        AutofillView.Login.Username(
            data = autofillViewData,
        )
    }

    else -> null
}

/**
 * Builds an [AutofillView.Identity] for the given identity-related [autofillHint], or null if
 * [autofillHint] does not belong to the identity category (never happens given
 * [AssistStructure.ViewNode.toAutofillView]'s exhaustive routing, but kept nullable rather than
 * throwing since this is the data layer).
 */
internal fun buildIdentityView(
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint,
): AutofillView.Identity? = when (autofillHint) {
    AutofillHint.IDENTITY_PERSON_NAME_FULL -> {
        AutofillView.Identity.PersonNameFull(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PERSON_NAME_PREFIX -> {
        AutofillView.Identity.PersonNamePrefix(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PERSON_NAME_GIVEN -> {
        AutofillView.Identity.PersonNameGiven(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PERSON_NAME_MIDDLE -> {
        AutofillView.Identity.PersonNameMiddle(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PERSON_NAME_FAMILY -> {
        AutofillView.Identity.PersonNameFamily(data = autofillViewData)
    }

    AutofillHint.IDENTITY_POSTAL_ADDRESS_FULL -> {
        AutofillView.Identity.PostalAddressFull(data = autofillViewData)
    }

    AutofillHint.IDENTITY_ADDRESS_STREET -> {
        AutofillView.Identity.AddressStreet(data = autofillViewData)
    }

    AutofillHint.IDENTITY_ADDRESS_EXTENDED -> {
        AutofillView.Identity.AddressExtended(data = autofillViewData)
    }

    AutofillHint.IDENTITY_ADDRESS_LOCALITY -> {
        AutofillView.Identity.AddressLocality(data = autofillViewData)
    }

    AutofillHint.IDENTITY_ADDRESS_REGION -> {
        AutofillView.Identity.AddressRegion(data = autofillViewData)
    }

    AutofillHint.IDENTITY_ADDRESS_COUNTRY -> {
        AutofillView.Identity.AddressCountry(data = autofillViewData)
    }

    AutofillHint.IDENTITY_POSTAL_CODE -> {
        AutofillView.Identity.PostalCode(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PHONE_FULL -> {
        AutofillView.Identity.PhoneFull(data = autofillViewData)
    }

    AutofillHint.IDENTITY_COMPANY -> {
        AutofillView.Identity.Company(data = autofillViewData)
    }

    AutofillHint.IDENTITY_EMAIL -> {
        // Never actually produced via this dispatch — an identity email candidate is added
        // alongside the primary Login.Username view in AutofillParserImpl's traverse(), not
        // through this single-hint path. Handled here anyway to keep this `when` exhaustive and
        // correct if that ever changes.
        AutofillView.Identity.Email(data = autofillViewData)
    }

    AutofillHint.IDENTITY_SSN -> {
        AutofillView.Identity.Ssn(data = autofillViewData)
    }

    AutofillHint.IDENTITY_PASSPORT_NUMBER -> {
        AutofillView.Identity.PassportNumber(data = autofillViewData)
    }

    AutofillHint.IDENTITY_LICENSE_NUMBER -> {
        AutofillView.Identity.LicenseNumber(data = autofillViewData)
    }

    else -> null
}
