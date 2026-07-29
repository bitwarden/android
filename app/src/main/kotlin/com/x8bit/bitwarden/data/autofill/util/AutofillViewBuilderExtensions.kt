package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * Builds an [AutofillView.Card] for the given card-related [autofillHint].
 */
internal fun AssistStructure.ViewNode.buildCardView(
    autofillOptions: List<String>,
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint.Card,
): AutofillView.Card = when (autofillHint) {
    AutofillHint.Card.EXPIRATION_MONTH -> {
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

    AutofillHint.Card.EXPIRATION_YEAR -> {
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

    AutofillHint.Card.EXPIRATION_DATE -> {
        AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
    }

    AutofillHint.Card.NUMBER -> {
        AutofillView.Card.Number(
            data = autofillViewData,
        )
    }

    AutofillHint.Card.SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
    }

    AutofillHint.Card.CARDHOLDER -> {
        AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
    }

    AutofillHint.Card.BRAND -> {
        val brandValue = this.autofillValue
            ?.extractCardBrandValue(
                autofillOptions = autofillOptions,
            )
        AutofillView.Card.Brand(
            data = autofillViewData,
            brandValue = brandValue,
        )
    }
}

/**
 * Builds an [AutofillView.Login] for the given login-related [autofillHint].
 */
internal fun buildLoginView(
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint.Login,
): AutofillView.Login = when (autofillHint) {
    AutofillHint.Login.PASSWORD -> {
        AutofillView.Login.Password(
            data = autofillViewData,
        )
    }

    AutofillHint.Login.USERNAME -> {
        AutofillView.Login.Username(
            data = autofillViewData,
        )
    }
}

/**
 * Builds an [AutofillView.Identity] for the given identity-related [autofillHint].
 */
internal fun buildIdentityView(
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint.Identity,
): AutofillView.Identity = when (autofillHint) {
    AutofillHint.Identity.PERSON_NAME_FULL -> {
        AutofillView.Identity.PersonNameFull(data = autofillViewData)
    }

    AutofillHint.Identity.PERSON_NAME_PREFIX -> {
        AutofillView.Identity.PersonNamePrefix(data = autofillViewData)
    }

    AutofillHint.Identity.PERSON_NAME_GIVEN -> {
        AutofillView.Identity.PersonNameGiven(data = autofillViewData)
    }

    AutofillHint.Identity.PERSON_NAME_MIDDLE -> {
        AutofillView.Identity.PersonNameMiddle(data = autofillViewData)
    }

    AutofillHint.Identity.PERSON_NAME_FAMILY -> {
        AutofillView.Identity.PersonNameFamily(data = autofillViewData)
    }

    AutofillHint.Identity.POSTAL_ADDRESS_FULL -> {
        AutofillView.Identity.PostalAddressFull(data = autofillViewData)
    }

    AutofillHint.Identity.ADDRESS_STREET -> {
        AutofillView.Identity.AddressStreet(data = autofillViewData)
    }

    AutofillHint.Identity.ADDRESS_EXTENDED -> {
        AutofillView.Identity.AddressExtended(data = autofillViewData)
    }
    AutofillHint.Identity.ADDRESS_LOCALITY -> {
        AutofillView.Identity.AddressLocality(data = autofillViewData)
    }

    AutofillHint.Identity.ADDRESS_REGION -> {
        AutofillView.Identity.AddressRegion(data = autofillViewData)
    }

    AutofillHint.Identity.ADDRESS_COUNTRY -> {
        AutofillView.Identity.AddressCountry(data = autofillViewData)
    }

    AutofillHint.Identity.POSTAL_CODE -> {
        AutofillView.Identity.PostalCode(data = autofillViewData)
    }

    AutofillHint.Identity.PHONE_FULL -> {
        AutofillView.Identity.PhoneFull(data = autofillViewData)
    }

    AutofillHint.Identity.COMPANY -> {
        AutofillView.Identity.Company(data = autofillViewData)
    }

    AutofillHint.Identity.EMAIL -> {
        // Produced by AutofillParserImpl's traverse(), not by this dispatch.
        AutofillView.Identity.Email(data = autofillViewData)
    }

    AutofillHint.Identity.SSN -> {
        AutofillView.Identity.Ssn(data = autofillViewData)
    }

    AutofillHint.Identity.PASSPORT_NUMBER -> {
        AutofillView.Identity.PassportNumber(data = autofillViewData)
    }

    AutofillHint.Identity.LICENSE_NUMBER -> {
        AutofillView.Identity.LicenseNumber(data = autofillViewData)
    }
}
