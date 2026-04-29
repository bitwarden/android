package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.AuthenticatedBillingApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.BitwardenDiscountJson
import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.CartItemJson
import com.bitwarden.network.model.CartJson
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.DiscountTypeJson
import com.bitwarden.network.model.PasswordManagerCartItemsJson
import com.bitwarden.network.model.PortalUrlResponseJson
import com.bitwarden.network.model.PremiumPlanResponseJson
import com.bitwarden.network.model.StorageJson
import com.bitwarden.network.model.SubscriptionStatusJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.math.BigDecimal
import java.time.Instant

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

    @Test
    fun `getPremiumPlan when response is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val actual = service.getPremiumPlan()
            assertTrue(actual.isFailure)
        }

    @Test
    fun `getPremiumPlan when response is Success should return Success`() =
        runTest {
            val response = MockResponse()
                .setBody(PREMIUM_PLAN_RESPONSE_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getPremiumPlan()
            assertEquals(PREMIUM_PLAN_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `getSubscription when response is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertTrue(actual.isFailure)
        }

    @Test
    fun `getSubscription when response is Success should return Success`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(SUBSCRIPTION_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `getSubscription with monthly cadence should parse correctly`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_MONTHLY_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(
                CadenceTypeJson.MONTHLY,
                actual.getOrNull()?.cart?.cadence,
            )
        }

    @Test
    fun `getSubscription should parse every SubscriptionStatusJson value`() =
        runTest {
            SubscriptionStatusJson.entries.forEach { status ->
                val body = subscriptionResponseJsonForStatus(status)
                val response = MockResponse()
                    .setBody(body)
                    .setResponseCode(200)
                server.enqueue(response)
                val actual = service.getSubscription()
                assertEquals(status, actual.getOrNull()?.status)
            }
        }

    @Test
    fun `getSubscription with null storage and discount should parse`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_MINIMAL_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertTrue(actual.isSuccess)
        }

    @Test
    fun `getSubscription with AMOUNT_OFF discount should parse`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_AMOUNT_OFF_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(
                DiscountTypeJson.AMOUNT_OFF,
                actual.getOrNull()?.cart?.discount?.type,
            )
        }

    @Test
    fun `getSubscription with PERCENT_OFF discount should parse`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_PERCENT_OFF_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(
                DiscountTypeJson.PERCENT_OFF,
                actual.getOrNull()?.cart?.discount?.type,
            )
        }

    @Test
    fun `getSubscription with unknown cadence should fail deserialization`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_UNKNOWN_CADENCE_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertTrue(actual.isFailure)
            assertTrue(actual.exceptionOrNull() is SerializationException)
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

private const val PREMIUM_PLAN_RESPONSE_JSON = """
{
  "name": "Premium",
  "legacyYear": null,
  "available": true,
  "seat": {
    "stripePriceId": "premium-annually-2026",
    "price": 19.99,
    "provided": 0
  },
  "storage": {
    "stripePriceId": "personal-storage-gb-annually",
    "price": 4.00,
    "provided": 5
  }
}
"""

private val PREMIUM_PLAN_RESPONSE = PremiumPlanResponseJson(
    name = "Premium",
    legacyYear = null,
    isAvailable = true,
    seat = PremiumPlanResponseJson.PurchasableJson(
        stripePriceId = "premium-annually-2026",
        price = 19.99,
        provided = 0,
    ),
    storage = PremiumPlanResponseJson.PurchasableJson(
        stripePriceId = "personal-storage-gb-annually",
        price = 4.00,
        provided = 5,
    ),
)

