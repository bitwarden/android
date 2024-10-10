package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem

/**
 * Converts [VerificationCodeItem] to a [VerificationCodeDisplayItem].
 */
fun VerificationCodeItem.toDisplayItem(alertThresholdSeconds: Int) =
    VerificationCodeDisplayItem(
        id = id,
        issuer = issuer,
        label = accountName,
        timeLeftSeconds = timeLeftSeconds,
        periodSeconds = periodSeconds,
        alertThresholdSeconds = alertThresholdSeconds,
        authCode = code,
        favorite = (source as? AuthenticatorItem.Source.Local)?.isFavorite ?: false,
    )
