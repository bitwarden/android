package com.x8bit.bitwarden.ui.vault.model

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT

/**
 * Defines all available expiration month options for cards.
 */
enum class VaultCardExpirationMonth(
    val value: Text,
    val number: String,
) {
    SELECT(
        value = SELECT_TEXT,
        number = "0",
    ),
    JANUARY(
        value = BitwardenString.january.dateText("01 - "),
        number = "1",
    ),
    FEBRUARY(
        value = BitwardenString.february.dateText("02 - "),
        number = "2",
    ),
    MARCH(
        value = BitwardenString.march.dateText("03 - "),
        number = "3",
    ),
    APRIL(
        value = BitwardenString.april.dateText("04 - "),
        number = "4",
    ),
    MAY(
        value = BitwardenString.may.dateText("05 - "),
        number = "5",
    ),
    JUNE(
        value = BitwardenString.june.dateText("06 - "),
        number = "6",
    ),
    JULY(
        value = BitwardenString.july.dateText("07 - "),
        number = "7",
    ),
    AUGUST(
        value = BitwardenString.august.dateText("08 - "),
        number = "8",
    ),
    SEPTEMBER(
        value = BitwardenString.september.dateText("09 - "),
        number = "9",
    ),
    OCTOBER(
        value = BitwardenString.october.dateText("10 - "),
        number = "10",
    ),
    NOVEMBER(
        value = BitwardenString.november.dateText("11 - "),
        number = "11",
    ),
    DECEMBER(
        value = BitwardenString.december.dateText("12 - "),
        number = "12",
    ),
}

private fun @receiver:StringRes Int.dateText(prefix: String): Text =
    prefix
        .asText()
        .concat(asText())
