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

    @Suppress("MaxLineLength")
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
