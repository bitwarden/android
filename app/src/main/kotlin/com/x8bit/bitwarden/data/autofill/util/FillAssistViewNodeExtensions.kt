package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

private const val FIELD_KEY_USERNAME = "username"
private const val FIELD_KEY_EMAIL = "email"
private const val FIELD_KEY_PASSWORD = "password"
private const val FIELD_KEY_NEW_PASSWORD = "newPassword"
private const val FIELD_KEY_CARD_NUMBER = "cardNumber"
private const val FIELD_KEY_CARDHOLDER_NAME = "cardholderName"
private const val FIELD_KEY_CARD_EXPIRATION_DATE = "cardExpirationDate"
private const val FIELD_KEY_CARD_EXPIRATION_MONTH = "cardExpirationMonth"
private const val FIELD_KEY_CARD_EXPIRATION_YEAR = "cardExpirationYear"
private const val FIELD_KEY_CARD_CVV = "cardCvv"
private const val FIELD_KEY_CARD_TYPE = "cardType"

/**
 * Traverses the [AssistStructure] and returns a list of [AutofillView]s classified by the
 * provided [hostRules]. Only view nodes whose [android.view.ViewStructure.HtmlInfo] attributes
 * match a [FillAssistRules.SelectorClause] are included; unmatched nodes are omitted (no
 * heuristic fallback).
 */
internal fun AssistStructure.buildFillAssistViews(
    hostRules: List<FillAssistRules.HostRule>,
    urlBarWebsite: String?,
): List<AutofillView> =
    (0 until windowNodeCount)
        .mapNotNull { getWindowNodeAt(it).rootViewNode }
        .flatMap { it.traverseForFillAssist(hostRules = hostRules, parentWebsite = urlBarWebsite) }

private fun AssistStructure.ViewNode.traverseForFillAssist(
    hostRules: List<FillAssistRules.HostRule>,
    parentWebsite: String?,
): List<AutofillView> {
    val website = this.website ?: parentWebsite
    val ownView = autofillId?.let { id ->
        hostRules
            .flatMap { it.fields.entries }
            .filter { (_, alternatives) ->
                alternatives.any {
                    htmlInfo?.matchesSelectorClause(it) ?: false
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { matchingEntries ->
                val data = toAutofillViewData(autofillId = id, website = website)
                matchingEntries.firstNotNullOfOrNull { (key, _) ->
                    key.toAutofillViewForFieldKey(
                        data = data,
                    )
                }
            }
    }
    val childViews = (0 until childCount)
        .flatMap { index ->
            getChildAt(index).traverseForFillAssist(
                hostRules = hostRules,
                parentWebsite = website,
            )
        }
    return listOfNotNull(ownView) + childViews
}

private fun String.toAutofillViewForFieldKey(data: AutofillView.Data): AutofillView? = when (this) {
    FIELD_KEY_USERNAME -> AutofillView.Login.Username(data = data)
    FIELD_KEY_EMAIL -> AutofillView.Login.Email(data = data)
    FIELD_KEY_PASSWORD, FIELD_KEY_NEW_PASSWORD -> AutofillView.Login.Password(data = data)
    FIELD_KEY_CARD_NUMBER -> AutofillView.Card.Number(data = data)
    FIELD_KEY_CARDHOLDER_NAME -> AutofillView.Card.CardholderName(data = data)
    FIELD_KEY_CARD_EXPIRATION_DATE -> AutofillView.Card.ExpirationDate(data = data)
    FIELD_KEY_CARD_EXPIRATION_MONTH -> AutofillView.Card.ExpirationMonth(
        data = data,
        monthValue = null,
    )

    FIELD_KEY_CARD_EXPIRATION_YEAR -> AutofillView.Card.ExpirationYear(
        data = data,
        yearValue = null,
    )

    FIELD_KEY_CARD_CVV -> AutofillView.Card.SecurityCode(data = data)
    FIELD_KEY_CARD_TYPE -> AutofillView.Card.Brand(data = data, brandValue = null)
    else -> null
}
