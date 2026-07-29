@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.data.autofill.util

import android.util.Pair
import android.view.ViewStructure.HtmlInfo
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

private const val HTML_ATTR_ID = "id"
private const val HTML_ATTR_NAME = "name"
private const val HTML_ATTR_TYPE = "type"
private const val HTML_ATTR_ROLE = "role"

/**
 * Whether this [HtmlInfo] represents a password field.
 */
fun HtmlInfo?.isPasswordField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS)

/**
 * Whether this [HtmlInfo] represents a username field.
 */
fun HtmlInfo?.isUsernameField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS)

/**
 * Whether this [HtmlInfo] represents a cardholder name field.
 */
fun HtmlInfo?.isCardholderNameField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARDHOLDER_NAME_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card number field.
 */
fun HtmlInfo?.isCardNumberField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_NUMBER_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration month field.
 */
fun HtmlInfo?.isCardExpirationMonthField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration year field.
 */
fun HtmlInfo?.isCardExpirationYearField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_YEAR_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration date field.
 */
fun HtmlInfo?.isCardExpirationDateField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_DATE_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card security code field.
 */
fun HtmlInfo?.isCardSecurityCodeField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_SECURITY_CODE_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card brand field.
 */
fun HtmlInfo?.isCardBrandField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_CARD_BRAND_HINTS)

/**
 * Whether this [HtmlInfo] represents an email field.
 */
fun HtmlInfo?.isEmailField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_EMAIL_HINTS)

/**
 * Whether this [HtmlInfo] represents a full person name field.
 */
fun HtmlInfo?.isPersonNameFullField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FULL_HINTS)

/**
 * Whether this [HtmlInfo] represents a person name prefix field.
 */
fun HtmlInfo?.isPersonNamePrefixField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_PREFIX_HINTS)

/**
 * Whether this [HtmlInfo] represents a given (first) name field.
 */
fun HtmlInfo?.isPersonNameGivenField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_GIVEN_HINTS)

/**
 * Whether this [HtmlInfo] represents a middle name field.
 */
fun HtmlInfo?.isPersonNameMiddleField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_MIDDLE_HINTS)

/**
 * Whether this [HtmlInfo] represents a family (last) name field.
 */
fun HtmlInfo?.isPersonNameFamilyField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PERSON_NAME_FAMILY_HINTS)

/**
 * Whether this [HtmlInfo] represents a full postal address field.
 */
fun HtmlInfo?.isPostalAddressFullField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_POSTAL_ADDRESS_FULL_HINTS)

/**
 * Whether this [HtmlInfo] represents a street address field.
 */
fun HtmlInfo?.isAddressStreetField(): Boolean = isInputField &&
    (
        hints().containsAnyTerms(SUPPORTED_RAW_ADDRESS_STREET_HINTS) ||
            hints().equalsAnyTerms(SUPPORTED_EXACT_ADDRESS_STREET_HINTS)
    )

/**
 * Whether this [HtmlInfo] represents an extended/secondary address (e.g. apartment, suite, unit)
 * field.
 */
fun HtmlInfo?.isAddressExtendedField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_ADDRESS_EXTENDED_HINTS)

/**
 * Whether this [HtmlInfo] represents a locality (city) field.
 */
fun HtmlInfo?.isAddressLocalityField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_ADDRESS_LOCALITY_HINTS)

/**
 * Whether this [HtmlInfo] represents a region (state/province) field.
 */
fun HtmlInfo?.isAddressRegionField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_ADDRESS_REGION_HINTS)

/**
 * Whether this [HtmlInfo] represents a country field.
 */
fun HtmlInfo?.isAddressCountryField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_ADDRESS_COUNTRY_HINTS)

/**
 * Whether this [HtmlInfo] represents a postal code field.
 */
fun HtmlInfo?.isPostalCodeField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_POSTAL_CODE_HINTS)

/**
 * Whether this [HtmlInfo] represents a phone number field.
 */
fun HtmlInfo?.isPhoneField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PHONE_HINTS)

/**
 * Whether this [HtmlInfo] represents a company field.
 */
fun HtmlInfo?.isCompanyField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_COMPANY_HINTS)

/**
 * Whether this [HtmlInfo] represents a social security number field.
 */
fun HtmlInfo?.isSsnField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_SSN_HINTS)

/**
 * Whether this [HtmlInfo] represents a passport number field.
 */
fun HtmlInfo?.isPassportNumberField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PASSPORT_HINTS)

/**
 * Whether this [HtmlInfo] represents a license number field.
 */
