package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultSettingsViewModelTest : BaseViewModelTest() {
    private val buildInfoManager = mockk<BuildInfoManager> {
        every { isFdroid } returns false
    }
    private val mutableFirstTimeStateFlow = MutableStateFlow(DEFAULT_FIRST_TIME_STATE)
    private val firstTimeActionManager = mockk<FirstTimeActionManager> {
        every { currentOrDefaultUserFirstTimeState } returns DEFAULT_FIRST_TIME_STATE
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowImportLoginsSettingsBadge(any()) } just runs
    }
    private val mutablePoliciesFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val policyManager = mockk<PolicyManager> {
        every { getActivePolicies(any()) } returns emptyList()
        every { getActivePoliciesFlow(any()) } returns mutablePoliciesFlow
    }

    private val mutableSnackbarSharedFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager = mockk<SnackbarRelayManager<SnackbarRelay>> {
        every {
            getSnackbarDataFlow(SnackbarRelay.LOGINS_IMPORTED)
        } returns mutableSnackbarSharedFlow
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
    fun `ImportItemsClick should emit NavigateToImportVault when policy is not empty`() = runTest {
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockk())

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
            assertEquals(
                VaultSettingsEvent.NavigateToImportVault,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ImportItemsClick should emit NavigateToImportItems when policy is empty`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
            assertEquals(
                VaultSettingsEvent.NavigateToImportItems,
                awaitItem(),
            )
        }
    }

    @Test
    fun `shouldShowImportCard should update when first time state changes`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.stateFlow.value.showImportActionCard)
        mutableFirstTimeStateFlow.update {
            it.copy(showImportLoginsCardInSettings = false)
        }
        assertFalse(viewModel.stateFlow.value.showImportActionCard)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ImportLoginsCardCtaClick action should set repository value to false and send navigation event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardCtaClick)
                assertEquals(
                    VaultSettingsEvent.NavigateToImportVault,
                    awaitItem(),
                )
            }
            verify(exactly = 0) {
                firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
            }
        }

    @Test
    fun `ImportLoginsCardDismissClick action should set repository value to false `() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardDismissClick)
        verify(exactly = 1) {
            firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ImportLoginsCardDismissClick action should not set repository value to false if already false`() =
        runTest {
            mutableFirstTimeStateFlow.update { it.copy(showImportLoginsCardInSettings = false) }
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardDismissClick)
            verify(exactly = 0) {
                firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
            }
        }

    @Test
    fun `SnackbarDataReceived action should send snackbar event`() = runTest {
        val viewModel = createViewModel()
        val expectedSnackbarData = BitwardenSnackbarData(message = "test message".asText())
        viewModel.eventFlow.test {
            mutableSnackbarSharedFlow.tryEmit(expectedSnackbarData)
            assertEquals(VaultSettingsEvent.ShowSnackbar(expectedSnackbarData), awaitItem())
        }
    }

    @Test
    fun `showImportItemsChevron should display based on policies`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            // Verify chevron is shown when there are no policies (default state)
            assertEquals(
                VaultSettingsState(showImportActionCard = true, showImportItemsChevron = true),
                awaitItem(),
            )

            // Verify chevron is hidden when policies exist
            mutablePoliciesFlow.tryEmit(listOf(mockk()))
            assertEquals(
                VaultSettingsState(showImportActionCard = true, showImportItemsChevron = false),
                awaitItem(),
            )

            // Verify chevron is hidden when there are no policies
            mutablePoliciesFlow.tryEmit(emptyList())
            assertEquals(
                VaultSettingsState(showImportActionCard = true, showImportItemsChevron = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun `showImportItemsChevron should be false when policy exists`() {
        val viewModel = createViewModel()
        mutablePoliciesFlow.tryEmit(listOf(mockk()))
        assertEquals(
            VaultSettingsState(showImportActionCard = true, showImportItemsChevron = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `showImportItemsChevron should be false when isFdroid is true`() {
        every { buildInfoManager.isFdroid } returns true
        val viewModel = createViewModel()
        assertEquals(
            VaultSettingsState(
                showImportActionCard = true,
                showImportItemsChevron = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ImportItemsClick should emit NavigateToImportVault when isFdroid is true`() =
        runTest {
            every { buildInfoManager.isFdroid } returns true
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
                assertEquals(
                    VaultSettingsEvent.NavigateToImportVault,
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(): VaultSettingsViewModel = VaultSettingsViewModel(
        buildInfoManager = buildInfoManager,
        firstTimeActionManager = firstTimeActionManager,
        snackbarRelayManager = snackbarRelayManager,
        policyManager = policyManager,
    )
}

private val DEFAULT_FIRST_TIME_STATE = FirstTimeState(showImportLoginsCardInSettings = true)
