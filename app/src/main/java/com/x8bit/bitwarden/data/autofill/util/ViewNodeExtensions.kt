package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.widget.EditText
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank

/**
 * The default web URI scheme.
 */
private const val DEFAULT_SCHEME: String = "https"

/**
 * The set of raw autofill hints that should be ignored.
 */
private val IGNORED_RAW_HINTS: List<String> = listOf(
    "search",
    "find",
    "recipient",
    "edit",
)

/**
 * The supported password autofill hints.
 */
private val SUPPORTED_RAW_PASSWORD_HINTS: List<String> = listOf(
    "password",
    "pswd",
)

/**
 * The supported raw autofill hints.
 */
private val SUPPORTED_RAW_USERNAME_HINTS: List<String> = listOf(
    "email",
    "phone",
    "username",
)

/**
 * The supported autofill Android View hints.
 */
private val SUPPORTED_VIEW_HINTS: List<String> = listOf(
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
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
                } catch (e: ClassNotFoundException) {
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
fun AssistStructure.ViewNode.toAutofillView(): AutofillView? =
    this
        .autofillId
        // We only care about nodes with a valid `AutofillId`.
        ?.let { nonNullAutofillId ->
            val supportedHint = this
                .autofillHints
                ?.firstOrNull { SUPPORTED_VIEW_HINTS.contains(it) }

            if (supportedHint != null || this.isInputField) {
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
                )
                buildAutofillView(
                    autofillOptions = autofillOptions,
                    autofillViewData = autofillViewData,
                    supportedHint = supportedHint,
                )
            } else {
                null
            }
        }

/**
 * Attempt to convert this [AssistStructure.ViewNode] and [autofillViewData] into an [AutofillView].
 */
private fun AssistStructure.ViewNode.buildAutofillView(
    autofillOptions: List<String>,
    autofillViewData: AutofillView.Data,
    supportedHint: String?,
): AutofillView = when {
    supportedHint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH -> {
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

    supportedHint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR -> {
        AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )
    }

    supportedHint == View.AUTOFILL_HINT_CREDIT_CARD_NUMBER -> {
        AutofillView.Card.Number(
            data = autofillViewData,
        )
    }

    supportedHint == View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> {
        AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
    }

    this.isPasswordField(supportedHint) -> {
        AutofillView.Login.Password(
            data = autofillViewData,
        )
    }

    this.isUsernameField(supportedHint) -> {
        AutofillView.Login.Username(
            data = autofillViewData,
        )
    }

    else -> {
        AutofillView.Unused(
            data = autofillViewData,
        )
    }
}

/**
 * Check whether this [AssistStructure.ViewNode] represents a password field.
 */
fun AssistStructure.ViewNode.isPasswordField(
    supportedHint: String?,
): Boolean {
    if (supportedHint == View.AUTOFILL_HINT_PASSWORD) return true

    val isInvalidField = this.idEntry?.containsAnyTerms(IGNORED_RAW_HINTS) == true ||
        this.hint?.containsAnyTerms(IGNORED_RAW_HINTS) == true
    val isUsernameField = this.isUsernameField(supportedHint)
    if (this.inputType.isPasswordInputType && !isInvalidField && !isUsernameField) return true

    return this
        .htmlInfo
        .isPasswordField()
}

/**
 * Check whether this [AssistStructure.ViewNode] includes any password specific terms.
 */
fun AssistStructure.ViewNode.hasPasswordTerms(): Boolean =
    this.idEntry?.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) == true ||
        this.hint?.containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS) == true

/**
 * Check whether this [AssistStructure.ViewNode] represents a username field.
 */
fun AssistStructure.ViewNode.isUsernameField(
    supportedHint: String?,
): Boolean =
    supportedHint == View.AUTOFILL_HINT_USERNAME ||
        supportedHint == View.AUTOFILL_HINT_EMAIL_ADDRESS ||
        inputType.isUsernameInputType ||
        idEntry?.containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS) == true ||
        hint?.containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS) == true ||
        htmlInfo.isUsernameField()

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
