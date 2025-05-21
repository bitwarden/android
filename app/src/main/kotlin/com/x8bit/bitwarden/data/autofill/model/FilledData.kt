package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId
import android.widget.inline.InlinePresentationSpec

/**
 * A fulfilled autofill dataset. This is all of the data to fulfill each view of the autofill
 * request for a given cipher.
 */
data class FilledData(
    val filledPartitions: List<FilledPartition>,
    val ignoreAutofillIds: List<AutofillId>,
    val originalPartition: AutofillPartition,
    val uri: String?,
    val vaultItemInlinePresentationSpec: InlinePresentationSpec?,
    val isVaultLocked: Boolean,
)
