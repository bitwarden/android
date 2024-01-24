package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
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
                val autofillViewData = AutofillView.Data(
                    autofillId = nonNullAutofillId,
                    idPackage = idPackage,
                    isFocused = isFocused,
                    webDomain = webDomain,
                    webScheme = webScheme,
                )
                buildAutofillView(
                    autofillViewData = autofillViewData,
                    hint = supportedHint,
                )
            }
    }

/**
 * Convert [autofillViewData] into an [AutofillView] if the [hint] is supported.
 */
private fun buildAutofillView(
    autofillViewData: AutofillView.Data,
    hint: String,
): AutofillView? = when (hint) {
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH -> {
        AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR -> {
        AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_NUMBER -> {
        AutofillView.Card.Number(
            data = autofillViewData,
        )
    }

    View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
    }

    View.AUTOFILL_HINT_PASSWORD -> {
        AutofillView.Login.Password(
            data = autofillViewData,
        )
    }

    View.AUTOFILL_HINT_EMAIL_ADDRESS,
    View.AUTOFILL_HINT_USERNAME,
    -> {
        AutofillView.Login.Username(
            data = autofillViewData,
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
    View.AUTOFILL_HINT_PASSWORD,
    View.AUTOFILL_HINT_USERNAME,
)
