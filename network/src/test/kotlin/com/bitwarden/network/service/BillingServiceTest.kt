package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.AuthenticatedBillingApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.PortalUrlResponseJson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class BillingServiceTest : BaseServiceTest() {

    private val billingApi: AuthenticatedBillingApi = retrofit.create()
    private val service = BillingServiceImpl(
        authenticatedBillingApi = billingApi,
    )

    @Test
    fun `createCheckoutSession when response is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val actual = service.createCheckoutSession()
            assertTrue(actual.isFailure)
        }

    @Test
    fun `createCheckoutSession when response is Success should return Success`() =
        runTest {
            val response = MockResponse()
                .setBody(CHECKOUT_SESSION_RESPONSE_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.createCheckoutSession()
            assertEquals(CHECKOUT_SESSION_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `getPortalUrl when response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.getPortalUrl()
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getPortalUrl when response is Success should return Success`() = runTest {
        val response = MockResponse()
            .setBody(PORTAL_URL_RESPONSE_JSON)
            .setResponseCode(200)
        server.enqueue(response)
        val actual = service.getPortalUrl()
        assertEquals(PORTAL_URL_RESPONSE.asSuccess(), actual)
    }
}

private const val CHECKOUT_SESSION_RESPONSE_JSON = """
{
  "checkoutSessionUrl": "https://checkout.stripe.com/c/pay/test_session_123"
}
"""

private val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseJson(
    checkoutSessionUrl = "https://checkout.stripe.com/c/pay/test_session_123",
)

private const val PORTAL_URL_RESPONSE_JSON = """
{
  "url": "https://billing.stripe.com/p/session/test_portal_456"
}
"""

private val PORTAL_URL_RESPONSE = PortalUrlResponseJson(
    url = "https://billing.stripe.com/p/session/test_portal_456",
)
