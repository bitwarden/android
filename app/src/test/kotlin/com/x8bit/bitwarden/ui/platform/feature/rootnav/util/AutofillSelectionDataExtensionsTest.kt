package com.x8bit.bitwarden.ui.platform.feature.rootnav.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillSelectionDataExtensionsTest {
    @Test
    fun `toVaultItemListingType should return the correct result for each type`() {
        mapOf(
            AutofillSelectionData.Type.CARD to VaultItemListingType.Card,
            AutofillSelectionData.Type.LOGIN to VaultItemListingType.Login,
        )
            .forEach { (type, expected) ->
                assertEquals(
                    expected,
                    type.toVaultItemListingType(),
                )
            }
    }
}
