package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

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
                        data,
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
    "username" -> AutofillView.Login.Username(data = data)
    "email" -> AutofillView.Login.Email(data = data)
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
