package com.x8bit.bitwarden.ui.vault.model

import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
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
        value = R.string.january.dateText("01 - "),
        number = "1",
    ),
    FEBRUARY(
        value = R.string.february.dateText("02 - "),
        number = "2",
    ),
    MARCH(
        value = R.string.march.dateText("03 - "),
        number = "3",
    ),
    APRIL(
        value = R.string.april.dateText("04 - "),
        number = "4",
    ),
    MAY(
        value = R.string.may.dateText("05 - "),
        number = "5",
    ),
    JUNE(
        value = R.string.june.dateText("06 - "),
        number = "6",
    ),
    JULY(
        value = R.string.july.dateText("07 - "),
        number = "7",
    ),
    AUGUST(
        value = R.string.august.dateText("08 - "),
        number = "8",
    ),
    SEPTEMBER(
        value = R.string.september.dateText("09 - "),
        number = "9",
    ),
    OCTOBER(
        value = R.string.october.dateText("10 - "),
        number = "10",
    ),
    NOVEMBER(
        value = R.string.november.dateText("11 - "),
        number = "11",
    ),
    DECEMBER(
        value = R.string.december.dateText("12 - "),
        number = "12",
    ),
}

private fun @receiver:StringRes Int.dateText(prefix: String): Text =
    prefix
        .asText()
        .concat(asText())
