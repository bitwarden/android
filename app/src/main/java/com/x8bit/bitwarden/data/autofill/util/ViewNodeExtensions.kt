package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * Attempt to convert this [AssistStructure.ViewNode] into an [AutofillView]. If the view node
 * doesn't contain a valid autofillId, it isn't an a view setup for autofill, so we return null. If
 * it is has an autofillHint that we do not support, we also return null.
 */
fun AssistStructure.ViewNode.toAutofillView(): AutofillView? = autofillId
    // We only care about nodes with a valid `AutofillId`.
    ?.let { nonNullAutofillId ->
        autofillHints
            ?.firstOrNull { SUPPORTED_HINTS.contains(it) }
            ?.let { supportedHint ->
                buildAutofillView(
                    autofillId = nonNullAutofillId,
                    isFocused = isFocused,
                    hint = supportedHint,
                )
            }
    }

/**
 * Convert the data into an [AutofillView] if the [hint] is supported.
 */
@Suppress("LongMethod")
private fun buildAutofillView(
    autofillId: AutofillId,
    isFocused: Boolean,
    hint: String,
): AutofillView? = when (hint) {
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH -> {
        AutofillView.Card.ExpirationMonth(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR -> {
        AutofillView.Card.ExpirationYear(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_NUMBER -> {
        AutofillView.Card.Number(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_EMAIL_ADDRESS -> {
        AutofillView.Login.EmailAddress(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_NAME -> {
        AutofillView.Identity.Name(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_PASSWORD -> {
        AutofillView.Login.Password(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_PHONE -> {
        AutofillView.Identity.PhoneNumber(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_POSTAL_ADDRESS -> {
        AutofillView.Identity.PostalAddress(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_POSTAL_CODE -> {
        AutofillView.Identity.PostalCode(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    View.AUTOFILL_HINT_USERNAME -> {
        AutofillView.Login.Username(
            autofillId = autofillId,
            isFocused = isFocused,
        )
    }

    else -> null
}

/**
 * All of the supported autofill hints for the app.
 */
private val SUPPORTED_HINTS: List<String> = listOf(
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
    View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
    View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
    View.AUTOFILL_HINT_EMAIL_ADDRESS,
    View.AUTOFILL_HINT_NAME,
    View.AUTOFILL_HINT_PASSWORD,
    View.AUTOFILL_HINT_PHONE,
    View.AUTOFILL_HINT_POSTAL_ADDRESS,
    View.AUTOFILL_HINT_POSTAL_CODE,
    View.AUTOFILL_HINT_USERNAME,
)
