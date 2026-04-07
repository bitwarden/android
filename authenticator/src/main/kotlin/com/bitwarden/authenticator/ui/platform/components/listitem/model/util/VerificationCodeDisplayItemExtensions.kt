package com.bitwarden.authenticator.ui.platform.components.listitem.model.util

import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.core.data.repository.util.SpecialCharWithPrecedenceComparator

/**
 * Sorts the data in alphabetical order by name. Using lexicographical sorting but giving
 * precedence to special characters over letters and digits.
 */
fun List<VerificationCodeDisplayItem>.sortAlphabetically(): List<VerificationCodeDisplayItem> =
    this.sortedWith(
        comparator = { item1, item2 ->
            SpecialCharWithPrecedenceComparator.compare(item1.title, item2.title)
        },
    )
