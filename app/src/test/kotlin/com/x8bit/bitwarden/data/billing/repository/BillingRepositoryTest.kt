package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.PortalUrlResponseJson
import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BillingRepositoryTest {

    private val billingService = mockk<BillingService>()
    private val repository = BillingRepositoryImpl(
        billingService = billingService,
    )

    @Test
    fun `getCheckoutSessionUrl when service returns success should return Success`() =
        runTest {
            val expectedUrl =
                "https://checkout.stripe.com/c/pay/test_session_123"
            coEvery {
                billingService.createCheckoutSession()
            } returns CheckoutSessionResponseJson(checkoutSessionUrl = expectedUrl).asSuccess()

            val result = repository.getCheckoutSessionUrl()

            assertEquals(CheckoutSessionResult.Success(url = expectedUrl), result)
        }

    @Test
    fun `getCheckoutSessionUrl when service returns failure should return Error`() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                billingService.createCheckoutSession()
            } returns exception.asFailure()

            val result = repository.getCheckoutSessionUrl()

            assertEquals(
                CheckoutSessionResult.Error(error = exception),
                result,
            )
        }

    @Test
    fun `getPortalUrl when service returns success should return Success`() =
        runTest {
            val expectedUrl =
                "https://billing.stripe.com/p/session/test_portal_456"
            coEvery {
                billingService.getPortalUrl()
            } returns PortalUrlResponseJson(url = expectedUrl).asSuccess()

            val result = repository.getPortalUrl()

            assertEquals(CustomerPortalResult.Success(url = expectedUrl), result)
        }

    @Test
    fun `getPortalUrl when service returns failure should return Error`() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                billingService.getPortalUrl()
            } returns exception.asFailure()

            val result = repository.getPortalUrl()

            assertEquals(
                CustomerPortalResult.Error(error = exception),
                result,
            )
        }
}
