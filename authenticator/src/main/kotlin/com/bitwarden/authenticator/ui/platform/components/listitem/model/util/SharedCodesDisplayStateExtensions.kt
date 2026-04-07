package com.bitwarden.authenticator.ui.platform.components.listitem.model.util

import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState.SharedCodesAccountSection
import com.bitwarden.core.data.repository.util.SpecialCharWithPrecedenceComparator

/**
 * Sorts the data in alphabetical order by name. Using lexicographical sorting but giving
 * precedence to special characters over letters and digits.
 */
fun List<SharedCodesAccountSection>.sortAlphabetically(): List<SharedCodesAccountSection> =
    this.sortedWith(
        comparator = { item1, item2 ->
            SpecialCharWithPrecedenceComparator.compare(item1.sortKey, item2.sortKey)
        },
    )
