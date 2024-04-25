package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockedNavBarViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk {
        every { updateLastActiveTime() } just runs
    }
    private val specialCircumstancesManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance = null } just runs
        every { specialCircumstance } returns null
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on init with GeneratorShortcut special circumstance should navigate to the generator screen`() =
        runTest {
            every {
                specialCircumstancesManager.specialCircumstance
            } returns SpecialCircumstance.GeneratorShortcut

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                assertEquals(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen, awaitItem())
            }
            verify(exactly = 1) {
                specialCircumstancesManager.specialCircumstance
                specialCircumstancesManager.specialCircumstance = null
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on init with VaultShortcut special circumstance should navigate to the generator screen`() =
        runTest {
            every {
                specialCircumstancesManager.specialCircumstance
            } returns SpecialCircumstance.VaultShortcut

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                assertEquals(VaultUnlockedNavBarEvent.NavigateToVaultScreen, awaitItem())
            }
            verify(exactly = 1) {
                specialCircumstancesManager.specialCircumstance
                specialCircumstancesManager.specialCircumstance = null
            }
        }

    @Test
    fun `on init with no shortcut special circumstance should do nothing`() = runTest {
        every { specialCircumstancesManager.specialCircumstance } returns null

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            expectNoEvents()
        }
        verify(exactly = 1) {
            specialCircumstancesManager.specialCircumstance
        }
        verify(exactly = 0) {
            specialCircumstancesManager.specialCircumstance = null
        }
    }

    @Test
    fun `VaultTabClick should navigate to the vault screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToVaultScreen, awaitItem())
        }
    }

    @Test
    fun `SendTabClick should navigate to the send screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToSendScreen, awaitItem())
        }
    }

    @Test
    fun `GeneratorTabClick should navigate to the generator screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen, awaitItem())
        }
    }

    @Test
    fun `SettingsTabClick should navigate to the settings screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToSettingsScreen, awaitItem())
        }
    }

    @Test
    fun `BackStackUpdate should call updateLastActiveTime`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockedNavBarAction.BackStackUpdate)
        verify { authRepository.updateLastActiveTime() }
    }

    private fun createViewModel() =
        VaultUnlockedNavBarViewModel(
            authRepository = authRepository,
            specialCircumstancesManager = specialCircumstancesManager,
        )
}
