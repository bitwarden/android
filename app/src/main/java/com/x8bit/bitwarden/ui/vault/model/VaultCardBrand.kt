package com.x8bit.bitwarden.ui.vault.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT

/**
 * Defines all available brand options for cards.
 */
enum class VaultCardBrand(val value: Text) {
    SELECT(value = SELECT_TEXT),
    VISA(value = "Visa".asText()),
    MASTERCARD(value = "Mastercard".asText()),
    AMERICAN_EXPRESS(value = "American Express".asText()),
    DISCOVER(value = "Discover".asText()),
    DINERS_CLUB(value = "Diners Club".asText()),
    JCB(value = "JCB".asText()),
    MAESTRO(value = "Maestro".asText()),
    UNIONPAY(value = "UnionPay".asText()),
    RUPAY(value = "RuPay".asText()),
    OTHER(value = R.string.other.asText()),
}
