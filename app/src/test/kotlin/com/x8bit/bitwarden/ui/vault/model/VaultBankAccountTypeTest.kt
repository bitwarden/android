package com.x8bit.bitwarden.ui.vault.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultBankAccountTypeTest {

    @Test
    fun `each entry should expose the expected server value`() {
        val expected = listOf(
            VaultBankAccountType.SELECT to "select",
            VaultBankAccountType.CHECKING to "checking",
            VaultBankAccountType.SAVINGS to "savings",
            VaultBankAccountType.CERTIFICATE_OF_DEPOSIT to "certificateOfDeposit",
            VaultBankAccountType.LINE_OF_CREDIT to "lineOfCredit",
            VaultBankAccountType.INVESTMENT_BROKERAGE to "investmentBrokerage",
            VaultBankAccountType.MONEY_MARKET to "moneyMarket",
            VaultBankAccountType.OTHER to "other",
        )

        val actual = VaultBankAccountType.entries.map { it to it.value }

        assertEquals(expected, actual)
    }

    @Test
    fun `parse should return matching entry for exact value`() {
        VaultBankAccountType.entries.forEach { entry ->
            assertEquals(entry, VaultBankAccountType.parse(entry.value))
        }
    }

    @Test
    fun `parse should return matching entry regardless of casing`() {
        assertEquals(
            VaultBankAccountType.CHECKING,
            VaultBankAccountType.parse("CHECKING"),
        )
        assertEquals(
            VaultBankAccountType.CERTIFICATE_OF_DEPOSIT,
            VaultBankAccountType.parse("CertificateOfDeposit"),
        )
        assertEquals(
            VaultBankAccountType.MONEY_MARKET,
            VaultBankAccountType.parse("MONEYmarket"),
        )
    }

    @Test
    fun `parse should fall back to OTHER for null input`() {
        assertEquals(VaultBankAccountType.OTHER, VaultBankAccountType.parse(null))
    }

    @Test
    fun `parse should fall back to OTHER for an unrecognized value`() {
        assertEquals(
            VaultBankAccountType.OTHER,
            VaultBankAccountType.parse("not-a-real-account-type"),
        )
    }

    @Test
    fun `parse should fall back to OTHER for a blank value`() {
        assertEquals(VaultBankAccountType.OTHER, VaultBankAccountType.parse(""))
    }
}
