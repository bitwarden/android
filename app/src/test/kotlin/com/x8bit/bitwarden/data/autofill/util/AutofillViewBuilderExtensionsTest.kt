package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillViewBuilderExtensionsTest {

    private val viewNode: AssistStructure.ViewNode = mockk {
        every { autofillValue } returns null
    }

    @Test
    fun `buildCardView should map every card hint to its Card view`() {
        val data = autofillViewData()
        val expectedByHint = mapOf(
            AutofillHint.Card.BRAND to AutofillView.Card.Brand(data = data, brandValue = null),
            AutofillHint.Card.CARDHOLDER to AutofillView.Card.CardholderName(data = data),
            AutofillHint.Card.EXPIRATION_DATE to AutofillView.Card.ExpirationDate(data = data),
            AutofillHint.Card.EXPIRATION_MONTH to
                AutofillView.Card.ExpirationMonth(data = data, monthValue = null),
            AutofillHint.Card.EXPIRATION_YEAR to
                AutofillView.Card.ExpirationYear(data = data, yearValue = null),
            AutofillHint.Card.NUMBER to AutofillView.Card.Number(data = data),
            AutofillHint.Card.SECURITY_CODE to AutofillView.Card.SecurityCode(data = data),
        )

        assertEquals(AutofillHint.Card.entries.toSet(), expectedByHint.keys)
        expectedByHint.forEach { (hint, expected) ->
            val actual = viewNode.buildCardView(
                autofillOptions = emptyList(),
                autofillViewData = data,
                autofillHint = hint,
            )

            assertEquals(expected, actual, "$hint mapped to the wrong view")
        }
    }

    @Test
    fun `buildLoginView should map every login hint to its Login view`() {
        val data = autofillViewData()
        val expectedByHint = mapOf(
            AutofillHint.Login.PASSWORD to AutofillView.Login.Password(data = data),
            AutofillHint.Login.USERNAME to AutofillView.Login.Username(data = data),
        )

        assertEquals(AutofillHint.Login.entries.toSet(), expectedByHint.keys)
        expectedByHint.forEach { (hint, expected) ->
            val actual = buildLoginView(
                autofillViewData = data,
                autofillHint = hint,
            )

            assertEquals(expected, actual, "$hint mapped to the wrong view")
        }
    }

    @Test
    fun `buildIdentityView should map every identity hint to its Identity view`() {
        val data = autofillViewData()
        val expectedByHint = mapOf(
            AutofillHint.Identity.ADDRESS_COUNTRY to
                AutofillView.Identity.AddressCountry(data = data),
            AutofillHint.Identity.ADDRESS_LOCALITY to
                AutofillView.Identity.AddressLocality(data = data),
            AutofillHint.Identity.ADDRESS_REGION to
                AutofillView.Identity.AddressRegion(data = data),
            AutofillHint.Identity.ADDRESS_STREET to
                AutofillView.Identity.AddressStreet(data = data),
            AutofillHint.Identity.COMPANY to AutofillView.Identity.Company(data = data),
            AutofillHint.Identity.EMAIL to AutofillView.Identity.Email(data = data),
            AutofillHint.Identity.LICENSE_NUMBER to
                AutofillView.Identity.LicenseNumber(data = data),
            AutofillHint.Identity.PASSPORT_NUMBER to
                AutofillView.Identity.PassportNumber(data = data),
            AutofillHint.Identity.PERSON_NAME_FAMILY to
                AutofillView.Identity.PersonNameFamily(data = data),
            AutofillHint.Identity.PERSON_NAME_FULL to
                AutofillView.Identity.PersonNameFull(data = data),
            AutofillHint.Identity.PERSON_NAME_GIVEN to
                AutofillView.Identity.PersonNameGiven(data = data),
            AutofillHint.Identity.PERSON_NAME_MIDDLE to
                AutofillView.Identity.PersonNameMiddle(data = data),
            AutofillHint.Identity.PERSON_NAME_PREFIX to
                AutofillView.Identity.PersonNamePrefix(data = data),
            AutofillHint.Identity.POSTAL_ADDRESS_FULL to
                AutofillView.Identity.PostalAddressFull(data = data),
            AutofillHint.Identity.POSTAL_CODE to AutofillView.Identity.PostalCode(data = data),
            AutofillHint.Identity.PHONE_FULL to AutofillView.Identity.PhoneFull(data = data),
            AutofillHint.Identity.SSN to AutofillView.Identity.Ssn(data = data),
        )

        assertEquals(AutofillHint.Identity.entries.toSet(), expectedByHint.keys)
        expectedByHint.forEach { (hint, expected) ->
            val actual = buildIdentityView(
                autofillViewData = data,
                autofillHint = hint,
            )

            assertEquals(expected, actual, "$hint mapped to the wrong view")
        }
    }

    private fun autofillViewData(): AutofillView.Data = AutofillView.Data(
        autofillId = mockk<AutofillId>(),
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = false,
        textValue = null,
        hasPasswordTerms = false,
        website = null,
    )
}
