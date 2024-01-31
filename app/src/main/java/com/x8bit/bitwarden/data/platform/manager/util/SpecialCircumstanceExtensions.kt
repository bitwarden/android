package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance

/**
 * Returns [AutofillSelectionData] when contained in the given [SpecialCircumstance].
 */
fun SpecialCircumstance.toAutofillSelectionDataOrNull(): AutofillSelectionData? =
    when (this) {
        is SpecialCircumstance.AutofillSave -> null
        is SpecialCircumstance.AutofillSelection -> this.autofillSelectionData
        is SpecialCircumstance.ShareNewSend -> null
    }
