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
    fun `onCopyPassportNumberClick should send CopyPassportNumberClick action`() {
        handlers.onCopyPassportNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.CopyPassportNumberClick,
            )
        }
    }

    @Test
    fun `onPassportNumberVisibilityClick should send PassportNumberVisibilityClick action`() {
        handlers.onPassportNumberVisibilityClick(true)
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport.PassportNumberVisibilityClick(
                    isVisible = true,
                ),
            )
        }
    }

    @Test
    fun `onNationalIdentificationNumberVisibilityClick should send the matching action`() {
        handlers.onNationalIdentificationNumberVisibilityClick(false)
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Passport
                    .NationalIdentificationNumberVisibilityClick(isVisible = false),
            )
        }
    }
}
