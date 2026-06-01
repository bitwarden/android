package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * The default web URI scheme.
 */
private const val DEFAULT_SCHEME: String = "https"

/**
 * The supported autofill Android View hints.
 */
private val SUPPORTED_VIEW_HINTS: List<String> = listOf(
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
    View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
    View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
    View.AUTOFILL_HINT_EMAIL_ADDRESS,
    View.AUTOFILL_HINT_PASSWORD,
    View.AUTOFILL_HINT_USERNAME,
)

/**
 * Whether this [AssistStructure.ViewNode] represents an input field.
 */
private val AssistStructure.ViewNode.isInputField: Boolean
    get() {
        val isEditText = className
            ?.let {
                try {
                    Class.forName(it)
                } catch (_: ClassNotFoundException) {
                    null
                }
            }
            ?.let { EditText::class.java.isAssignableFrom(it) } == true
        return isEditText || htmlInfo.isInputField
    }

/**
 * Attempt to convert this [AssistStructure.ViewNode] into an [AutofillView]. If the view node
 * doesn't contain a valid autofillId, it isn't an a view setup for autofill, so we return null. If
 * it doesn't have a supported hint and isn't an input field, we also return null.
 */
fun AssistStructure.ViewNode.toAutofillView(
    parentWebsite: String?,
): AutofillView? {
    val nonNullAutofillId = this.autofillId ?: return null
    if (this.supportedAutofillHint == null && !this.isInputField) return null
    val autofillOptions = this
        .autofillOptions
        .orEmpty()
        .map { it.toString() }
    val autofillViewData = AutofillView.Data(
        autofillId = nonNullAutofillId,
        autofillOptions = autofillOptions,
        autofillType = this.autofillType,
        isFocused = this.isFocused,
        textValue = this.autofillValue?.extractTextValue(),
        hasPasswordTerms = this.hasPasswordTerms(),
        website = this.website ?: parentWebsite,
    )
    return buildAutofillView(
        autofillOptions = autofillOptions,
        autofillViewData = autofillViewData,
        autofillHint = this.supportedAutofillHint,
    )
}

/**
 * The first supported autofill hint for this view node, or null if none are found.
 */
private val AssistStructure.ViewNode.supportedAutofillHint: AutofillHint?
    get() = firstSupportedAutofillHintOrNull()
        ?: when {
            this.isUsernameField -> AutofillHint.USERNAME
            this.isPasswordField -> AutofillHint.PASSWORD
            this.isCardExpirationMonthField -> AutofillHint.CARD_EXPIRATION_MONTH
            this.isCardExpirationYearField -> AutofillHint.CARD_EXPIRATION_YEAR
            this.isCardExpirationDateField -> AutofillHint.CARD_EXPIRATION_DATE
            this.isCardNumberField -> AutofillHint.CARD_NUMBER
            this.isCardSecurityCodeField -> AutofillHint.CARD_SECURITY_CODE
            this.isCardholderNameField -> AutofillHint.CARD_CARDHOLDER
            this.isCardBrandField -> AutofillHint.CARD_BRAND
            else -> null
        }

/**
 * Get the first supported autofill hint from the view node's autofillHints, or null if none are
 * found.
 */
private fun AssistStructure.ViewNode.firstSupportedAutofillHintOrNull(): AutofillHint? =
    autofillHints
        ?.firstOrNull { SUPPORTED_VIEW_HINTS.contains(it) }
        ?.toBitwardenAutofillHintOrNull()

private fun String.toBitwardenAutofillHintOrNull(): AutofillHint? =
    when (this) {
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH -> AutofillHint.CARD_EXPIRATION_MONTH
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR -> AutofillHint.CARD_EXPIRATION_YEAR
        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE -> AutofillHint.CARD_EXPIRATION_DATE
        View.AUTOFILL_HINT_CREDIT_CARD_NUMBER -> AutofillHint.CARD_NUMBER
        View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> AutofillHint.CARD_SECURITY_CODE
        View.AUTOFILL_HINT_PASSWORD -> AutofillHint.PASSWORD
        View.AUTOFILL_HINT_EMAIL_ADDRESS,
        View.AUTOFILL_HINT_USERNAME,
            -> AutofillHint.USERNAME

        else -> null
    }

/**
 * Attempt to convert this [AssistStructure.ViewNode] and [autofillViewData] into an [AutofillView].
 */
