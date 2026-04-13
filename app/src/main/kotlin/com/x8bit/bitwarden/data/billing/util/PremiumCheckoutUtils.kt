package com.x8bit.bitwarden.data.billing.util

import android.net.Uri
import android.os.Parcelable
import androidx.browser.auth.AuthTabIntent
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.parcelize.Parcelize

/**
 * Query parameter name used by Stripe to indicate the checkout outcome.
 */
private const val RESULT_PARAM = "result"

/**
 * Query parameter value indicating a successful checkout.
 */
private const val RESULT_SUCCESS = "success"

/**
 * Retrieves a [PremiumCheckoutCallbackResult] from an
 * [AuthTabIntent.AuthResult].
 *
 * - [PremiumCheckoutCallbackResult.Success]: The user completed payment.
 * - [PremiumCheckoutCallbackResult.Canceled]: The user left without paying.
 */
@OmitFromCoverage
fun AuthTabIntent.AuthResult.getPremiumCheckoutCallbackResult(): PremiumCheckoutCallbackResult =
    when (resultCode) {
        AuthTabIntent.RESULT_OK -> resultUri.getPremiumCheckoutCallbackResult()
        else -> PremiumCheckoutCallbackResult.Canceled
    }

/**
 * Retrieves a [PremiumCheckoutCallbackResult] from a redirect [Uri].
 *
 * Examines the `result` query parameter: `?result=success` maps to
 * [PremiumCheckoutCallbackResult.Success], anything else maps to
 * [PremiumCheckoutCallbackResult.Canceled].
 */
fun Uri?.getPremiumCheckoutCallbackResult(): PremiumCheckoutCallbackResult {
    val resultParam = this?.getQueryParameter(RESULT_PARAM)
    return if (resultParam.equals(RESULT_SUCCESS, ignoreCase = true)) {
        PremiumCheckoutCallbackResult.Success
    } else {
        PremiumCheckoutCallbackResult.Canceled
    }
}

/**
 * Represents the result of a premium checkout callback from Stripe.
 */
sealed class PremiumCheckoutCallbackResult : Parcelable {

    /**
     * The user completed payment successfully.
     */
    @Parcelize
    data object Success : PremiumCheckoutCallbackResult()

    /**
     * The user canceled or left checkout without completing payment.
     */
    @Parcelize
    data object Canceled : PremiumCheckoutCallbackResult()
}
