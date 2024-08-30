package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockedNavBarViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
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
                assertEquals(
                    VaultUnlockedNavBarEvent.NavigateToGeneratorScreen(returnToSubgraphRoot = false),
                    awaitItem(),
                )
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
                assertEquals(
                    VaultUnlockedNavBarEvent.NavigateToVaultScreen(returnToSubgraphRoot = true),
                    awaitItem(),
                )
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
    fun `new user state should update vault nav bar title`() = runTest {
        val activeUserId = "activeUserId"
        val accountWithOrganizations: UserState.Account = mockk {
            every { userId } returns activeUserId
            every { organizations } returns listOf(mockk())
        }
        val expectedWithOrganizations = VaultUnlockedNavBarState(
            vaultNavBarLabelRes = R.string.vaults,
            vaultNavBarContentDescriptionRes = R.string.vaults,
            currentTab = BottomNavDestination.VAULT,
        )
        val accountWithoutOrganizations: UserState.Account = mockk {
            every { userId } returns activeUserId
            every { organizations } returns emptyList()
        }
        val expectedWithoutOrganizations = VaultUnlockedNavBarState(
            vaultNavBarLabelRes = R.string.my_vault,
            vaultNavBarContentDescriptionRes = R.string.my_vault,
            currentTab = BottomNavDestination.VAULT,
        )

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                expectedWithoutOrganizations,
                awaitItem(),
            )

            mutableUserStateFlow.tryEmit(
                UserState(
                    activeUserId = activeUserId,
                    accounts = listOf(accountWithOrganizations),
                ),
            )
            assertEquals(
                expectedWithOrganizations,
                awaitItem(),
            )

            mutableUserStateFlow.tryEmit(
                UserState(
                    activeUserId = activeUserId,
                    accounts = listOf(accountWithoutOrganizations),
                ),
            )
            assertEquals(
                expectedWithoutOrganizations,
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultTabClick should navigate to the vault screen with option to return to subgraph root true`() =
        runTest {
            val viewModel = createViewModel()
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick)
                assertEquals(
                    VaultUnlockedNavBarEvent.NavigateToVaultScreen(returnToSubgraphRoot = true),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultTabClick should navigate to the vault screen with option to return to subgraph root false`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick)
                assertEquals(
                    VaultUnlockedNavBarState(
                        vaultNavBarLabelRes = R.string.my_vault,
                        vaultNavBarContentDescriptionRes = R.string.my_vault,
                        currentTab = BottomNavDestination.SEND,
                    ),
                    viewModel.stateFlow.value,
                )
                assertEquals(
                    VaultUnlockedNavBarEvent.NavigateToSendScreen(returnToSubgraphRoot = false),
                    awaitItem(),
                )
                viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick)
                assertEquals(
                    VaultUnlockedNavBarEvent.NavigateToVaultScreen(returnToSubgraphRoot = false),
                    awaitItem(),
                )
            }
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `SendTabClick should navigate to the send screen and update the state`() = runTest {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick)
            assertEquals(
                VaultUnlockedNavBarEvent.NavigateToSendScreen(returnToSubgraphRoot = false),
                awaitItem(),
            )
        }
        assertEquals(
            VaultUnlockedNavBarState(
                vaultNavBarLabelRes = R.string.my_vault,
                vaultNavBarContentDescriptionRes = R.string.my_vault,
                currentTab = BottomNavDestination.SEND,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `GeneratorTabClick should navigate to the generator screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick)
            assertEquals(
                VaultUnlockedNavBarEvent.NavigateToGeneratorScreen(returnToSubgraphRoot = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SettingsTabClick should navigate to the settings screen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick)
            assertEquals(
                VaultUnlockedNavBarEvent.NavigateToSettingsScreen(returnToSubgraphRoot = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `if state exists in SavedStateHandle apply as the initial state`() {
        val savedState = DEFAULT_STATE.copy(currentTab = BottomNavDestination.SEND)
        val viewModel = createViewModel(savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    private fun createViewModel(initialState: VaultUnlockedNavBarState = DEFAULT_STATE) =
        VaultUnlockedNavBarViewModel(
            authRepository = authRepository,
            specialCircumstancesManager = specialCircumstancesManager,
            savedStateHandle = SavedStateHandle(initialState = mapOf("state" to initialState)),
        )
}

private val DEFAULT_STATE = VaultUnlockedNavBarState(
    vaultNavBarLabelRes = R.string.my_vault,
    vaultNavBarContentDescriptionRes = R.string.my_vault,
    currentTab = BottomNavDestination.VAULT,
)
