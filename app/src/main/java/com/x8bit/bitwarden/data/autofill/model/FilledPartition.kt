package com.x8bit.bitwarden.data.autofill.model

/**
 * All of the data required to build a `Dataset` for fulfilling a partition of data based on a
 * cipher.
 *
 * @param filledItems A filled copy of each view from this partition.
 */
data class FilledPartition(
    val filledItems: List<FilledItem>,
)
