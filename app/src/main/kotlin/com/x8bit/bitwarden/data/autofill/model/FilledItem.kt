package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue

/**
 * A fulfilled autofill view. This contains everything required to build the autofill UI
 * representing this item.
 */
data class FilledItem(
    val autofillId: AutofillId,
    val value: AutofillValue,
)
