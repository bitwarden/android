package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import androidx.autofill.HintConstants
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.data.autofill.model.AutofillHint
import com.x8bit.bitwarden.data.autofill.model.AutofillView

/**
 * The default web URI scheme.
 */
private const val DEFAULT_SCHEME: String = "https"

/**
 * The W3C HTML `autocomplete="address-line2"` token, surfaced verbatim by Chrome in
 * [AssistStructure.ViewNode.getAutofillHints]. Kept as a raw string because no androidx
 * [HintConstants] value equals it — [HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS]
 * is "extendedAddress", which Chrome does not emit.
 */
private const val AUTOFILL_HINT_ADDRESS_LINE_2: String = "address-line2"

/**
 * The supported autofill Android View hints that predate identity autofill.
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
 * The supported autofill Android View hints that are only meaningful for identity autofill.
 * Consulted only when identity autofill is enabled --
 * see [AssistStructure.ViewNode.toAutofillView].
 */
private val SUPPORTED_IDENTITY_VIEW_HINTS: List<String> = listOf(
    HintConstants.AUTOFILL_HINT_PERSON_NAME,
    HintConstants.AUTOFILL_HINT_PERSON_NAME_PREFIX,
    HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN,
    HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE,
    HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY,
    View.AUTOFILL_HINT_POSTAL_ADDRESS,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER,
    // Raw W3C `autocomplete` token Chrome reports verbatim; no androidx constant equals it.
    AUTOFILL_HINT_ADDRESS_LINE_2,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION,
    HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY,
    View.AUTOFILL_HINT_POSTAL_CODE,
    View.AUTOFILL_HINT_PHONE,
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
    isIdentityAutofillEnabled: Boolean,
): AutofillView? {
    val nonNullAutofillId = this.autofillId ?: return null
    val hint = this.supportedAutofillHint(isIdentityAutofillEnabled = isIdentityAutofillEnabled)
    val isInput = this.isInputField
    if (hint == null && !isInput) return null

    // When a container (autofillType=NONE) is classified with a semantic hint, the container
    // itself cannot receive an autofill value. Redirect to the first autofillable child so the
    // fill reaches the inner EditText instead of silently failing on the wrapper.
    // The child's autofillType must also be used — the container's type=0 (NONE) would cause
    // buildFilledItemOrNull to return null and drop the field from the fill dataset entirely.
    val autofillableChild =
        if (hint != null && autofillType == View.AUTOFILL_TYPE_NONE) {
            findFirstAutofillableChild()
        } else {
            null
        }
    val effectiveAutofillId = autofillableChild?.autofillId ?: nonNullAutofillId
    val effectiveAutofillType = autofillableChild?.autofillType ?: autofillType

    val autofillOptions = this
        .autofillOptions
        .orEmpty()
        .map { it.toString() }
    val autofillViewData = AutofillView.Data(
        autofillId = effectiveAutofillId,
        autofillOptions = autofillOptions,
        autofillType = effectiveAutofillType,
        isFocused = this.isFocused,
        textValue = this.autofillValue?.extractTextValue(),
        hasPasswordTerms = this.hasPasswordTerms(),
        website = this.website ?: parentWebsite,
    )
    return buildAutofillView(
        autofillOptions = autofillOptions,
        autofillViewData = autofillViewData,
        autofillHint = hint,
    )
}

private data class AutofillableChild(val autofillId: AutofillId, val autofillType: Int)

private fun AssistStructure.ViewNode.findFirstAutofillableChild(): AutofillableChild? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child.autofillType == View.AUTOFILL_TYPE_TEXT) {
            val id = child.autofillId ?: continue
            return AutofillableChild(autofillId = id, autofillType = child.autofillType)
        }
        child.findFirstAutofillableChild()?.let { return it }
    }
    return null
}

/**
 * Builds an [AutofillView.Data] for this [AssistStructure.ViewNode] using the given [autofillId]
 * and [website].
 */
