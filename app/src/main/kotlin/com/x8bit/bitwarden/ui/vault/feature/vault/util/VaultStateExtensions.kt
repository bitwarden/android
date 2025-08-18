package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

/**
 * Converts the given [VaultState.ViewState] to a [VaultFilterData] (if applicable).
 */
fun VaultState.ViewState.vaultFilterDataIfRequired(
    vaultFilterData: VaultFilterData?,
): VaultFilterData? =
    when (this) {
        is VaultState.ViewState.Content,
        is VaultState.ViewState.NoItems,
            -> vaultFilterData?.let {
            if (it.vaultFilterTypes.contains(VaultFilterType.MyVault) ||
                it.vaultFilterTypes.size > 2
            ) {
                it
            } else {
                null
            }
        }

        is VaultState.ViewState.Error,
        is VaultState.ViewState.Loading,
            -> null
    }
