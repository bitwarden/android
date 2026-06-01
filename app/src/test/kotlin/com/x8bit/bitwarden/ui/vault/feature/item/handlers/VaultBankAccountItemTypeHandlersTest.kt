package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class VaultBankAccountItemTypeHandlersTest {

    private val viewModel = mockk<VaultItemViewModel> {
        every { trySendAction(any()) } returns Unit
    }
    private val handlers = VaultBankAccountItemTypeHandlers.create(viewModel = viewModel)

    @Test
    fun `onCopyNameOnAccountClick should send CopyNameOnAccountClick action`() {
        handlers.onCopyNameOnAccountClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyNameOnAccountClick,
            )
        }
    }

    @Test
    fun `onCopyAccountNumberClick should send CopyAccountNumberClick action`() {
        handlers.onCopyAccountNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyAccountNumberClick,
            )
        }
    }

    @Test
    fun `onCopyRoutingNumberClick should send CopyRoutingNumberClick action`() {
        handlers.onCopyRoutingNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyRoutingNumberClick,
            )
        }
    }

    @Test
    fun `onCopyBranchNumberClick should send CopyBranchNumberClick action`() {
        handlers.onCopyBranchNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyBranchNumberClick,
            )
        }
    }

    @Test
    fun `onCopyPinClick should send CopyPinClick action`() {
        handlers.onCopyPinClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyPinClick,
            )
        }
    }

    @Test
    fun `onCopySwiftCodeClick should send CopySwiftCodeClick action`() {
        handlers.onCopySwiftCodeClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopySwiftCodeClick,
            )
        }
    }

    @Test
    fun `onCopyIbanClick should send CopyIbanClick action`() {
        handlers.onCopyIbanClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyIbanClick,
            )
        }
    }

    @Test
    fun `onCopyBankContactPhoneClick should send CopyBankContactPhoneClick action`() {
        handlers.onCopyBankContactPhoneClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.BankAccount.CopyBankContactPhoneClick,
            )
        }
    }
}
