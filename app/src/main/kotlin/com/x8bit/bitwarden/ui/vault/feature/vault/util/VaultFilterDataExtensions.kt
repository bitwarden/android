package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData

/**
 * Derives an app bar title given the [VaultFilterData].
 */
fun VaultFilterData?.toAppBarTitle(): Text =
    if (this != null) {
        BitwardenString.vaults
    } else {
        BitwardenString.my_vault
    }
        .asText()
