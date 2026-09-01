package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

private const val FIELD_KEY_USERNAME = "username"
private const val FIELD_KEY_EMAIL = "email"
private const val FIELD_KEY_PHONE = "phone"
private const val FIELD_KEY_PASSWORD = "password"
private const val FIELD_KEY_NEW_PASSWORD = "newPassword"
private const val FIELD_KEY_CARD_NUMBER = "cardNumber"
private const val FIELD_KEY_CARDHOLDER_NAME = "cardholderName"
private const val FIELD_KEY_CARD_EXPIRATION_DATE = "cardExpirationDate"
private const val FIELD_KEY_CARD_EXPIRATION_MONTH = "cardExpirationMonth"
private const val FIELD_KEY_CARD_EXPIRATION_YEAR = "cardExpirationYear"
private const val FIELD_KEY_CARD_CVV = "cardCvv"
private const val FIELD_KEY_CARD_TYPE = "cardType"
private const val FIELD_KEY_PERSON_NAME_FULL = "personNameFull"
private const val FIELD_KEY_PERSON_NAME_PREFIX = "personNamePrefix"
private const val FIELD_KEY_PERSON_NAME_GIVEN = "personNameGiven"
private const val FIELD_KEY_PERSON_NAME_MIDDLE = "personNameMiddle"
private const val FIELD_KEY_PERSON_NAME_FAMILY = "personNameFamily"
private const val FIELD_KEY_POSTAL_ADDRESS_FULL = "postalAddressFull"
private const val FIELD_KEY_ADDRESS_STREET = "addressStreet"
private const val FIELD_KEY_ADDRESS_LOCALITY = "addressLocality"
private const val FIELD_KEY_ADDRESS_REGION = "addressRegion"
private const val FIELD_KEY_ADDRESS_COUNTRY = "addressCountry"
private const val FIELD_KEY_POSTAL_CODE = "postalCode"
private const val FIELD_KEY_PHONE_FULL = "phoneFull"
private const val FIELD_KEY_COMPANY = "company"
private const val FIELD_KEY_SSN = "ssn"
private const val FIELD_KEY_PASSPORT_NUMBER = "passportNumber"
private const val FIELD_KEY_LICENSE_NUMBER = "licenseNumber"

/**
 * Traverses the [AssistStructure] and returns a list of [AutofillView]s classified by the
 * provided [hostRules]. Only view nodes whose [android.view.ViewStructure.HtmlInfo] attributes
 * match a [FillAssistRules.SelectorClause] are included; unmatched nodes are omitted (no
 * heuristic fallback). All identity classification is gated behind [isIdentityAutofillEnabled].
 */
internal fun AssistStructure.buildFillAssistViews(
    hostRules: List<FillAssistRules.HostRule>,
    urlBarWebsite: String?,
    isIdentityAutofillEnabled: Boolean,
): List<AutofillView> =
    (0 until windowNodeCount)
        .mapNotNull { getWindowNodeAt(it).rootViewNode }
        .flatMap {
            it.traverseForFillAssist(
                hostRules = hostRules,
                parentWebsite = urlBarWebsite,
                isIdentityAutofillEnabled = isIdentityAutofillEnabled,
            )
        }

private fun AssistStructure.ViewNode.traverseForFillAssist(
    hostRules: List<FillAssistRules.HostRule>,
    parentWebsite: String?,
    isIdentityAutofillEnabled: Boolean,
): List<AutofillView> {
    val website = this.website ?: parentWebsite
    val ownViews = autofillId?.let { id ->
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
                val candidateViews = matchingEntries.mapNotNull { (key, _) ->
                    key.toAutofillViewForFieldKey(
                        data = data,
                        isIdentityAutofillEnabled = isIdentityAutofillEnabled,
                    )?.let { key to it }
                }
                // Prefer Username: it has no format gate, while Login.Email rejects non-email
                // values via isValidEmail().
                val view = candidateViews
                    .firstOrNull { (_, view) -> view is AutofillView.Login.Username }
                    ?.second
                    ?: candidateViews.firstOrNull()?.second
                    ?: return@let null

                // Dual-classify off the full matched-key set, not just the winning key, so a
                // field matched under both "email" and "phone" gets both Identity views.
                val isLoginIdentifierView = view is AutofillView.Login.Username ||
                    view is AutofillView.Login.Email
                buildList<AutofillView> {
                    add(view)
                    if (isIdentityAutofillEnabled && isLoginIdentifierView) {
                        val matchedKeys = candidateViews.mapTo(mutableSetOf()) { it.first }
                        if (FIELD_KEY_EMAIL in matchedKeys) {
                            add(AutofillView.Identity.Email(data = view.data))
                        }
                        if (FIELD_KEY_PHONE in matchedKeys) {
                            add(AutofillView.Identity.PhoneFull(data = view.data))
                        }
                    }
                }
            }
    }.orEmpty()
    val childViews = (0 until childCount)
        .flatMap { index ->
            getChildAt(index).traverseForFillAssist(
                hostRules = hostRules,
                parentWebsite = website,
                isIdentityAutofillEnabled = isIdentityAutofillEnabled,
            )
        }
    return ownViews + childViews
}