@Suppress("LongMethod")
private fun AssistStructure.ViewNode.buildAutofillView(
    autofillOptions: List<String>,
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint?,
): AutofillView = when (autofillHint) {
    AutofillHint.CARD_EXPIRATION_MONTH -> {
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

    AutofillHint.CARD_EXPIRATION_YEAR -> {
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

    AutofillHint.CARD_EXPIRATION_DATE -> {
        AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_NUMBER -> {
        AutofillView.Card.Number(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_CARDHOLDER -> {
        AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
    }

    AutofillHint.PASSWORD -> {
        AutofillView.Login.Password(
            data = autofillViewData,
        )
    }

    AutofillHint.USERNAME -> {
        AutofillView.Login.Username(
            data = autofillViewData,
        )
    }

    AutofillHint.CARD_BRAND -> {
        val brandValue = this.autofillValue
            ?.extractCardBrandValue(
                autofillOptions = autofillOptions,
            )
        AutofillView.Card.Brand(
            data = autofillViewData,
            brandValue = brandValue,
        )
    }

    null -> {
        AutofillView.Unused(
            data = autofillViewData,
        )
    }
}

/**
 * Check whether this [AssistStructure.ViewNode] represents a password field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPasswordField: Boolean
    get() {
        val isUsernameField = this.isUsernameField
        if (
            this.inputType.isPasswordInputType &&
            !this.containsIgnoredHintTerms() &&
            !isUsernameField
        ) {
            return true
        }

        return hint?.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) == true ||
            htmlInfo.isPasswordField()
    }

/**
 * Check whether this [AssistStructure.ViewNode] includes any password specific terms.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun AssistStructure.ViewNode.hasPasswordTerms(): Boolean =
    this.idEntry?.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) == true ||
        this.hint?.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) == true ||
        this.htmlInfo.hints().any { it.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) }

/**
 * Check whether this [AssistStructure.ViewNode] represents a username field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isUsernameField: Boolean
    get() = inputType.isUsernameInputType ||
        idEntry?.containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS) == true ||
        hint?.containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS) == true ||
        htmlInfo.isUsernameField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card expiration month field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardExpirationMonthField: Boolean
    get() = idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS) == true ||
        hint?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS) == true ||
        htmlInfo.isCardExpirationMonthField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card expiration year field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardExpirationYearField: Boolean
    get() = idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_YEAR_HINT_PATTERNS) == true ||
        hint?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_YEAR_HINT_PATTERNS) == true ||
        htmlInfo.isCardExpirationYearField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card expiration date field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardExpirationDateField: Boolean
    get() = idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_DATE_HINT_PATTERNS) == true ||
        hint?.matchesAnyExpressions(SUPPORTED_RAW_CARD_EXP_DATE_HINT_PATTERNS) == true ||
        htmlInfo.isCardExpirationDateField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card number field based.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardNumberField: Boolean
    get() = idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARD_NUMBER_HINT_PATTERNS) == true ||
        hint?.matchesAnyExpressions(SUPPORTED_RAW_CARD_NUMBER_HINT_PATTERNS) == true ||
        htmlInfo.isCardNumberField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card security code field based.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardSecurityCodeField: Boolean
    get() =
        idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARD_SECURITY_CODE_HINT_PATTERNS) == true ||
            hint?.matchesAnyExpressions(SUPPORTED_RAW_CARD_SECURITY_CODE_HINT_PATTERNS) == true ||
            htmlInfo.isCardSecurityCodeField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a cardholder name field based.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardholderNameField: Boolean
    get() = idEntry?.matchesAnyExpressions(SUPPORTED_RAW_CARDHOLDER_NAME_HINT_PATTERNS) == true ||
        hint?.matchesAnyExpressions(SUPPORTED_RAW_CARDHOLDER_NAME_HINT_PATTERNS) == true ||
        htmlInfo.isCardholderNameField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a card brand field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCardBrandField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_CARD_BRAND_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_CARD_BRAND_HINTS) == true ||
        htmlInfo.isCardBrandField()

/**
 * Check whether this [AssistStructure.ViewNode] contains any ignored hint terms.
 */
private fun AssistStructure.ViewNode.containsIgnoredHintTerms(): Boolean =
    this.idEntry?.containsAnyTerms(IGNORED_RAW_HINTS) == true ||
        this.hint?.containsAnyTerms(IGNORED_RAW_HINTS) == true ||
        this.htmlInfo.hints().any { it.containsAnyTerms(IGNORED_RAW_HINTS) }

/**
 * The website that this [AssistStructure.ViewNode] is a part of representing.
 */
val AssistStructure.ViewNode.website: String?
    get() = this
        .webDomain
        .takeUnless { it?.isBlank() == true }
        ?.let { webDomain ->
            val webScheme = this
                .webScheme
                .orNullIfBlank()
                ?: DEFAULT_SCHEME

            buildUri(
                domain = webDomain,
                scheme = webScheme,
            )
        }
