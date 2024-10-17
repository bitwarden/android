package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.base.util.asText

/**
 * Convert [SharedVerificationCodesState.Success] into [SharedCodesDisplayState.Codes].
 */
fun SharedVerificationCodesState.Success.toSharedCodesDisplayState(
    alertThresholdSeconds: Int,
): SharedCodesDisplayState.Codes {
    val codesMap =
        mutableMapOf<AuthenticatorItem.Source.Shared, MutableList<VerificationCodeDisplayItem>>()
    // Make a map where each key is a Bitwarden account and each value is a list of verification
    // codes for that account:
    this.items.forEach {
        codesMap.putIfAbsent(it.source as AuthenticatorItem.Source.Shared, mutableListOf())
        codesMap[it.source]?.add(it.toDisplayItem(alertThresholdSeconds))
    }
    // Flatten that map down to a list of accounts that each has a list of codes:
    return codesMap
        .map {
            SharedCodesDisplayState.SharedCodesAccountSection(
                label = R.string.shared_accounts_header.asText(
                    it.key.email,
                    it.key.environmentLabel,
                ),
                codes = it.value,
            )
        }
        .let { SharedCodesDisplayState.Codes(it) }
}
