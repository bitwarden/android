package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.PortalUrlResponseJson
import com.bitwarden.network.service.BillingService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BillingRepositoryTest {

    private val billingService = mockk<BillingService>()
    private val repository = BillingRepositoryImpl(
        billingService = billingService,
    )

    @Test
    fun `getCheckoutSessionUrl when service returns success should return checkout URL`() =
        runTest {
            val expectedUrl =
                "https://checkout.stripe.com/c/pay/test_session_123"
            coEvery {
                billingService.createCheckoutSession()
            } returns Result.success(
                CheckoutSessionResponseJson(
                    checkoutSessionUrl = expectedUrl,
                ),
            )

            val result = repository.getCheckoutSessionUrl()

            assertEquals(Result.success(expectedUrl), result)
        }

    @Test
    fun `getCheckoutSessionUrl when service returns failure should return failure`() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                billingService.createCheckoutSession()
            } returns Result.failure(exception)

            val result = repository.getCheckoutSessionUrl()

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `getPortalUrl when service returns success should return portal URL`() =
        runTest {
            val expectedUrl =
                "https://billing.stripe.com/p/session/test_portal_456"
            coEvery {
                billingService.getPortalUrl()
            } returns Result.success(
                PortalUrlResponseJson(
                    url = expectedUrl,
                ),
            )

            val result = repository.getPortalUrl()

            assertEquals(Result.success(expectedUrl), result)
        }

    @Test
    fun `getPortalUrl when service returns failure should return failure`() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                billingService.getPortalUrl()
            } returns Result.failure(exception)

            val result = repository.getPortalUrl()

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