internal fun AssistStructure.ViewNode.toAutofillViewData(
    autofillId: AutofillId,
    website: String?,
): AutofillView.Data = AutofillView.Data(
    autofillId = autofillId,
    autofillOptions = autofillOptions?.map { it.toString() }.orEmpty(),
    autofillType = autofillType,
    isFocused = isFocused,
    textValue = autofillValue?.extractTextValue(),
    hasPasswordTerms = hasPasswordTerms(),
    website = website,
)

/**
 * The first supported autofill hint for this view node, or null if none are found. Identity
 * classification is gated on [isIdentityAutofillEnabled] so that, until identity fulfillment
 * ships, a node classifies exactly as it did before identity heuristics existed -- e.g. falling
 * through to [isUsernameField] rather than being claimed by an identity heuristic.
 */
@Suppress("CyclomaticComplexMethod")
private fun AssistStructure.ViewNode.supportedAutofillHint(
    isIdentityAutofillEnabled: Boolean,
): AutofillHint? = firstSupportedAutofillHintOrNull(
    isIdentityAutofillEnabled = isIdentityAutofillEnabled,
)
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
        !isIdentityAutofillEnabled -> null
        this.isPersonNameFullField -> AutofillHint.IDENTITY_PERSON_NAME_FULL
        this.isPersonNamePrefixField -> AutofillHint.IDENTITY_PERSON_NAME_PREFIX
        this.isPersonNameGivenField -> AutofillHint.IDENTITY_PERSON_NAME_GIVEN
        this.isPersonNameMiddleField -> AutofillHint.IDENTITY_PERSON_NAME_MIDDLE
        this.isPersonNameFamilyField -> AutofillHint.IDENTITY_PERSON_NAME_FAMILY
        this.isPostalAddressFullField -> AutofillHint.IDENTITY_POSTAL_ADDRESS_FULL
        this.isAddressStreetField -> AutofillHint.IDENTITY_ADDRESS_STREET
        this.isAddressLocalityField -> AutofillHint.IDENTITY_ADDRESS_LOCALITY
        this.isAddressRegionField -> AutofillHint.IDENTITY_ADDRESS_REGION
        this.isAddressCountryField -> AutofillHint.IDENTITY_ADDRESS_COUNTRY
        this.isPostalCodeField -> AutofillHint.IDENTITY_POSTAL_CODE
        this.isPhoneField -> AutofillHint.IDENTITY_PHONE_FULL
        this.isCompanyField -> AutofillHint.IDENTITY_COMPANY
        this.isSsnField -> AutofillHint.IDENTITY_SSN
        this.isPassportNumberField -> AutofillHint.IDENTITY_PASSPORT_NUMBER
        this.isLicenseNumberField -> AutofillHint.IDENTITY_LICENSE_NUMBER
        else -> null
    }

/**
 * Get the first supported autofill hint from the view node's autofillHints, or null if none are
 * found. [SUPPORTED_IDENTITY_VIEW_HINTS] is only consulted when [isIdentityAutofillEnabled] is
 * true.
 */
