package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
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
    private val environmentRepository = FakeEnvironmentRepository()
    private val mutableImportLoginsFlagFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlagFlow(FlagKey.ImportLoginsFlow) } returns mutableImportLoginsFlagFlow
        every { getFeatureFlag(FlagKey.ImportLoginsFlow) } returns false
    }
    private val mockUserState = mockk<UserState> {
        every { activeUserFirstTimeState } returns DEFAULT_FIRST_TIME_STATE
    }
    private val mockUserStateFlow = MutableStateFlow(mockUserState)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mockUserStateFlow
        every { setShowImportLogins(any()) } just runs
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

    @Test
    fun `shouldShowImportCard should update when user state changes`() = runTest {
        mutableImportLoginsFlagFlow.update { true }
        val viewModel = createViewModel()
        assertTrue(viewModel.stateFlow.value.shouldShowImportCard)
        val newUserState = mockk<UserState>(relaxed = true) {
            every { activeUserFirstTimeState } returns DEFAULT_FIRST_TIME_STATE.copy(
                showImportLoginsCard = false,
            )
        }
        mockUserStateFlow.update { newUserState }
        assertFalse(viewModel.stateFlow.value.shouldShowImportCard)
    }

    @Test
    fun `shouldShowImportCard should be false when feature flag not enabled`() = runTest {
        val viewModel = createViewModel()
        mutableImportLoginsFlagFlow.update { false }
        assertFalse(viewModel.stateFlow.value.shouldShowImportCard)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ImportLoginsCardCtaClick action should set repository value to false and send navigation event`() =
        runTest {
            val viewModel = createViewModel()
            val expected = "https://vault.bitwarden.com/#/tools/import"
            mutableImportLoginsFlagFlow.update { true }
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardCtaClick)
                assertEquals(
                    VaultSettingsEvent.NavigateToImportVault(url = expected),
                    awaitItem(),
                )
            }
            verify(exactly = 1) { authRepository.setShowImportLogins(false) }
        }

    @Test
    fun `ImportLoginsCardDismissClick action should set repository value to false `() = runTest {
        val viewModel = createViewModel()
        mutableImportLoginsFlagFlow.update { true }
        viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardDismissClick)
        verify(exactly = 1) { authRepository.setShowImportLogins(false) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ImportLoginsCardDismissClick action should not set repository value to false if already false`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardDismissClick)
            verify(exactly = 0) { authRepository.setShowImportLogins(false) }
        }

    private fun createViewModel(): VaultSettingsViewModel = VaultSettingsViewModel(
        environmentRepository = environmentRepository,
        featureFlagManager = featureFlagManager,
        authRepository = authRepository,
    )
}

private val DEFAULT_FIRST_TIME_STATE = UserState.FirstTimeState(showImportLoginsCard = true)
