package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData

/**
 * Derives an app bar title given the [VaultFilterData].
 */
fun VaultFilterData?.toAppBarTitle(): Text =
    if (this != null) {
        R.string.vaults
    } else {
        R.string.my_vault
    }
        .asText()