private fun AssistStructure.ViewNode.firstSupportedAutofillHintOrNull(
    isIdentityAutofillEnabled: Boolean,
): AutofillHint? {
    val supportedHints = if (isIdentityAutofillEnabled) {
        SUPPORTED_VIEW_HINTS + SUPPORTED_IDENTITY_VIEW_HINTS
    } else {
        SUPPORTED_VIEW_HINTS
    }
    return autofillHints
        ?.firstOrNull { supportedHints.contains(it) }
        ?.toBitwardenAutofillHintOrNull()
}

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

        HintConstants.AUTOFILL_HINT_PERSON_NAME -> AutofillHint.IDENTITY_PERSON_NAME_FULL
        HintConstants.AUTOFILL_HINT_PERSON_NAME_PREFIX -> AutofillHint.IDENTITY_PERSON_NAME_PREFIX
        HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN -> AutofillHint.IDENTITY_PERSON_NAME_GIVEN
        HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE -> AutofillHint.IDENTITY_PERSON_NAME_MIDDLE
        HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY -> AutofillHint.IDENTITY_PERSON_NAME_FAMILY
        View.AUTOFILL_HINT_POSTAL_ADDRESS -> AutofillHint.IDENTITY_POSTAL_ADDRESS_FULL
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS -> {
            AutofillHint.IDENTITY_ADDRESS_STREET
        }

        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS,
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER,
        AUTOFILL_HINT_ADDRESS_LINE_2,
            -> {
            AutofillHint.IDENTITY_ADDRESS_EXTENDED
        }

        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY -> {
            AutofillHint.IDENTITY_ADDRESS_LOCALITY
        }

        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION -> AutofillHint.IDENTITY_ADDRESS_REGION
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY -> {
            AutofillHint.IDENTITY_ADDRESS_COUNTRY
        }

        View.AUTOFILL_HINT_POSTAL_CODE -> AutofillHint.IDENTITY_POSTAL_CODE
        View.AUTOFILL_HINT_PHONE -> AutofillHint.IDENTITY_PHONE_FULL

        else -> null
    }

/**
 * Attempt to convert this [AssistStructure.ViewNode] and [autofillViewData] into an [AutofillView].
 * Dispatches to a type-specific builder ([buildCardView], [buildLoginView], [buildIdentityView])
 * based on which category [autofillHint] belongs to. This `when` intentionally lists every
 * [AutofillHint] value (rather than a single fallback per category) so the compiler still forces a
 * decision here whenever a new hint is added, even though the actual construction logic lives in
 * the category-specific builder.
 */
private fun AssistStructure.ViewNode.buildAutofillView(
    autofillOptions: List<String>,
    autofillViewData: AutofillView.Data,
    autofillHint: AutofillHint?,
): AutofillView = when (autofillHint) {
    AutofillHint.CARD_EXPIRATION_MONTH,
    AutofillHint.CARD_EXPIRATION_YEAR,
    AutofillHint.CARD_EXPIRATION_DATE,
    AutofillHint.CARD_NUMBER,
    AutofillHint.CARD_SECURITY_CODE,
    AutofillHint.CARD_CARDHOLDER,
    AutofillHint.CARD_BRAND,
        -> buildCardView(
        autofillOptions = autofillOptions,
        autofillViewData = autofillViewData,
        autofillHint = autofillHint,
    )
        ?: AutofillView.Unused(data = autofillViewData)

    AutofillHint.PASSWORD,
    AutofillHint.USERNAME,
        -> buildLoginView(
        autofillViewData = autofillViewData,
        autofillHint = autofillHint,
    )
        ?: AutofillView.Unused(data = autofillViewData)

    AutofillHint.IDENTITY_PERSON_NAME_FULL,
    AutofillHint.IDENTITY_PERSON_NAME_PREFIX,
    AutofillHint.IDENTITY_PERSON_NAME_GIVEN,
    AutofillHint.IDENTITY_PERSON_NAME_MIDDLE,
    AutofillHint.IDENTITY_PERSON_NAME_FAMILY,
    AutofillHint.IDENTITY_POSTAL_ADDRESS_FULL,
    AutofillHint.IDENTITY_ADDRESS_STREET,
    AutofillHint.IDENTITY_ADDRESS_EXTENDED,
    AutofillHint.IDENTITY_ADDRESS_LOCALITY,
    AutofillHint.IDENTITY_ADDRESS_REGION,
    AutofillHint.IDENTITY_ADDRESS_COUNTRY,
    AutofillHint.IDENTITY_POSTAL_CODE,
    AutofillHint.IDENTITY_PHONE_FULL,
    AutofillHint.IDENTITY_COMPANY,
    AutofillHint.IDENTITY_EMAIL,
    AutofillHint.IDENTITY_SSN,
    AutofillHint.IDENTITY_PASSPORT_NUMBER,
    AutofillHint.IDENTITY_LICENSE_NUMBER,
        -> buildIdentityView(
        autofillViewData = autofillViewData,
        autofillHint = autofillHint,
    )
        ?: AutofillView.Unused(data = autofillViewData)

    null -> AutofillView.Unused(data = autofillViewData)
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
 * Check whether this [AssistStructure.ViewNode] represents an email field. Kept separate from
 * [isUsernameField] so an email-hinted or email-heuristic field can be independently offered as
 * an identity candidate alongside the existing username classification.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isEmailField: Boolean
    get() = autofillHints?.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS) == true ||
        idEntry
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_EMAIL_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_EMAIL_HINTS) == true ||
        htmlInfo.isEmailField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a full person name field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPersonNameFullField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FULL_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FULL_HINTS) == true ||
        htmlInfo.isPersonNameFullField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a person name prefix field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPersonNamePrefixField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_PREFIX_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_PREFIX_HINTS) == true ||
        htmlInfo.isPersonNamePrefixField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a given (first) name field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPersonNameGivenField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_GIVEN_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_GIVEN_HINTS) == true ||
        htmlInfo.isPersonNameGivenField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a middle name field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPersonNameMiddleField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_MIDDLE_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_MIDDLE_HINTS) == true ||
        htmlInfo.isPersonNameMiddleField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a family (last) name field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPersonNameFamilyField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FAMILY_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FAMILY_HINTS) == true ||
        htmlInfo.isPersonNameFamilyField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a full postal address field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPostalAddressFullField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_POSTAL_ADDRESS_FULL_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_POSTAL_ADDRESS_FULL_HINTS) == true ||
        htmlInfo.isPostalAddressFullField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a street address field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isAddressStreetField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_STREET_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_STREET_HINTS) == true ||
        htmlInfo.isAddressStreetField()

