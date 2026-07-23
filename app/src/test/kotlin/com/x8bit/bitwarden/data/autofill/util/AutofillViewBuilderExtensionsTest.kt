package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AutofillViewBuilderExtensionsTest {

    private val viewNode: AssistStructure.ViewNode = mockk()

    @Test
    fun `buildCardView should return AutofillView Card Number when hint is CARD_NUMBER`() {
        val data = autofillViewData()

        val actual = viewNode.buildCardView(
            autofillOptions = emptyList(),
            autofillViewData = data,
            autofillHint = AutofillHint.CARD_NUMBER,
        )

        assertEquals(AutofillView.Card.Number(data = data), actual)
    }

    @Test
    fun `buildCardView should return null when hint is not a card hint`() {
        val actual = viewNode.buildCardView(
            autofillOptions = emptyList(),
            autofillViewData = autofillViewData(),
            autofillHint = AutofillHint.USERNAME,
        )

        assertNull(actual)
    }

    @Test
    fun `buildLoginView should return AutofillView Login Username when hint is USERNAME`() {
        val data = autofillViewData()

        val actual = buildLoginView(
            autofillViewData = data,
            autofillHint = AutofillHint.USERNAME,
        )

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Test
    fun `buildLoginView should return null when hint is not a login hint`() {
        val actual = buildLoginView(
            autofillViewData = autofillViewData(),
            autofillHint = AutofillHint.CARD_NUMBER,
        )

        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildIdentityView should return AutofillView Identity Company when hint is IDENTITY_COMPANY`() {
        val data = autofillViewData()

        val actual = buildIdentityView(
            autofillViewData = data,
            autofillHint = AutofillHint.IDENTITY_COMPANY,
        )

        assertEquals(AutofillView.Identity.Company(data = data), actual)
    }

    @Test
    fun `buildIdentityView should return null when hint is not an identity hint`() {
        val actual = buildIdentityView(
            autofillViewData = autofillViewData(),
            autofillHint = AutofillHint.USERNAME,
        )

        assertNull(actual)
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
