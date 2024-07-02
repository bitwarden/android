package com.x8bit.bitwarden.data.autofill.util

import android.view.autofill.AutofillValue

/**
 * Extract a month value from this [AutofillValue].
 */
@Suppress("MagicNumber")
fun AutofillValue.extractMonthValue(
    autofillOptions: List<String>,
): String? =
    when {
        this.isList && autofillOptions.size == 13 -> {
            this.listValue.toString()
        }

        this.isList && autofillOptions.size == 12 -> {
            (this.listValue + 1).toString()
        }

        this.isText -> this.textValue.toString()

        else -> null
    }

/**
 * Extract a text value from this [AutofillValue].
 */
fun AutofillValue.extractTextValue(): String? =
    if (this.isText) {
        this
            .textValue
            .takeIf { it.isNotBlank() }
            ?.toString()
    } else {
        null
    }