/**
 * Check whether this [AssistStructure.ViewNode] represents an extended/secondary address (e.g.
 * apartment, suite, unit) field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isAddressExtendedField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_EXTENDED_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_EXTENDED_HINTS) == true ||
        htmlInfo.isAddressExtendedField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a locality (city) field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isAddressLocalityField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_LOCALITY_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_LOCALITY_HINTS) == true ||
        htmlInfo.isAddressLocalityField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a region (state/province) field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isAddressRegionField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_REGION_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_REGION_HINTS) == true ||
        htmlInfo.isAddressRegionField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a country field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isAddressCountryField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_COUNTRY_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_ADDRESS_COUNTRY_HINTS) == true ||
        htmlInfo.isAddressCountryField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a postal code field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPostalCodeField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_POSTAL_CODE_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_POSTAL_CODE_HINTS) == true ||
        htmlInfo.isPostalCodeField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a phone number field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPhoneField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PHONE_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PHONE_HINTS) == true ||
        htmlInfo.isPhoneField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a company field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isCompanyField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_COMPANY_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_COMPANY_HINTS) == true ||
        htmlInfo.isCompanyField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a social security number field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isSsnField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_SSN_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_SSN_HINTS) == true ||
        htmlInfo.isSsnField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a passport number field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isPassportNumberField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_PASSPORT_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_PASSPORT_HINTS) == true ||
        htmlInfo.isPassportNumberField()

/**
 * Check whether this [AssistStructure.ViewNode] represents a license number field.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val AssistStructure.ViewNode.isLicenseNumberField: Boolean
    get() = idEntry
        ?.toLowerCaseAndStripNonAlpha()
        ?.containsAnyTerms(SUPPORTED_RAW_LICENSE_HINTS) == true ||
        hint
            ?.toLowerCaseAndStripNonAlpha()
            ?.containsAnyTerms(SUPPORTED_RAW_LICENSE_HINTS) == true ||
        htmlInfo.isLicenseNumberField()

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
