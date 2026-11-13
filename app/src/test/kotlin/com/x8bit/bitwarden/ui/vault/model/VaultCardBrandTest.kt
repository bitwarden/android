package com.x8bit.bitwarden.ui.vault.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultCardBrandTest {

    @Test
    fun `findVaultCardBrandWithNameOrNull should return matching brand, regardless of format`() {
        val names = listOf(
            "UNIONpay",
            "AMEx",
            "diNERs  cLub",
            "rupay",
            "DinaCard",
            "diNA_card",
            "Mir",
            "m i r",
            "nothing card",
        )

        val result = names.map { it.findVaultCardBrandWithNameOrNull() }

        assertEquals(
            listOf(
                VaultCardBrand.UNIONPAY,
                VaultCardBrand.AMEX,
                VaultCardBrand.DINERS_CLUB,
                VaultCardBrand.RUPAY,
                VaultCardBrand.DINA_CARD,
                VaultCardBrand.DINA_CARD,
                VaultCardBrand.MIR,
                VaultCardBrand.MIR,
                null,
            ),
            result,
        )
    }
}
