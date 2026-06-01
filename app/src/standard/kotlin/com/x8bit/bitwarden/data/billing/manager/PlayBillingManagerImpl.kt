package com.x8bit.bitwarden.data.billing.manager

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.PendingPurchasesParams
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val SUPPORTED_BILLING_COUNTRY = "US"

/**
 * Standard implementation of [PlayBillingManager] using the Google Play Billing Library.
 *
 * Uses a connect-per-call lifecycle: a new [BillingClient] is created, connected, queried,
 * and disconnected for each call.
 */
@OmitFromCoverage
class PlayBillingManagerImpl(
    private val context: Context,
    dispatcherManager: DispatcherManager,
) : PlayBillingManager {

    private val unconfinedScope =
        CoroutineScope(dispatcherManager.unconfined)

    private val mutableIsInAppBillingSupportedFlow = MutableStateFlow(false)

    override val isInAppBillingSupportedFlow: StateFlow<Boolean> =
        mutableIsInAppBillingSupportedFlow.asStateFlow()

    init {
        unconfinedScope.launch {
            mutableIsInAppBillingSupportedFlow.value =
                queryBillingCountry().getOrNull() == SUPPORTED_BILLING_COUNTRY
        }
    }

    private suspend fun queryBillingCountry(): Result<String> {
        val billingClient = BillingClient
            .newBuilder(context)
            .setListener { _, _ ->
                // No-op: we don't handle purchases.
            }
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()

        return billingClient.useConnection {
            if (responseCode != BillingClient.BillingResponseCode.OK) {
                return@useConnection BillingException("Connection failed: $debugMessage")
                    .asFailure()
            }
            val (configResult, billingConfig) =
                billingClient.getBillingConfig()
            if (configResult.responseCode != BillingClient.BillingResponseCode.OK ||
                billingConfig == null
            ) {
                BillingException("Config query failed: ${configResult.debugMessage}").asFailure()
            } else {
                billingConfig.countryCode.asSuccess()
            }
        }
    }
}

/**
 * Connects to the [BillingClient], executes [block] with the [BillingResult], and guarantees
 * [BillingClient.endConnection] is called when finished. Catches [IllegalStateException] thrown
 * by [BillingClient.startConnection] if the client is already connected or mid-connection.
 */
private suspend fun <T> BillingClient.useConnection(
    block: suspend BillingResult.() -> Result<T>,
): Result<T> = try {
    block(
        suspendCancellableCoroutine { continuation ->
            startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(
                        billingResult: BillingResult,
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(billingResult)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // No-op: connect-per-call lifecycle, no reconnection.
                    }
                },
            )
        },
    )
} catch (e: IllegalStateException) {
    e.asFailure()
} finally {
    endConnection()
}

/**
 * Wraps [BillingClient.getBillingConfigAsync] in a suspend function using
 * [suspendCancellableCoroutine].
 */
private suspend fun BillingClient.getBillingConfig(): Pair<BillingResult, BillingConfig?> =
    suspendCancellableCoroutine { continuation ->
        val params = GetBillingConfigParams.newBuilder().build()
        getBillingConfigAsync(params) { billingResult, billingConfig ->
            if (continuation.isActive) {
                continuation.resume(billingResult to billingConfig)
            }
        }
    }

/**
 * Exception type for billing-specific errors.
 */
private class BillingException(message: String) : Exception(message)
