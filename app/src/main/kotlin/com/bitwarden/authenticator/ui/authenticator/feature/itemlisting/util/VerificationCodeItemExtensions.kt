package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem

fun List<VerificationCodeItem>.toViewState(
    alertThresholdSeconds: Int,
): ItemListingState.ViewState =
    if (isEmpty()) {
        ItemListingState.ViewState.NoItems
    } else {
        ItemListingState.ViewState.Content(
            favoriteItems = this
                .filter { it.favorite }
                .sortedBy { it.issuer }
                .map {
                    it.toDisplayItem(alertThresholdSeconds = alertThresholdSeconds)
                },
            itemList = filterNot { it.favorite }
                .sortedBy { it.issuer }
                .map {
                    it.toDisplayItem(alertThresholdSeconds = alertThresholdSeconds)
                },
        )
    }

fun VerificationCodeItem.toDisplayItem(alertThresholdSeconds: Int) =
    VerificationCodeDisplayItem(
        id = id,
        issuer = issuer,
        username = username,
        timeLeftSeconds = timeLeftSeconds,
        periodSeconds = periodSeconds,
        alertThresholdSeconds = alertThresholdSeconds,
        authCode = code,
        favorite = favorite,
    )
