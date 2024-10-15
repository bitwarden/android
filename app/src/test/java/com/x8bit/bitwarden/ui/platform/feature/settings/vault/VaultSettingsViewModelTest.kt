package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultSettingsViewModelTest : BaseViewModelTest() {
    private val environmentRepository = FakeEnvironmentRepository()
    private val mutableImportLoginsFlagFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlagFlow(FlagKey.ImportLoginsFlow) } returns mutableImportLoginsFlagFlow
        every { getFeatureFlag(FlagKey.ImportLoginsFlow) } returns false
    }

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

    @Test
    fun `ImportLoginsFeatureFlagChanged should update state`() {
        val viewModel = createViewModel()
        assertFalse(
            viewModel.stateFlow.value.isNewImportLoginsFlowEnabled,
        )
        mutableImportLoginsFlagFlow.update { true }
        assertTrue(viewModel.stateFlow.value.isNewImportLoginsFlowEnabled)
    }

    private fun createViewModel(): VaultSettingsViewModel = VaultSettingsViewModel(
        environmentRepository = environmentRepository,
        featureFlagManager = featureFlagManager,
    )
}
