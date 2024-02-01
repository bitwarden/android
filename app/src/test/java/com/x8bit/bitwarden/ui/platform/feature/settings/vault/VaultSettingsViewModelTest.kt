package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultSettingsViewModelTest : BaseViewModelTest() {
    private val environmentRepository = FakeEnvironmentRepository()

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.BackClick)
            assertEquals(VaultSettingsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ExportVaultClick should emit NavigateToExportVault`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ExportVaultClick)
            assertEquals(
                VaultSettingsEvent.NavigateToExportVault,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ImportItemsClick should emit send NavigateToImportVault with correct url`() = runTest {
        val viewModel = createViewModel()
        val expected = "https://vault.bitwarden.com/#/tools/import"
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
            assertEquals(
                VaultSettingsEvent.NavigateToImportVault(expected),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): VaultSettingsViewModel = VaultSettingsViewModel(
        environmentRepository = environmentRepository,
    )
}
