package com.x8bit.bitwarden.data.autofill.util

import android.view.View
import android.view.autofill.AutofillValue
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * Convert this [AutofillView] into a [FilledItem]. Return null if not possible.
 */
fun AutofillView.buildFilledItemOrNull(
    value: String,
): FilledItem? =
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

/**
 * Build a list [AutofillValue] out of [value] or return null if not possible.
 */
@Suppress("MagicNumber")
private fun AutofillView.buildListAutofillValueOrNull(
    value: String,
): AutofillValue? =
    if (this is AutofillView.Card.ExpirationMonth) {
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
    } else {
        this
            .data
            .autofillOptions
            .indexOfFirst { it == value }
            .takeIf { it != -1 }
            ?.let { AutofillValue.forList(it) }
    }
