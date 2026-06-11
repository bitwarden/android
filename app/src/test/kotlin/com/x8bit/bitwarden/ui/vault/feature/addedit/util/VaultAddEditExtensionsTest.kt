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

    @Test
    fun `withAuthenticatorKeyPremiumGate gates Login when user lacks Premium`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(),
        )

        val result = viewState.withAuthenticatorKeyPremiumGate(isPremium = false)

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    isAuthenticatorKeyPremiumGated = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `withAuthenticatorKeyPremiumGate does not gate Login when user has Premium`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(),
        )

        val result = viewState.withAuthenticatorKeyPremiumGate(isPremium = true)

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    isAuthenticatorKeyPremiumGated = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `withAuthenticatorKeyPremiumGate returns input unchanged for non-Login content`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Card(),
        )

        val result = viewState.withAuthenticatorKeyPremiumGate(isPremium = false)

        assertEquals(viewState, result)
    }

    @Test
    fun `withAuthenticatorKeyPremiumGate returns input unchanged for non-Content view state`() {
        val viewState: VaultAddEditState.ViewState = VaultAddEditState.ViewState.Loading

        val result = viewState.withAuthenticatorKeyPremiumGate(isPremium = false)

        assertEquals(viewState, result)
    }
}
