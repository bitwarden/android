package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AutofillSaveItemExtensionsTest {
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
                type = VaultAddEditState.ViewState.Content.ItemType.Card(
                    number = "number",
                    expirationMonth = VaultCardExpirationMonth.JANUARY,
                    expirationYear = "2024",
                    securityCode = "securityCode",
                ),
            ),
            AutofillSaveItem.Card(
                number = "number",
                expirationMonth = "1",
                expirationYear = "2024",
                securityCode = "securityCode",
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
                    username = "username",
                    password = "password",
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
            AutofillSaveItem.Login(
                username = "username",
                password = "password",
                uri = "https://www.test.com",
            )
                .toDefaultAddTypeContent(isIndividualVaultDisabled = true),
        )
    }
}
