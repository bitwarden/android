package com.x8bit.bitwarden.ui.vault.model

/**
 * Defines all available account type options for bank accounts.
 */
enum class VaultBankAccountType {
    SELECT,
    CHECKING,
    SAVINGS,
    CERTIFICATE_OF_DEPOSIT,
    LINE_OF_CREDIT,
    INVESTMENT_BROKERAGE,
    MONEY_MARKET,
    OTHER,
}

/**
 * Returns a [VaultBankAccountType] with the provided [String] or null.
 */
fun String.findVaultBankAccountTypeWithNameOrNull(): VaultBankAccountType? =
    VaultBankAccountType
        .entries
        .find { vaultBankAccountType ->
            vaultBankAccountType.name.lowercaseWithoutSpacesOrUnderscores ==
                this.lowercaseWithoutSpacesOrUnderscores
        }

private val String.lowercaseWithoutSpacesOrUnderscores: String
    get() = lowercase()
        .replace(" ", "")
        .replace("_", "")
