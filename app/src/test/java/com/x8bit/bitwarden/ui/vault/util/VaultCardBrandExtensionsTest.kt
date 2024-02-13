package com.x8bit.bitwarden.ui.vault.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultCardBrandExtensionsTest {

    @Test
    fun `longName should return the correct value for each VaultCardBrand`() {
        mapOf(
            VaultCardBrand.SELECT to SELECT_TEXT,
            VaultCardBrand.VISA to "Visa".asText(),
            VaultCardBrand.MASTERCARD to "Mastercard".asText(),
            VaultCardBrand.AMEX to "American Express".asText(),
            VaultCardBrand.DISCOVER to "Discover".asText(),
            VaultCardBrand.DINERS_CLUB to "Diners Club".asText(),
            VaultCardBrand.JCB to "JCB".asText(),
            VaultCardBrand.MAESTRO to "Maestro".asText(),
            VaultCardBrand.UNIONPAY to "UnionPay".asText(),
            VaultCardBrand.RUPAY to "RuPay".asText(),
            VaultCardBrand.OTHER to R.string.other.asText(),
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
            VaultCardBrand.SELECT to SELECT_TEXT,
            VaultCardBrand.VISA to "Visa".asText(),
            VaultCardBrand.MASTERCARD to "Mastercard".asText(),
            VaultCardBrand.AMEX to "Amex".asText(),
            VaultCardBrand.DISCOVER to "Discover".asText(),
            VaultCardBrand.DINERS_CLUB to "Diners Club".asText(),
            VaultCardBrand.JCB to "JCB".asText(),
            VaultCardBrand.MAESTRO to "Maestro".asText(),
            VaultCardBrand.UNIONPAY to "UnionPay".asText(),
            VaultCardBrand.RUPAY to "RuPay".asText(),
            VaultCardBrand.OTHER to R.string.other.asText(),
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
            VaultCardBrand.DISCOVER to "Discover",
            VaultCardBrand.DINERS_CLUB to "Diners Club",
            VaultCardBrand.JCB to "JCB",
            VaultCardBrand.MAESTRO to "Maestro",
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
