package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT
import com.x8bit.bitwarden.ui.vault.model.VaultBankAccountType

/**
 * Helper that exposes the display name [Text] for a [VaultBankAccountType].
 */
val VaultBankAccountType.longName: Text
    get() = when (this) {
        VaultBankAccountType.SELECT -> SELECT_TEXT
        VaultBankAccountType.CHECKING -> BitwardenString.checking.asText()
        VaultBankAccountType.SAVINGS -> BitwardenString.savings.asText()
        VaultBankAccountType.CERTIFICATE_OF_DEPOSIT -> {
            BitwardenString.certificate_of_deposit.asText()
        }

        VaultBankAccountType.LINE_OF_CREDIT -> BitwardenString.line_of_credit.asText()
        VaultBankAccountType.INVESTMENT_BROKERAGE -> {
            BitwardenString.investment_brokerage.asText()
        }

        VaultBankAccountType.MONEY_MARKET -> BitwardenString.money_market.asText()
        VaultBankAccountType.OTHER -> BitwardenString.other.asText()
    }
