package com.x8bit.bitwarden.data.autofill.model

/**
 * All of the data required to build a `Dataset` for fulfilling a partition of data based on an
 * [AutofillCipher].
 *
 * @param autofillCipher The cipher used to fulfill these [filledItems].
 * @param filledItems A filled copy of each view from this partition.
 */
data class FilledPartition(
    val autofillCipher: AutofillCipher,
    val filledItems: List<FilledItem>,
)
