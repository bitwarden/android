package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AutofillSelectionDataExtensionsTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(UUID::randomUUID)
    }

    @BeforeEach
    fun tearDown() {
        unmockkStatic(UUID::randomUUID)
    }

    @Test
    fun `toDefaultAddTypeContent for a Card type should return the correct Content`() {
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Card(),
            ),
            AutofillSelectionData(
                type = AutofillSelectionData.Type.CARD,
                framework = AutofillSelectionData.Framework.AUTOFILL,
                uri = null,
            )
                .toDefaultAddTypeContent(isIndividualVaultDisabled = false),
        )
    }

    @Test
    fun `toDefaultAddTypeContent for a Login type should return the correct Content`() {
        every { UUID.randomUUID().toString() } returns "uuid"
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    name = "www.test.com",
                ),
                isIndividualVaultDisabled = true,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    uriList = listOf(
                        UriItem(
                            id = "uuid",
                            uri = "https://www.test.com",
                            match = null,
                            checksum = null,
                        ),
                    ),
                ),
            ),
            AutofillSelectionData(
                type = AutofillSelectionData.Type.LOGIN,
                framework = AutofillSelectionData.Framework.AUTOFILL,
                uri = "https://www.test.com",
            )
                .toDefaultAddTypeContent(isIndividualVaultDisabled = true),
        )
    }
}
