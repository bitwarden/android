package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class VaultPassportItemTypeHandlersTest {

    private val viewModel = mockk<VaultItemViewModel> {
        every { trySendAction(any()) } returns Unit
    }
    private val handlers = VaultPassportItemTypeHandlers.create(viewModel = viewModel)

    @Test
    fun `onCopyGivenNameClick should send CopyGivenNameClick action`() {
        handlers.onCopyGivenNameClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.CopyGivenNameClick,
            )
        }
    }

    @Test
    fun `onCopySurnameClick should send CopySurnameClick action`() {
        handlers.onCopySurnameClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.CopySurnameClick,
            )
        }
    }

    @Test
    fun `onCopyPassportNumberClick should send CopyPassportNumberClick action`() {
        handlers.onCopyPassportNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.CopyPassportNumberClick,
            )
        }
    }

    @Test
    fun `onCopyNationalIdentificationNumberClick should send the matching action`() {
        handlers.onCopyNationalIdentificationNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.CopyNationalIdentificationNumberClick,
            )
        }
    }
}
