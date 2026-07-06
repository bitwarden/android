package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

/**
 * Attempts to match this [AssistStructure.ViewNode] against the given [hostRules] and return the
 * corresponding [AutofillView], or null if no rule matches or the node has no [AutofillView.Data].
 */
internal fun AssistStructure.ViewNode.toFillAssistView(
    hostRules: List<FillAssistRules.HostRule>,
    website: String?,
): AutofillView? {
    val id = autofillId ?: return null
    return hostRules
        .flatMap { it.fields.entries }
        .filter { (_, alternatives) ->
            alternatives.any { htmlInfo?.matchesSelectorClause(it) ?: false }
        }
        .takeIf { it.isNotEmpty() }
        ?.let { matchingEntries ->
            val data = toAutofillViewData(autofillId = id, website = website)
            matchingEntries.firstNotNullOfOrNull { (key, _) -> key.toFillAssistAutofillView(data) }
        }
}

internal fun String.toFillAssistAutofillView(data: AutofillView.Data): AutofillView? = when (this) {
    "username" -> AutofillView.Login.Username(data = data)
    "password", "newPassword" -> AutofillView.Login.Password(data = data)
    "cardNumber" -> AutofillView.Card.Number(data = data)
    "cardholderName" -> AutofillView.Card.CardholderName(data = data)
    "cardExpirationDate" -> AutofillView.Card.ExpirationDate(data = data)
    "cardExpirationMonth" -> AutofillView.Card.ExpirationMonth(data = data, monthValue = null)
    "cardExpirationYear" -> AutofillView.Card.ExpirationYear(data = data, yearValue = null)
    "cardCvv" -> AutofillView.Card.SecurityCode(data = data)
    "cardType" -> AutofillView.Card.Brand(data = data, brandValue = null)
    else -> null
}