fun HtmlInfo?.isLicenseNumberField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_LICENSE_HINTS)

/**
 * Attributes that can be used as hints to determine the type of data the associated node expects.
 *
 * This function is untestable as [HtmlInfo] contains [android.util.Pair] which requires
 * instrumentation testing.
 *
 * @see IGNORED_RAW_HINTS
 * @see SUPPORTED_HTML_ATTRIBUTE_HINTS
 */
fun HtmlInfo?.hints(): List<String> = this
    ?.let { htmlInfo ->
        htmlInfo
            .attributes
            // Filter out attributes with null values or values that match ignored raw hints
            ?.filter { attribute ->
                attribute.second != null &&
                    !attribute.second.containsAnyTerms(IGNORED_RAW_HINTS)
            }
            // Filter attributes that match supported HTML attribute hints
            ?.filter { attribute ->
                attribute.first.containsAnyTerms(
                    terms = SUPPORTED_HTML_ATTRIBUTE_HINTS,
                    ignoreCase = true,
                )
            }
            .orEmpty()
            .mapNotNull { it.second }
    }
    .orEmpty()

/**
 * Whether this [HtmlInfo] represents an input field.
 */
val HtmlInfo?.isInputField: Boolean get() = this?.tag == "input"

/**
 * Whether this [HtmlInfo] matches the given [FillAssistRules.SelectorClause].
 *
 * This function is untestable as [HtmlInfo] contains [android.util.Pair] which requires
 * instrumentation testing.
 */
internal fun HtmlInfo.matchesSelectorClause(clause: FillAssistRules.SelectorClause): Boolean {
    // A clause with no usable constraint must not match every node with the same tag.
    if (clause.isUnconstrained) return false
    if (clause.tag != null && clause.tag != tag) return false
    val attrs = attributes ?: return clause.hasNoAttributeConstraints

    return matchesAttr(attrs, clause.id, HTML_ATTR_ID) &&
        matchesAttr(attrs, clause.name, HTML_ATTR_NAME) &&
        matchesAttr(attrs, clause.type, HTML_ATTR_TYPE) &&
        matchesAttr(attrs, clause.role, HTML_ATTR_ROLE)
}

/**
 * Whether this [FillAssistRules.SelectorClause] has no tag or attribute constraint, and would
 * therefore vacuously match every node if not explicitly rejected.
 */
private val FillAssistRules.SelectorClause.isUnconstrained: Boolean
    get() = tag == null && hasNoAttributeConstraints

/**
 * Whether this [FillAssistRules.SelectorClause] has no `id`/`name`/`type`/`role` constraint.
 */
private val FillAssistRules.SelectorClause.hasNoAttributeConstraints: Boolean
    get() = id == null && name == null && type == null && role == null

/**
 * Whether [value] is unconstrained, or [attrs] contains an attribute named [key] with [value].
 */
private fun matchesAttr(
    attrs: List<Pair<String, String>>,
    value: String?,
    key: String,
): Boolean = value == null || attrs.any { it.first == key && it.second == value }

/**
 * Checks if the list of strings contains any of the specified patterns.
 */
private fun List<String>.containsAnyPatterns(patterns: List<Regex>): Boolean = this
    .any { string -> patterns.any { pattern -> string.matches(pattern) } }

/**
 * Checks if the list of strings contains any of the specified terms.
 */
private fun List<String>.containsAnyTerms(terms: List<String>): Boolean =
    this.any { string ->
        string
            .toLowerCaseAndStripNonAlpha()
            .containsAnyTerms(terms)
    }

/**
 * Checks if any string in the list, once normalized, is exactly equal to one of the [terms]. Used
 * for terms too generic to match safely as a substring (e.g. "address" -- see
 * [SUPPORTED_EXACT_ADDRESS_STREET_HINTS]).
 */
private fun List<String>.equalsAnyTerms(terms: List<String>): Boolean =
    this.any { string -> string.toLowerCaseAndStripNonAlpha() in terms }

/**
 * The supported attribute keys whose value can represent an autofill hint.
 *
 * `autocomplete` is the standards-based signal both Chrome and Firefox populate directly with
 * tokens like `given-name`/`street-address`/`email` (see the WHATWG HTML spec) — more reliable
 * than guessing from `name`/`id`/`label` text, and browser-agnostic since it isn't tied to any
 * single browser's naming conventions.
 */
private val SUPPORTED_HTML_ATTRIBUTE_HINTS: List<String> = listOf(
    "name",
    "label",
    "type",
    "hint",
    "autofill",
    "autocomplete",
)
