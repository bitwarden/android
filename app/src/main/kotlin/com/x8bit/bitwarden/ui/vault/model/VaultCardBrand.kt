package com.x8bit.bitwarden.ui.vault.model

/**
 * Defines all available brand options for cards.
 */
enum class VaultCardBrand {
    SELECT,
    VISA,
    MASTERCARD,
    AMEX,
    DISCOVER,
    DINERS_CLUB,
    JCB,
    MAESTRO,
    UNIONPAY,
    RUPAY,
    OTHER,
}

/**
 * Returns a [VaultCardBrand] with the provided [String] or null.
 */
fun String.findVaultCardBrandWithNameOrNull(): VaultCardBrand? =
    VaultCardBrand
        .entries
        .find { vaultCardBrand ->
            vaultCardBrand.name.lowercaseWithoutSpacesOrUnderScores ==
                this.lowercaseWithoutSpacesOrUnderScores
        }

private val String.lowercaseWithoutSpacesOrUnderScores: String
    get() = lowercase()
        .replace(" ", "")
        .replace("_", "")
