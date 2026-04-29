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
    fun `detectCardBrand should detect RuPay`() {
        assertEquals(
            VaultCardBrand.RUPAY,
            "8100000000000005".detectCardBrand(),
        )
        assertEquals(
            VaultCardBrand.RUPAY,
            "8200000000000004".detectCardBrand(),
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

    @Test
    fun `formatCardNumber should format Amex correctly`() {
        assertEquals("3782 822463 10005", "378282246310005".formatCardNumber())
        assertEquals("3411 111111 11111", "341111111111111".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Diners Club 14 digits correctly`() {
        assertEquals("3056 930902 5904", "30569309025904".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Diners Club with non 14 digits as default`() {
        assertEquals("3600 0000 0000 0084", "3600000000000084".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Maestro 13 digits correctly`() {
        assertEquals("5018 5753 94858", "5018575394858".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Maestro 15 digits correctly`() {
        assertEquals("5018 575394 85843", "501857539485843".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Maestro 19 digits correctly`() {
        assertEquals("5018 5753 9485 8437 306", "5018575394858437306".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format Maestro other digits as default`() {
        assertEquals("5018 5753 9485 8437", "5018575394858437".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format UnionPay 19 digits correctly`() {
        assertEquals("622795 5237950556428", "6227955237950556428".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format UnionPay with non 19 digits as default`() {
        assertEquals("6227 9552 3795 0556", "6227955237950556".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format standard card numbers in groups of 4`() {
        assertEquals("4111 1111 1111 1111", "4111111111111111".formatCardNumber())
        assertEquals("5500 0000 0000 0004", "5500000000000004".formatCardNumber())
        assertEquals("6011 1111 1111 1117", "6011111111111117".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should format card numbers in 4 groups of 4 and append remainder`() {
        assertEquals("4111 1111 1111 1111 1", "41111111111111111".formatCardNumber())
        assertEquals("5500 0000 0000 0004 0000", "55000000000000040000".formatCardNumber())
        assertEquals("6011 1111 1111 1117 11111", "601111111111111711111".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should keep short standard card numbers unchanged`() {
        assertEquals("123", "123".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should return empty string for empty input`() {
        assertEquals("", "".formatCardNumber())
    }

    @Test
    fun `formatCardNumber should sanitize and return empty when input has no digits`() {
        assertEquals("----", "----".formatCardNumber())
    }
}
