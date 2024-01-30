package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillSelectionDataExtensionsTest {
    @Test
    fun `toDefaultAddTypeContent for a Card type should return the correct Content`() {
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(),
            ),
            AutofillSelectionData(
                type = AutofillSelectionData.Type.CARD,
                uri = null,
            )
                .toDefaultAddTypeContent(),
        )
    }

    @Test
    fun `toDefaultAddTypeContent for a Login type should return the correct Content`() {
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    name = "www.test.com",
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    uri = "https://www.test.com",
                ),
            ),
            AutofillSelectionData(
                type = AutofillSelectionData.Type.LOGIN,
                uri = "https://www.test.com",
            )
                .toDefaultAddTypeContent(),
        )
    }
}
