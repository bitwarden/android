package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingState
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.VerificationCodeDisplayItem

fun List<VerificationCodeItem>.toViewState(
    alertThresholdSeconds: Int,
): ItemListingState.ViewState =
    if (isEmpty()) {
        ItemListingState.ViewState.NoItems
    } else {
        ItemListingState.ViewState.Content(
            map { it.toDisplayItem(alertThresholdSeconds = alertThresholdSeconds) },
        )
    }

fun VerificationCodeItem.toDisplayItem(alertThresholdSeconds: Int) =
    VerificationCodeDisplayItem(
        id = id,
        label = label,
        supportingLabel = username,
        timeLeftSeconds = timeLeftSeconds,
        periodSeconds = periodSeconds,
        alertThresholdSeconds = alertThresholdSeconds,
        authCode = code,
    )
