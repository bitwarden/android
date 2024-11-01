package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VaultAddEditExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID().toString() } returns "123"
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(UUID::randomUUID)
    }

    @Test
    fun `toItemType should return the correct response`() {
        val vaultItemCipherTypeList = listOf(
            VaultItemCipherType.LOGIN,
            VaultItemCipherType.CARD,
            VaultItemCipherType.SECURE_NOTE,
            VaultItemCipherType.IDENTITY,
            VaultItemCipherType.SSH_KEY,
        )

        val result = vaultItemCipherTypeList.map { it.toItemType() }

        assertEquals(
            listOf(
                VaultAddEditState.ViewState.Content.ItemType.Login(),
                VaultAddEditState.ViewState.Content.ItemType.Card(),
                VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                VaultAddEditState.ViewState.Content.ItemType.Identity(),
                VaultAddEditState.ViewState.Content.ItemType.SshKey(),
            ),
            result,
        )
    }
}
