package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockedNavBarViewModelTest : BaseViewModelTest() {
    @Test
    fun `VaultTabClick should navigate to the vault screen`() = runTest {
        val viewModel = VaultUnlockedNavBarViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToVaultScreen, awaitItem())
        }
    }

    @Test
    fun `SendTabClick should navigate to the send screen`() = runTest {
        val viewModel = VaultUnlockedNavBarViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToSendScreen, awaitItem())
        }
    }

    @Test
    fun `GeneratorTabClick should navigate to the generator screen`() = runTest {
        val viewModel = VaultUnlockedNavBarViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultUnlockedNavBarAction.GeneratorTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen, awaitItem())
        }
    }

    @Test
    fun `SettingsTabClick should navigate to the settings screen`() = runTest {
        val viewModel = VaultUnlockedNavBarViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultUnlockedNavBarAction.SettingsTabClick)
            assertEquals(VaultUnlockedNavBarEvent.NavigateToSettingsScreen, awaitItem())
        }
    }
}
