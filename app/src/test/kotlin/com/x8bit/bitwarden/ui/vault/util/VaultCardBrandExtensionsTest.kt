package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultCardBrandExtensionsTest {

    @Test
    fun `longName should return the correct value for each VaultCardBrand`() {
        mapOf(
            VaultCardBrand.SELECT to BitwardenString.select.asText(),
            VaultCardBrand.VISA to "Visa".asText(),
            VaultCardBrand.MASTERCARD to "Mastercard".asText(),
            VaultCardBrand.AMEX to "American Express".asText(),
            VaultCardBrand.DINA_CARD to "DinaCard".asText(),
            VaultCardBrand.DISCOVER to "Discover".asText(),
            VaultCardBrand.DINERS_CLUB to "Diners Club".asText(),
            VaultCardBrand.JCB to "JCB".asText(),
            VaultCardBrand.MAESTRO to "Maestro".asText(),
            VaultCardBrand.MIR to "Mir".asText(),
            VaultCardBrand.UNIONPAY to "UnionPay".asText(),
            VaultCardBrand.RUPAY to "RuPay".asText(),
            VaultCardBrand.OTHER to BitwardenString.other.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.longName,
                )
            }
    }

    @Test
    fun `shortName should return the correct value for each VaultCardBrand`() {
        mapOf(
            VaultCardBrand.SELECT to BitwardenString.select.asText(),
            VaultCardBrand.VISA to "Visa".asText(),
            VaultCardBrand.MASTERCARD to "Mastercard".asText(),
            VaultCardBrand.AMEX to "Amex".asText(),
            VaultCardBrand.DINA_CARD to "DinaCard".asText(),
            VaultCardBrand.DISCOVER to "Discover".asText(),
            VaultCardBrand.DINERS_CLUB to "Diners Club".asText(),
            VaultCardBrand.JCB to "JCB".asText(),
            VaultCardBrand.MAESTRO to "Maestro".asText(),
            VaultCardBrand.MIR to "Mir".asText(),
            VaultCardBrand.UNIONPAY to "UnionPay".asText(),
            VaultCardBrand.RUPAY to "RuPay".asText(),
            VaultCardBrand.OTHER to BitwardenString.other.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.shortName,
                )
            }
    }

    @Test
    fun `stringLongNameOrNull should return the correct value for each VaultCardBrand`() {
        mapOf(
            VaultCardBrand.SELECT to null,
            VaultCardBrand.VISA to "Visa",
            VaultCardBrand.MASTERCARD to "Mastercard",
            VaultCardBrand.AMEX to "Amex",
            VaultCardBrand.DINA_CARD to "DinaCard",
            VaultCardBrand.DISCOVER to "Discover",
            VaultCardBrand.DINERS_CLUB to "Diners Club",
            VaultCardBrand.JCB to "JCB",
            VaultCardBrand.MAESTRO to "Maestro",
            VaultCardBrand.MIR to "Mir",
            VaultCardBrand.UNIONPAY to "UnionPay",
            VaultCardBrand.RUPAY to "RuPay",
            VaultCardBrand.OTHER to "Other",
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.stringLongNameOrNull,
                )
            }
    }
}
