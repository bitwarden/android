package com.x8bit.bitwarden.data.autofill.util

import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * The text value representation of the expiration month from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.expirationMonthSaveValue: String?
    get() = this
        .views
        .filterIsInstance<AutofillView.Card.ExpirationMonth>()
        .firstOrNull { it.monthValue != null }
        ?.monthValue

/**
 * The text value representation of the year from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.expirationYearSaveValue: String?
    get() = this
        .views
        .filterIsInstance<AutofillView.Card.ExpirationYear>()
        .firstOrNull { it.yearValue != null }
        ?.yearValue

/**
 * The text value representation of the card number from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.numberSaveValue: String?
    get() = this
        .extractNonNullTextValueOrNull { it is AutofillView.Card.Number }

/**
 * The text value representation of the security code from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.securityCodeSaveValue: String?
    get() = this
        .extractNonNullTextValueOrNull { it is AutofillView.Card.SecurityCode }

/**
 * The text value representation of the cardholder name from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.cardholderNameSaveValue: String?
    get() = this
        .extractNonNullTextValueOrNull { it is AutofillView.Card.CardholderName }

/**
 * The text value representation of the brand from the [AutofillPartition.Card].
 */
val AutofillPartition.Card.brandSaveValue: String?
    get() = this
        .views
        .filterIsInstance<AutofillView.Card.Brand>()
        .firstOrNull { it.brandValue != null }
        ?.brandValue
        ?: this.extractNonNullTextValueOrNull { it is AutofillView.Card.Brand }

/**
 * The text value representation of the password from the [AutofillPartition.Login].
 */
val AutofillPartition.Login.passwordSaveValue: String?
    get() = this
        .extractNonNullTextValueOrNull { it is AutofillView.Login.Password }

/**
 * The text value representation of the username from the [AutofillPartition.Login].
 */
val AutofillPartition.Login.usernameSaveValue: String?
    get() = this
        .extractNonNullTextValueOrNull { it is AutofillView.Login.Username }

/**
 * Search [AutofillPartition.views] for an [AutofillView] that matches [condition] and has a
 * non-null text value then return that text value.
 */
private fun AutofillPartition.extractNonNullTextValueOrNull(
    condition: (AutofillView) -> Boolean,
): String? =
    this
        .views
        .firstOrNull { condition(it) && it.data.textValue != null }
        ?.data
        ?.textValue
