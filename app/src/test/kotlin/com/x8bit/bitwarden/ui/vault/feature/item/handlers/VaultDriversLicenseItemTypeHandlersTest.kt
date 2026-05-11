package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class VaultDriversLicenseItemTypeHandlersTest {

    private val viewModel = mockk<VaultItemViewModel> {
        every { trySendAction(any()) } returns Unit
    }
    private val handlers = VaultDriversLicenseItemTypeHandlers.create(viewModel = viewModel)

    @Test
    fun `onCopyFirstNameClick should send CopyFirstNameClick action`() {
        handlers.onCopyFirstNameClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.DriversLicense.CopyFirstNameClick,
            )
        }
    }

    @Test
    fun `onCopyMiddleNameClick should send CopyMiddleNameClick action`() {
        handlers.onCopyMiddleNameClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.DriversLicense.CopyMiddleNameClick,
            )
        }
    }

    @Test
    fun `onCopyLastNameClick should send CopyLastNameClick action`() {
        handlers.onCopyLastNameClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.DriversLicense.CopyLastNameClick,
            )
        }
    }

    @Test
    fun `onCopyLicenseNumberClick should send CopyLicenseNumberClick action`() {
        handlers.onCopyLicenseNumberClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.DriversLicense.CopyLicenseNumberClick,
            )
        }
    }
}