private const val SUBSCRIPTION_RESPONSE_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 19.80,
        "discount": null
      },
      "additionalStorage": {
        "translationKey": "additionalStorage",
        "quantity": 24,
        "cost": 24.00,
        "discount": null
      }
    },
    "secretsManager": null,
    "cadence": "annually",
    "discount": {
      "type": "amount-off",
      "value": 2.10
    },
    "estimatedTax": 3.85
  },
  "storage": {
    "available": 5,
    "used": 0,
    "readableUsed": "0 Bytes"
  },
  "cancelAt": null,
  "canceled": null,
  "nextCharge": "2026-04-02T00:00:00Z",
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE = BitwardenSubscriptionResponseJson(
    status = SubscriptionStatusJson.ACTIVE,
    cart = CartJson(
        passwordManager = PasswordManagerCartItemsJson(
            seats = CartItemJson(
                translationKey = "premiumMembership",
                quantity = 1,
                cost = BigDecimal("19.80"),
                discount = null,
            ),
            additionalStorage = CartItemJson(
                translationKey = "additionalStorage",
                quantity = 24,
                cost = BigDecimal("24.00"),
                discount = null,
            ),
        ),
        secretsManager = null,
        cadence = CadenceTypeJson.ANNUALLY,
        discount = BitwardenDiscountJson(
            type = DiscountTypeJson.AMOUNT_OFF,
            value = BigDecimal("2.10"),
        ),
        estimatedTax = BigDecimal("3.85"),
    ),
    storage = StorageJson(
        available = 5,
        used = 0.0,
        readableUsed = "0 Bytes",
    ),
    cancelAt = null,
    canceled = null,
    nextCharge = Instant.parse("2026-04-02T00:00:00Z"),
    suspension = null,
    gracePeriod = null,
)

private const val SUBSCRIPTION_RESPONSE_MONTHLY_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 1.67,
        "discount": null
      },
      "additionalStorage": null
    },
    "secretsManager": null,
    "cadence": "monthly",
    "discount": null,
    "estimatedTax": 0
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "nextCharge": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private const val SUBSCRIPTION_RESPONSE_MINIMAL_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 19.80,
        "discount": null
      },
      "additionalStorage": null
    },
    "secretsManager": null,
    "cadence": "annually",
    "discount": null,
    "estimatedTax": 0
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "nextCharge": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private const val SUBSCRIPTION_RESPONSE_AMOUNT_OFF_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 19.80,
        "discount": null
      },
      "additionalStorage": null
    },
    "secretsManager": null,
    "cadence": "annually",
    "discount": {
      "type": "amount-off",
      "value": 5.00
    },
    "estimatedTax": 0
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "nextCharge": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private const val SUBSCRIPTION_RESPONSE_PERCENT_OFF_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 19.80,
        "discount": null
      },
      "additionalStorage": null
    },
    "secretsManager": null,
    "cadence": "annually",
    "discount": {
      "type": "percent-off",
      "value": 15.00
    },
    "estimatedTax": 0
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "nextCharge": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private const val SUBSCRIPTION_RESPONSE_UNKNOWN_CADENCE_JSON = """
{
  "status": "active",
  "cart": {
    "passwordManager": {
      "seats": {
        "translationKey": "premiumMembership",
        "quantity": 1,
        "cost": 19.80,
        "discount": null
      },
      "additionalStorage": null
    },
    "secretsManager": null,
    "cadence": "weekly",
    "discount": null,
    "estimatedTax": 0
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "nextCharge": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private fun subscriptionResponseJsonForStatus(
    status: SubscriptionStatusJson,
): String {
    val wireValue = when (status) {
        SubscriptionStatusJson.ACTIVE -> "active"
        SubscriptionStatusJson.CANCELED -> "canceled"
        SubscriptionStatusJson.PAST_DUE -> "past_due"
        SubscriptionStatusJson.INCOMPLETE -> "incomplete"
        SubscriptionStatusJson.INCOMPLETE_EXPIRED -> "incomplete_expired"
        SubscriptionStatusJson.UNPAID -> "unpaid"
        SubscriptionStatusJson.TRIALING -> "trialing"
        SubscriptionStatusJson.PAUSED -> "paused"
    }
    return """
    {
      "status": "$wireValue",
      "cart": {
        "passwordManager": {
          "seats": {
            "translationKey": "premiumMembership",
            "quantity": 1,
            "cost": 19.80,
            "discount": null
          },
          "additionalStorage": null
        },
        "secretsManager": null,
        "cadence": "annually",
        "discount": null,
        "estimatedTax": 0
      },
      "storage": null,
      "cancelAt": null,
      "canceled": null,
      "nextCharge": null,
      "suspension": null,
      "gracePeriod": null
    }
    """.trimIndent()
}
