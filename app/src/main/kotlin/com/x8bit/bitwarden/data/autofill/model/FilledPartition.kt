package com.x8bit.bitwarden.data.autofill.model

import android.widget.inline.InlinePresentationSpec

/**
 * All of the data required to build a `Dataset` for fulfilling a partition of data based on an
 * [AutofillCipher].
 *
 * @param autofillCipher The cipher used to fulfill these [filledItems].
 * @param filledItems A filled copy of each view from this partition.
 * @param inlinePresentationSpec The spec for the inline presentation given one is expected.
 */
data class FilledPartition(
    val autofillCipher: AutofillCipher,
    val filledItems: List<FilledItem>,
    val inlinePresentationSpec: InlinePresentationSpec?,
)
