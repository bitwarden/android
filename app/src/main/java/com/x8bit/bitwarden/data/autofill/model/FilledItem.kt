package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * A fulfilled autofill view. This contains everything required to build the autofill UI
 * representing this item.
 */
data class FilledItem(
    val autofillId: AutofillId,
)
