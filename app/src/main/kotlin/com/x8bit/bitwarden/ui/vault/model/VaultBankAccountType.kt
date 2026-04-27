package com.x8bit.bitwarden.ui.vault.model

/**
 * Defines all available account type options for bank accounts. The [value] corresponds to the
 * string representation used in the server contract, with the exception of [SELECT], which is a
 * UI-only placeholder representing no selection.
 */
enum class VaultBankAccountType(val value: String) {
    SELECT(value = "select"),
    CHECKING(value = "checking"),
    SAVINGS(value = "savings"),
    CERTIFICATE_OF_DEPOSIT(value = "certificateOfDeposit"),
    LINE_OF_CREDIT(value = "lineOfCredit"),
    INVESTMENT_BROKERAGE(value = "investmentBrokerage"),
    MONEY_MARKET(value = "moneyMarket"),
    OTHER(value = "other"),
    ;

    /**
     * Companion object for [VaultBankAccountType] that exposes parsing utilities.
     */
    companion object {
        /**
         * Returns the [VaultBankAccountType] matching the provided [value] (case-insensitive),
         * falling back to [OTHER] if no match is found.
         */
        fun parse(value: String?): VaultBankAccountType =
            entries
                .firstOrNull { it.value.equals(other = value, ignoreCase = true) }
                ?: OTHER
    }
}
