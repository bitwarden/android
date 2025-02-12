package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem

/**
 * Converts [VerificationCodeItem] to a [VerificationCodeDisplayItem].
 */
fun VerificationCodeItem.toDisplayItem(
    alertThresholdSeconds: Int,
    sharedVerificationCodesState: SharedVerificationCodesState,
) = VerificationCodeDisplayItem(
    id = id,
    title = issuer ?: label ?: "--",
    subtitle = if (issuer != null) {
        // Only show label if it is not being used as the primary title:
        label
    } else {
        null
    },
    timeLeftSeconds = timeLeftSeconds,
    periodSeconds = periodSeconds,
    alertThresholdSeconds = alertThresholdSeconds,
    authCode = code,
    allowLongPressActions = when (source) {
        is AuthenticatorItem.Source.Local -> true
        is AuthenticatorItem.Source.Shared -> false
    },
    favorite = (source as? AuthenticatorItem.Source.Local)?.isFavorite ?: false,
    showMoveToBitwarden = when (source) {
        // Shared items should never show Move to Bitwarden action:
        is AuthenticatorItem.Source.Shared -> false

        // Local items should only show Move to Bitwarden if we are successfully syncing: =
        is AuthenticatorItem.Source.Local -> when (sharedVerificationCodesState) {
            SharedVerificationCodesState.AppNotInstalled,
            SharedVerificationCodesState.Error,
            SharedVerificationCodesState.FeatureNotEnabled,
            SharedVerificationCodesState.Loading,
            SharedVerificationCodesState.OsVersionNotSupported,
            SharedVerificationCodesState.SyncNotEnabled,
                -> false

            is SharedVerificationCodesState.Success -> true
        }
    },
)
