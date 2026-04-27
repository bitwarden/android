package com.bitwarden.authenticator.ui.authenticator.feature.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem

private const val NEXT_CODE_SHOW_THRESHOLD_SECONDS = 10

/**
 * Converts [VerificationCodeItem] to a [VerificationCodeDisplayItem].
 *
 * @param showNextCode When `true`, the upcoming code is mapped to
 *  [VerificationCodeDisplayItem.nextAuthCode] for items within the last
 *  [NEXT_CODE_SHOW_THRESHOLD_SECONDS] seconds of their validity period. When `false`,
 *  [VerificationCodeDisplayItem.nextAuthCode] is always `null`.
 */
fun VerificationCodeItem.toDisplayItem(
    alertThresholdSeconds: Int,
    sharedVerificationCodesState: SharedVerificationCodesState,
    showOverflow: Boolean,
    showNextCode: Boolean = false,
): VerificationCodeDisplayItem = VerificationCodeDisplayItem(
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
    nextAuthCode = if (showNextCode && timeLeftSeconds <= NEXT_CODE_SHOW_THRESHOLD_SECONDS) {
        nextCode
    } else {
        null
    },
    showOverflow = showOverflow,
    favorite = (source as? AuthenticatorItem.Source.Local)?.isFavorite ?: false,
    showMoveToBitwarden = when (source) {
        // Shared items should never show "Copy to Bitwarden vault" action:
        is AuthenticatorItem.Source.Shared -> false

        // Local items should only show "Copy to Bitwarden vault" if we are successfully syncing: =
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
