package com.x8bit.bitwarden.data.billing.repository.util

import com.bitwarden.network.model.BitwardenDiscountJson
import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.CartItemJson
import com.bitwarden.network.model.CartJson
import com.bitwarden.network.model.DiscountTypeJson
import com.bitwarden.network.model.PasswordManagerCartItemsJson
import com.bitwarden.network.model.StorageJson
import com.bitwarden.network.model.SubscriptionStatusJson
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class BitwardenSubscriptionResponseJsonExtensionsTest {

    @Test
    fun `toSubscriptionInfo maps ACTIVE and TRIALING to ACTIVE`() {
        listOf(SubscriptionStatusJson.ACTIVE, SubscriptionStatusJson.TRIALING).forEach {
            val info = buildResponse(status = it).toSubscriptionInfo()
            assertEquals(PremiumSubscriptionStatus.ACTIVE, info.status)
        }
    }

    @Test
    fun `toSubscriptionInfo maps CANCELED and UNPAID to CANCELED`() {
        listOf(SubscriptionStatusJson.CANCELED, SubscriptionStatusJson.UNPAID).forEach {
            val info = buildResponse(status = it).toSubscriptionInfo()
            assertEquals(PremiumSubscriptionStatus.CANCELED, info.status)
        }
    }

    @Test
    fun `toSubscriptionInfo maps INCOMPLETE_EXPIRED to EXPIRED`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.INCOMPLETE_EXPIRED,
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.EXPIRED, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps INCOMPLETE to UPDATE_PAYMENT`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.INCOMPLETE,
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.UPDATE_PAYMENT, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps PAST_DUE to PAST_DUE`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.PAST_DUE,
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.PAST_DUE, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps PAUSED to PAUSED`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.PAUSED,
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.PAUSED, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps ACTIVE with cancelAt to PENDING_CANCELLATION`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.ACTIVE,
            cancelAt = Instant.parse("2026-05-01T00:00:00Z"),
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.PENDING_CANCELLATION, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps TRIALING with cancelAt to PENDING_CANCELLATION`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.TRIALING,
            cancelAt = Instant.parse("2026-05-01T00:00:00Z"),
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.PENDING_CANCELLATION, info.status)
    }

    @Test
    fun `toSubscriptionInfo passes cancelAt through to SubscriptionInfo`() {
        val cancelAt = Instant.parse("2026-05-01T00:00:00Z")
        val info = buildResponse(cancelAt = cancelAt).toSubscriptionInfo()
        assertEquals(cancelAt, info.cancelAt)
    }

    @Test
    fun `toSubscriptionInfo maps CANCELED with cancelAt still to CANCELED`() {
        val info = buildResponse(
            status = SubscriptionStatusJson.CANCELED,
            cancelAt = Instant.parse("2026-05-01T00:00:00Z"),
        ).toSubscriptionInfo()
        assertEquals(PremiumSubscriptionStatus.CANCELED, info.status)
    }

    @Test
    fun `toSubscriptionInfo maps cadence to PlanCadence`() {
        val annually = buildResponse(cadence = CadenceTypeJson.ANNUALLY).toSubscriptionInfo()
        assertEquals(PlanCadence.ANNUALLY, annually.cadence)

        val monthly = buildResponse(cadence = CadenceTypeJson.MONTHLY).toSubscriptionInfo()
        assertEquals(PlanCadence.MONTHLY, monthly.cadence)
    }

    @Test
    fun `toSubscriptionInfo maps seatsCost and null storageCost when not present`() {
        val info = buildResponse(seatsCost = BigDecimal("19.80")).toSubscriptionInfo()
        assertEquals(BigDecimal("19.80"), info.seatsCost)
        assertNull(info.storageCost)
    }

    @Test
    fun `toSubscriptionInfo maps storageCost from additionalStorage when present`() {
        val info = buildResponse(
            seatsCost = BigDecimal("19.80"),
            storageCost = BigDecimal("24.00"),
        ).toSubscriptionInfo()
        assertEquals(BigDecimal("24.00"), info.storageCost)
    }

    @Test
    fun `toSubscriptionInfo discountAmount is null when no discount`() {
        val info = buildResponse(discount = null).toSubscriptionInfo()
        assertNull(info.discountAmount)
    }

    @Test
    fun `toSubscriptionInfo discountAmount for AMOUNT_OFF passes value through`() {
        val info = buildResponse(
            discount = BitwardenDiscountJson(
                type = DiscountTypeJson.AMOUNT_OFF,
                value = BigDecimal("2.10"),
            ),
        ).toSubscriptionInfo()
        assertEquals(BigDecimal("2.10"), info.discountAmount)
    }

    @Test
    fun `toSubscriptionInfo discountAmount for PERCENT_OFF applies to PM subtotal`() {
        // seats 20 + storage 10 = 30 subtotal, 15% = 4.50
        val info = buildResponse(
            seatsCost = BigDecimal("20.00"),
            storageCost = BigDecimal("10.00"),
            discount = BitwardenDiscountJson(
                type = DiscountTypeJson.PERCENT_OFF,
                value = BigDecimal("15.00"),
            ),
        ).toSubscriptionInfo()
        assertEquals(BigDecimal("4.50"), info.discountAmount)
    }

    @Test
    fun `toSubscriptionInfo passes estimatedTax through`() {
        val info = buildResponse(estimatedTax = BigDecimal("3.85")).toSubscriptionInfo()
        assertEquals(BigDecimal("3.85"), info.estimatedTax)
    }

    @Test
    fun `toSubscriptionInfo nextChargeTotal sums line items, subtracts discount, adds tax`() {
        // Matches the design example: 19.80 + 24.00 - 2.10 + 3.85 = 45.55
        val info = buildResponse(
            seatsCost = BigDecimal("19.80"),
            storageCost = BigDecimal("24.00"),
            discount = BitwardenDiscountJson(
                type = DiscountTypeJson.AMOUNT_OFF,
                value = BigDecimal("2.10"),
            ),
            estimatedTax = BigDecimal("3.85"),
        ).toSubscriptionInfo()
        assertEquals(BigDecimal("45.55"), info.nextChargeTotal)
    }

    @Test
    fun `toSubscriptionInfo nextChargeTotal with minimal cart equals seatsCost`() {
        // User-provided JSON: 19.80 + 0 - 0 + 0 = 19.80
        val info = buildResponse(seatsCost = BigDecimal("19.80")).toSubscriptionInfo()
        assertEquals(BigDecimal("19.80"), info.nextChargeTotal)
    }

    @Test
    fun `toSubscriptionInfo maps timestamps and gracePeriod`() {
        val canceled = Instant.parse("2026-01-01T00:00:00Z")
        val next = Instant.parse("2027-04-21T17:35:42Z")
        val suspension = Instant.parse("2026-05-02T00:00:00Z")
        val info = buildResponse(
            canceled = canceled,
            nextCharge = next,
            suspension = suspension,
            gracePeriod = 14,
        ).toSubscriptionInfo()
        assertEquals(canceled, info.canceledDate)
        assertEquals(next, info.nextCharge)
        assertEquals(suspension, info.suspensionDate)
        assertEquals(14, info.gracePeriodDays)
    }

    @Test
    fun `toSubscriptionInfo has null timestamps and gracePeriod when not provided`() {
        val info = buildResponse().toSubscriptionInfo()
        assertNull(info.cancelAt)
        assertNull(info.canceledDate)
        assertNull(info.nextCharge)
        assertNull(info.suspensionDate)
        assertNull(info.gracePeriodDays)
    }

    @Suppress("LongParameterList")
    private fun buildResponse(
        status: SubscriptionStatusJson = SubscriptionStatusJson.ACTIVE,
        cadence: CadenceTypeJson = CadenceTypeJson.ANNUALLY,
        seatsCost: BigDecimal = BigDecimal("19.80"),
        storageCost: BigDecimal? = null,
        discount: BitwardenDiscountJson? = null,
        estimatedTax: BigDecimal = BigDecimal.ZERO,
        storage: StorageJson? = null,
        cancelAt: Instant? = null,
        canceled: Instant? = null,
        nextCharge: Instant? = null,
        suspension: Instant? = null,
        gracePeriod: Int? = null,
    ): BitwardenSubscriptionResponseJson = BitwardenSubscriptionResponseJson(
        status = status,
        cart = CartJson(
            passwordManager = PasswordManagerCartItemsJson(
                seats = CartItemJson(
                    translationKey = "premiumMembership",
                    quantity = 1,
                    cost = seatsCost,
                    discount = null,
                ),
                additionalStorage = storageCost?.let {
                    CartItemJson(
                        translationKey = "additionalStorage",
                        quantity = 1,
                        cost = it,
                        discount = null,
                    )
                },
            ),
            secretsManager = null,
            cadence = cadence,
            discount = discount,
            estimatedTax = estimatedTax,
        ),
        storage = storage,
        cancelAt = cancelAt,
        canceled = canceled,
        nextCharge = nextCharge,
        suspension = suspension,
        gracePeriod = gracePeriod,
    )
}