/**
 * Maps this field key to the [AutofillView] it represents, or null if this key is unrecognized.
 * Delegates to a type-specific mapper ([toLoginViewForFieldKey], [toCardViewForFieldKey],
 * [toIdentityViewForFieldKey]) grouped by the category the field key belongs to.
 */
private fun String.toAutofillViewForFieldKey(
    data: AutofillView.Data,
    isIdentityAutofillEnabled: Boolean,
): AutofillView? =
    toLoginViewForFieldKey(data = data)
        ?: toCardViewForFieldKey(data = data)
        ?: if (isIdentityAutofillEnabled) toIdentityViewForFieldKey(data = data) else null

private fun String.toLoginViewForFieldKey(data: AutofillView.Data): AutofillView.Login? =
    when (this) {
        FIELD_KEY_USERNAME, FIELD_KEY_PHONE -> AutofillView.Login.Username(data = data)
        FIELD_KEY_EMAIL -> AutofillView.Login.Email(data = data)
        FIELD_KEY_PASSWORD, FIELD_KEY_NEW_PASSWORD -> AutofillView.Login.Password(data = data)
        else -> null
    }

private fun String.toCardViewForFieldKey(data: AutofillView.Data): AutofillView.Card? =
    when (this) {
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

private fun String.toIdentityViewForFieldKey(data: AutofillView.Data): AutofillView.Identity? =
    when (this) {
        FIELD_KEY_PERSON_NAME_FULL -> AutofillView.Identity.PersonNameFull(data = data)
        FIELD_KEY_PERSON_NAME_PREFIX -> AutofillView.Identity.PersonNamePrefix(data = data)
        FIELD_KEY_PERSON_NAME_GIVEN -> AutofillView.Identity.PersonNameGiven(data = data)
        FIELD_KEY_PERSON_NAME_MIDDLE -> AutofillView.Identity.PersonNameMiddle(data = data)
        FIELD_KEY_PERSON_NAME_FAMILY -> AutofillView.Identity.PersonNameFamily(data = data)
        FIELD_KEY_POSTAL_ADDRESS_FULL -> AutofillView.Identity.PostalAddressFull(data = data)
        FIELD_KEY_ADDRESS_STREET -> AutofillView.Identity.AddressStreet(data = data)
        FIELD_KEY_ADDRESS_LOCALITY -> AutofillView.Identity.AddressLocality(data = data)
        FIELD_KEY_ADDRESS_REGION -> AutofillView.Identity.AddressRegion(data = data)
        FIELD_KEY_ADDRESS_COUNTRY -> AutofillView.Identity.AddressCountry(data = data)
        FIELD_KEY_POSTAL_CODE -> AutofillView.Identity.PostalCode(data = data)
        FIELD_KEY_PHONE_FULL -> AutofillView.Identity.PhoneFull(data = data)
        FIELD_KEY_COMPANY -> AutofillView.Identity.Company(data = data)
        FIELD_KEY_SSN -> AutofillView.Identity.Ssn(data = data)
        FIELD_KEY_PASSPORT_NUMBER -> AutofillView.Identity.PassportNumber(data = data)
        FIELD_KEY_LICENSE_NUMBER -> AutofillView.Identity.LicenseNumber(data = data)
        else -> null
    }
