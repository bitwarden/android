package com.x8bit.bitwarden.data.autofill.util

import android.view.autofill.AutofillValue
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * Convert this [AutofillView] into a [FilledItem].
 */
fun AutofillView.buildFilledItemOrNull(
    value: String,
): FilledItem =
    // TODO: handle other autofill types (BIT-1457)
    FilledItem(
        autofillId = data.autofillId,
        value = AutofillValue.forText(value),
    )
