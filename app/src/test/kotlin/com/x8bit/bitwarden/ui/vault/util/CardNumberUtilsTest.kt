package com.x8bit.bitwarden.ui.vault.util

import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardNumberUtilsTest {

    @Test
    fun `detectCardBrand should detect Visa`() {
        assertEquals(VaultCardBrand.VISA, "4111111111111111".detectCardBrand())
        assertEquals(VaultCardBrand.VISA, "4012888888881881".detectCardBrand())
    }

    @Test
    fun `detectCardBrand should detect Mastercard`() {
        assertEquals(
            VaultCardBrand.MASTERCARD,
            "5500000000000004".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.MASTERCARD,
            "5100000000000008".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.MASTERCARD,
            "2221000000000009".detectCardBrand(),
        )
    }

    @Test
    fun `detectCardBrand should detect Amex`() {
        assertEquals(VaultCardBrand.AMEX, "378282246310005".detectCardBrand())
        assertEquals(VaultCardBrand.AMEX, "341111111111111".detectCardBrand())
    }

    @Test
    fun `detectCardBrand should detect Discover`() {
        assertEquals(
            VaultCardBrand.DISCOVER,
            "6011111111111117".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.DISCOVER,
            "6500000000000002".detectCardBrand(),
        )
    }

    @Test
    fun `detectCardBrand should detect Diners Club`() {
        assertEquals(
            VaultCardBrand.DINERS_CLUB,
            "30569309025904".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.DINERS_CLUB,
            "36000000000008".detectCardBrand(),
        )
    }

    @Test
    fun `detectCardBrand should detect JCB`() {
        assertEquals(VaultCardBrand.JCB, "3528000000000007".detectCardBrand())
        assertEquals(VaultCardBrand.JCB, "3589000000000003".detectCardBrand())
    }

    @Test
    fun `detectCardBrand should detect Maestro`() {
        assertEquals(
            VaultCardBrand.MAESTRO,
            "5018000000000009".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.MAESTRO,
            "6304000000000000".detectCardBrand(),
        )
    }

    @Test
    fun `detectCardBrand should detect UnionPay`() {
        assertEquals(
            VaultCardBrand.UNIONPAY,
            "6200000000000005".detectCardBrand(),
        )
    }

    @Test
    fun `detectCardBrand should return OTHER for unknown prefixes`() {
        assertEquals(
            VaultCardBrand.OTHER,
            "9999999999999995".detectCardBrand(),
        )
        assertEquals(VaultCardBrand.OTHER, "".detectCardBrand())
    }
}
