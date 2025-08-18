package com.x8bit.bitwarden.ui.platform.feature.rootnav.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

/**
 * Derives a [VaultItemListingType] from the given [AutofillSelectionData.Type].
 */
fun AutofillSelectionData.Type.toVaultItemListingType(): VaultItemListingType =
    when (this) {
        AutofillSelectionData.Type.CARD -> VaultItemListingType.Card
        AutofillSelectionData.Type.LOGIN -> VaultItemListingType.Login
    }
