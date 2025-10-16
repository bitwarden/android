package com.bitwarden.authenticator.ui.authenticator.feature.util

import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.toImmutableList

/**
 * Convert [SharedVerificationCodesState.Success] into [SharedCodesDisplayState.Codes].
 */
fun SharedVerificationCodesState.Success.toSharedCodesDisplayState(
    alertThresholdSeconds: Int,
    currentSections: List<SharedCodesDisplayState.SharedCodesAccountSection> = emptyList(),
): SharedCodesDisplayState.Codes {
    val codesMap =
        mutableMapOf<AuthenticatorItem.Source.Shared, MutableList<VerificationCodeDisplayItem>>()
    // Make a map where each key is a Bitwarden account and each value is a list of verification
    // codes for that account:
    this.items.forEach {
        codesMap.putIfAbsent(it.source as AuthenticatorItem.Source.Shared, mutableListOf())
        codesMap[it.source]?.add(
            it.toDisplayItem(
                alertThresholdSeconds = alertThresholdSeconds,
                // Always map based on Error state, because shared codes will never
                // show "Copy to Bitwarden vault" action.
                sharedVerificationCodesState = SharedVerificationCodesState.Error,
                showOverflow = false,
            ),
        )
    }
    // Flatten that map down to a list of accounts that each has a list of codes:
    return codesMap
        .map {
            SharedCodesDisplayState.SharedCodesAccountSection(
                id = it.key.userId,
                label = BitwardenString.shared_accounts_header.asText(
                    it.key.email,
                    it.key.environmentLabel,
                    it.value.size,
                ),
                codes = it.value.toImmutableList(),
                isExpanded = currentSections
                    .find { section -> section.id == it.key.userId }
                    ?.isExpanded
                    ?: true,
            )
        }
        .let { SharedCodesDisplayState.Codes(it.toImmutableList()) }
}
