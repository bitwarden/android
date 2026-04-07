package com.x8bit.bitwarden.data.autofill.util

import android.view.View
import android.view.autofill.AutofillValue
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull

/**
 * The android app URI scheme. Example: androidapp://com.x8bit.bitwarden
 */
private const val ANDROID_APP_SCHEME: String = "androidapp"

/**
 * Convert this [AutofillView] into a [FilledItem]. Return null if not possible.
 */
fun AutofillView.buildFilledItemOrNull(
    value: String,
): FilledItem? =
    // Do not try to autofill fields that are empty in the vault
    if (value.isEmpty()) {
        null
    } else {
        when (this.data.autofillType) {
            View.AUTOFILL_TYPE_DATE -> {
                value
                    .toLongOrNull()
                    ?.let { AutofillValue.forDate(it) }
            }

            View.AUTOFILL_TYPE_LIST -> this.buildListAutofillValueOrNull(value = value)
            View.AUTOFILL_TYPE_TEXT -> AutofillValue.forText(value)
            View.AUTOFILL_TYPE_TOGGLE -> {
                value
                    .toBooleanStrictOrNull()
                    ?.let { AutofillValue.forToggle(it) }
            }

            else -> null
        }
            ?.let { autofillValue ->
                FilledItem(
                    autofillId = this.data.autofillId,
                    value = autofillValue,
                )
            }
    }

/**
 * Build a list [AutofillValue] out of [value] or return null if not possible.
 */
@Suppress("MagicNumber")
private fun AutofillView.buildListAutofillValueOrNull(
    value: String,
): AutofillValue? =
    when (this) {
        is AutofillView.Card.ExpirationMonth -> {
            val autofillOptionsSize = this.data.autofillOptions.size
            // The idea here is that `value` is a numerical representation of a month.
            val monthIndex = value.toIntOrNull()
            when {
                monthIndex == null -> null
                // We expect there is some placeholder or empty space at the beginning of the list.
                autofillOptionsSize == 13 -> AutofillValue.forList(monthIndex)
                autofillOptionsSize >= monthIndex -> AutofillValue.forList(monthIndex - 1)
                else -> null
            }
        }

        is AutofillView.Card.ExpirationYear -> {
            val autofillOptions = this.data.autofillOptions
            autofillOptions
                .firstOrNull { it == value || it.takeLast(2) == value.takeLast(2) }
                ?.let { AutofillValue.forList(autofillOptions.indexOf(it)) }
        }

        is AutofillView.Card.Brand -> {
            value.findVaultCardBrandWithNameOrNull()
                ?.takeUnless { it == VaultCardBrand.SELECT }
                ?.let { vaultCardBrand ->
                    this.data.autofillOptions
                        .firstOrNull { it.findVaultCardBrandWithNameOrNull() == vaultCardBrand }
                        ?.let { AutofillValue.forList(this.data.autofillOptions.indexOf(it)) }
                }
        }

        is AutofillView.Card.CardholderName,
        is AutofillView.Card.ExpirationDate,
        is AutofillView.Card.Number,
        is AutofillView.Card.SecurityCode,
        is AutofillView.Login.Password,
        is AutofillView.Login.Username,
        is AutofillView.Unused,
            -> {
            this
                .data
                .autofillOptions
                .indexOfFirst { it == value }
                .takeIf { it != -1 }
                ?.let { AutofillValue.forList(it) }
        }
    }

/**
 * Try and build a URI. First, try building a website from the list of [ViewNodeTraversalData]. If
 * that fails, try converting [packageName] into an Android app URI.
 */
fun AutofillView.buildUriOrNull(
    packageName: String?,
): String? {
    // Search list of ViewNodeTraversalData for a website URI.
    this.data.website?.let { websiteUri -> return websiteUri }

    // If the package name is available, build a URI out of that.
    return packageName?.let { buildUri(domain = it, scheme = ANDROID_APP_SCHEME) }
}
