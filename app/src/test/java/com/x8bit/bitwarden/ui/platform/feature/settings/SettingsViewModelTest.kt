package com.x8bit.bitwarden.ui.platform.feature.settings

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
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
import org.junit.jupiter.api.Test

class SettingsViewModelTest : BaseViewModelTest() {

    private val mutableAutofillBadgeCountFlow = MutableStateFlow(0)
    private val mutableVaultBadgeCountFlow = MutableStateFlow(0)
    private val mutableSecurityBadgeCountFlow = MutableStateFlow(0)
    private val firstTimeManager = mockk<FirstTimeActionManager> {
        every { allSecuritySettingsBadgeCountFlow } returns mutableSecurityBadgeCountFlow
        every { allAutofillSettingsBadgeCountFlow } returns mutableAutofillBadgeCountFlow
        every { allVaultSettingsBadgeCountFlow } returns mutableVaultBadgeCountFlow
    }
    private val specialCircumstanceManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance } returns null
    }

    @Test
    fun `on SettingsClick with ABOUT should emit NavigateAbout`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT))
            assertEquals(SettingsEvent.NavigateAbout, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with ACCOUNT_SECURITY should emit NavigateAccountSecurity`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
            assertEquals(SettingsEvent.NavigateAccountSecurity, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with APPEARANCE should emit NavigateAppearance`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE))
            assertEquals(SettingsEvent.NavigateAppearance, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with AUTO_FILL should emit NavigateAutoFill`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL))
            assertEquals(SettingsEvent.NavigateAutoFill, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with OTHER should emit NavigateOther`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER))
            assertEquals(SettingsEvent.NavigateOther, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with VAULT should emit NavigateVault`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT))
            assertEquals(SettingsEvent.NavigateVault, awaitItem())
        }
    }

    @Test
    fun `initial state reflects the current state of the repository`() {
        mutableAutofillBadgeCountFlow.update { 1 }
        mutableSecurityBadgeCountFlow.update { 2 }
        mutableVaultBadgeCountFlow.update { 3 }
        val viewModel = createViewModel()
        assertEquals(
            SettingsState(
                autoFillCount = 1,
                securityCount = 2,
                vaultCount = 3,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `State updates when repository emits new values for badge counts`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                SettingsState(
                    autoFillCount = 0,
                    securityCount = 0,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableSecurityBadgeCountFlow.update { 2 }
            assertEquals(
                SettingsState(
                    autoFillCount = 0,
                    securityCount = 2,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableAutofillBadgeCountFlow.update { 1 }
            assertEquals(
                SettingsState(
                    autoFillCount = 1,
                    securityCount = 2,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableVaultBadgeCountFlow.update { 3 }
            assertEquals(
                SettingsState(
                    autoFillCount = 1,
                    securityCount = 2,
                    vaultCount = 3,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `init should send NavigateAccountSecurityShortcut when special circumstance is AccountSecurityShortcut`() =
        runTest {
            every {
                specialCircumstanceManager.specialCircumstance
            } returns SpecialCircumstance.AccountSecurityShortcut
            every { specialCircumstanceManager.specialCircumstance = null } just runs
            createViewModel().eventFlow.test {
                assertEquals(
                    SettingsEvent.NavigateAccountSecurityShortcut, awaitItem(),
                )
            }
            verify { specialCircumstanceManager.specialCircumstance = null }
        }

    private fun createViewModel() = SettingsViewModel(
        firstTimeActionManager = firstTimeManager,
        specialCircumstanceManager = specialCircumstanceManager,
    )
}
