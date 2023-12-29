package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultSettingsViewModelTest : BaseViewModelTest() {

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.BackClick)
            assertEquals(VaultSettingsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ExportVaultClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ExportVaultClick)
            assertEquals(
                VaultSettingsEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ImportItemsClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
            assertEquals(
                VaultSettingsEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): VaultSettingsViewModel = VaultSettingsViewModel()
}
