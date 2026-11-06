package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.AuthenticatedBillingApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.DiscountTypeJson
import com.bitwarden.network.model.GetSubscriptionResponse
import com.bitwarden.network.model.InvoicePreviewDiscountJson
import com.bitwarden.network.model.InvoicePreviewItemJson
import com.bitwarden.network.model.InvoicePreviewJson
import com.bitwarden.network.model.PasswordManagerInvoicePreviewJson
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
    fun `getSubscription when response is non-404 Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertTrue(actual.isFailure)
        }

    @Test
    fun `getSubscription when response is 404 should return NotFound`() =
        runTest {
            val response = MockResponse().setResponseCode(404)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(GetSubscriptionResponse.NotFound.asSuccess(), actual)
        }

    @Test
    fun `getSubscription when response is Success should return Success`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(
                GetSubscriptionResponse.Success(subscription = SUBSCRIPTION_RESPONSE).asSuccess(),
                actual,
            )
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
                GetSubscriptionResponse.Success(
                    subscription = SUBSCRIPTION_RESPONSE_MONTHLY,
                ).asSuccess(),
                actual,
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
                assertEquals(
                    status,
                    (actual.getOrNull() as? GetSubscriptionResponse.Success)
                        ?.subscription
                        ?.status,
                )
            }
        }

    @Test
    fun `getSubscription with null storage and discounts should parse`() =
        runTest {
            val response = MockResponse()
                .setBody(SUBSCRIPTION_RESPONSE_MINIMAL_JSON)
                .setResponseCode(200)
            server.enqueue(response)
            val actual = service.getSubscription()
            assertEquals(
                GetSubscriptionResponse.Success(
                    subscription = SUBSCRIPTION_RESPONSE_MINIMAL,
                ).asSuccess(),
                actual,
            )
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
                GetSubscriptionResponse.Success(
                    subscription = SUBSCRIPTION_RESPONSE_AMOUNT_OFF,
                ).asSuccess(),
                actual,
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
                GetSubscriptionResponse.Success(
                    subscription = SUBSCRIPTION_RESPONSE_PERCENT_OFF,
                ).asSuccess(),
                actual,
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
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 19.80,
        "discounts": null
      },
      "additionalStorage": {
        "quantity": 24,
        "cost": 24.00,
        "discounts": null
      }
    },
    "cadence": "annually",
    "discounts": [
      {
        "type": "amount-off",
        "value": 2.10,
        "amount": 2.10,
        "label": "Welcome Coupon",
        "durationInMonths": 12
      }
    ],
    "startingBalance": 0,
    "estimatedTax": 3.85,
    "total": 45.55,
    "amountDue": 45.55,
    "nextPaymentAttempt": "2026-04-02T00:00:00Z"
  },
  "storage": {
    "available": 5,
    "used": 0,
    "readableUsed": "0 Bytes"
  },
  "cancelAt": null,
  "canceled": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE = BitwardenSubscriptionResponseJson(
    status = SubscriptionStatusJson.ACTIVE,
    invoicePreview = InvoicePreviewJson(
        passwordManager = PasswordManagerInvoicePreviewJson(
            seats = InvoicePreviewItemJson(
                quantity = 1,
                cost = BigDecimal("19.80"),
                discounts = null,
            ),
            additionalStorage = InvoicePreviewItemJson(
                quantity = 24,
                cost = BigDecimal("24.00"),
                discounts = null,
            ),
        ),
        cadence = CadenceTypeJson.ANNUALLY,
        discounts = listOf(
            InvoicePreviewDiscountJson(
                type = DiscountTypeJson.AMOUNT_OFF,
                value = BigDecimal("2.10"),
                amount = BigDecimal("2.10"),
                label = "Welcome Coupon",
                durationInMonths = 12,
            ),
        ),
        startingBalance = BigDecimal("0"),
        estimatedTax = BigDecimal("3.85"),
        total = BigDecimal("45.55"),
        amountDue = BigDecimal("45.55"),
        nextPaymentAttempt = Instant.parse("2026-04-02T00:00:00Z"),
    ),
    storage = StorageJson(
        available = 5,
        used = 0.0,
        readableUsed = "0 Bytes",
    ),
    cancelAt = null,
    canceled = null,
    suspension = null,
    gracePeriod = null,
)

private const val SUBSCRIPTION_RESPONSE_MONTHLY_JSON = """
{
  "status": "active",
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 1.67,
        "discounts": null
      },
      "additionalStorage": null
    },
    "cadence": "monthly",
    "discounts": null,
    "startingBalance": null,
    "estimatedTax": 0,
    "total": 1.67,
    "amountDue": 1.67,
    "nextPaymentAttempt": null
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE_MONTHLY = BitwardenSubscriptionResponseJson(
    status = SubscriptionStatusJson.ACTIVE,
    invoicePreview = InvoicePreviewJson(
        passwordManager = PasswordManagerInvoicePreviewJson(
            seats = InvoicePreviewItemJson(
                quantity = 1,
                cost = BigDecimal("1.67"),
                discounts = null,
            ),
            additionalStorage = null,
        ),
        cadence = CadenceTypeJson.MONTHLY,
        discounts = null,
        startingBalance = null,
        estimatedTax = BigDecimal("0"),
        total = BigDecimal("1.67"),
        amountDue = BigDecimal("1.67"),
        nextPaymentAttempt = null,
    ),
    storage = null,
    cancelAt = null,
    canceled = null,
    suspension = null,
    gracePeriod = null,
)

private const val SUBSCRIPTION_RESPONSE_MINIMAL_JSON = """
{
  "status": "active",
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 19.80,
        "discounts": null
      },
      "additionalStorage": null
    },
    "cadence": "annually",
    "discounts": null,
    "startingBalance": null,
    "estimatedTax": 0,
    "total": 19.80,
    "amountDue": 19.80,
    "nextPaymentAttempt": null
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE_MINIMAL = BitwardenSubscriptionResponseJson(
    status = SubscriptionStatusJson.ACTIVE,
    invoicePreview = InvoicePreviewJson(
        passwordManager = PasswordManagerInvoicePreviewJson(
            seats = InvoicePreviewItemJson(
                quantity = 1,
                cost = BigDecimal("19.80"),
                discounts = null,
            ),
            additionalStorage = null,
        ),
        cadence = CadenceTypeJson.ANNUALLY,
        discounts = null,
        startingBalance = null,
        estimatedTax = BigDecimal("0"),
        total = BigDecimal("19.80"),
        amountDue = BigDecimal("19.80"),
        nextPaymentAttempt = null,
    ),
    storage = null,
    cancelAt = null,
    canceled = null,
    suspension = null,
    gracePeriod = null,
)

private const val SUBSCRIPTION_RESPONSE_AMOUNT_OFF_JSON = """
{
  "status": "active",
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 19.80,
        "discounts": null
      },
      "additionalStorage": null
    },
    "cadence": "annually",
    "discounts": [
      {
        "type": "amount-off",
        "value": 5.00,
        "amount": 5.00,
        "label": null,
        "durationInMonths": null
      }
    ],
    "startingBalance": null,
    "estimatedTax": 0,
    "total": 19.80,
    "amountDue": 19.80,
    "nextPaymentAttempt": null
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE_AMOUNT_OFF = SUBSCRIPTION_RESPONSE_MINIMAL.copy(
    invoicePreview = SUBSCRIPTION_RESPONSE_MINIMAL.invoicePreview.copy(
        discounts = listOf(
            InvoicePreviewDiscountJson(
                type = DiscountTypeJson.AMOUNT_OFF,
                value = BigDecimal("5.00"),
                amount = BigDecimal("5.00"),
                label = null,
                durationInMonths = null,
            ),
        ),
    ),
)

private const val SUBSCRIPTION_RESPONSE_PERCENT_OFF_JSON = """
{
  "status": "active",
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 19.80,
        "discounts": [
          {
            "type": "percent-off",
            "value": 15,
            "amount": 2.97,
            "label": null,
            "durationInMonths": null
          }
        ]
      },
      "additionalStorage": null
    },
    "cadence": "annually",
    "discounts": null,
    "startingBalance": null,
    "estimatedTax": 0,
    "total": 19.80,
    "amountDue": 19.80,
    "nextPaymentAttempt": null
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
  "suspension": null,
  "gracePeriod": null
}
"""

private val SUBSCRIPTION_RESPONSE_PERCENT_OFF = SUBSCRIPTION_RESPONSE_MINIMAL.copy(
    invoicePreview = SUBSCRIPTION_RESPONSE_MINIMAL.invoicePreview.copy(
        passwordManager = SUBSCRIPTION_RESPONSE_MINIMAL.invoicePreview.passwordManager.copy(
            seats = InvoicePreviewItemJson(
                quantity = 1,
                cost = BigDecimal("19.80"),
                discounts = listOf(
                    InvoicePreviewDiscountJson(
                        type = DiscountTypeJson.PERCENT_OFF,
                        value = BigDecimal("15"),
                        amount = BigDecimal("2.97"),
                        label = null,
                        durationInMonths = null,
                    ),
                ),
            ),
        ),
    ),
)

private const val SUBSCRIPTION_RESPONSE_UNKNOWN_CADENCE_JSON = """
{
  "status": "active",
  "invoicePreview": {
    "passwordManager": {
      "seats": {
        "quantity": 1,
        "cost": 19.80,
        "discounts": null
      },
      "additionalStorage": null
    },
    "cadence": "weekly",
    "discounts": null,
    "startingBalance": null,
    "estimatedTax": 0,
    "total": 19.80,
    "amountDue": 19.80,
    "nextPaymentAttempt": null
  },
  "storage": null,
  "cancelAt": null,
  "canceled": null,
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
    }
    return """
    {
      "status": "$wireValue",
      "invoicePreview": {
        "passwordManager": {
          "seats": {
            "quantity": 1,
            "cost": 19.80,
            "discounts": null
          },
          "additionalStorage": null
        },
        "cadence": "annually",
        "discounts": null,
        "startingBalance": null,
        "estimatedTax": 0,
        "total": 19.80,
        "amountDue": 19.80,
        "nextPaymentAttempt": null
      },
      "storage": null,
      "cancelAt": null,
      "canceled": null,
      "suspension": null,
      "gracePeriod": null
    }
    """.trimIndent()
}
