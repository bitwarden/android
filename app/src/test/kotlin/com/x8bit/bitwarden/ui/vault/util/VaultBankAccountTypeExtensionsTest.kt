package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT
import com.x8bit.bitwarden.ui.vault.model.VaultBankAccountType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultBankAccountTypeExtensionsTest {

    @Test
    fun `longName should return the correct value for each VaultBankAccountType`() {
        mapOf(
            VaultBankAccountType.SELECT to SELECT_TEXT,
            VaultBankAccountType.CHECKING to BitwardenString.checking.asText(),
            VaultBankAccountType.SAVINGS to BitwardenString.savings.asText(),
            VaultBankAccountType.CERTIFICATE_OF_DEPOSIT to
                BitwardenString.certificate_of_deposit.asText(),
            VaultBankAccountType.LINE_OF_CREDIT to BitwardenString.line_of_credit.asText(),
            VaultBankAccountType.INVESTMENT_BROKERAGE to
                BitwardenString.investment_brokerage.asText(),
            VaultBankAccountType.MONEY_MARKET to BitwardenString.money_market.asText(),
            VaultBankAccountType.OTHER to BitwardenString.other.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(label, type.longName)
            }
    }

    @Test
    fun `stringNameOrNull should return the correct value for each VaultBankAccountType`() {
        mapOf(
            VaultBankAccountType.SELECT to null,
            VaultBankAccountType.CHECKING to "Checking",
            VaultBankAccountType.SAVINGS to "Savings",
            VaultBankAccountType.CERTIFICATE_OF_DEPOSIT to "Certificate of deposit",
            VaultBankAccountType.LINE_OF_CREDIT to "Line of credit",
            VaultBankAccountType.INVESTMENT_BROKERAGE to "Investment/brokerage",
            VaultBankAccountType.MONEY_MARKET to "Money market",
            VaultBankAccountType.OTHER to "Other",
        )
            .forEach { (type, label) ->
                assertEquals(label, type.stringNameOrNull)
            }
    }
}
