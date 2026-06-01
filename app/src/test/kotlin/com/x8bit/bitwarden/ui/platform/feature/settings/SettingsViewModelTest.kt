package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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

    private val mutablePlanRowEligibleFlow = MutableStateFlow(false)
    private val mutableUpgradedToPremiumCardEligibleFlow = MutableStateFlow(false)
    private val mutableIsSelfHostedFlow = MutableStateFlow(false)
    private val premiumStateManager: PremiumStateManager = mockk(relaxed = true) {
        every { isPlanRowEligibleFlow } returns mutablePlanRowEligibleFlow
        every {
            isUpgradedToPremiumCardEligibleFlow
        } returns mutableUpgradedToPremiumCardEligibleFlow
        every { isSelfHostedFlow } returns mutableIsSelfHostedFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toSettingsArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toSettingsArgs)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.CloseClick)
            assertEquals(SettingsEvent.NavigateBack, awaitItem())
        }
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
    fun `on SettingsClick with PLAN should emit NavigatePlan`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    SettingsAction.SettingsClick(Settings.PLAN),
                )
                assertEquals(SettingsEvent.NavigatePlan, awaitItem())
            }
        }

    @Test
    fun `initial state reflects the current state of the repository`() {
        mutableAutofillBadgeCountFlow.update { 1 }
        mutableSecurityBadgeCountFlow.update { 2 }
        mutableVaultBadgeCountFlow.update { 3 }
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
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
                DEFAULT_STATE.copy(
                    autoFillCount = 0,
                    securityCount = 0,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableSecurityBadgeCountFlow.update { 2 }
            assertEquals(
                DEFAULT_STATE.copy(
                    autoFillCount = 0,
                    securityCount = 2,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableAutofillBadgeCountFlow.update { 1 }
            assertEquals(
                DEFAULT_STATE.copy(
                    autoFillCount = 1,
                    securityCount = 2,
                    vaultCount = 0,
                ),
                awaitItem(),
            )

            mutableVaultBadgeCountFlow.update { 3 }
            assertEquals(
                DEFAULT_STATE.copy(
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

    @Test
    fun `Plan row should appear when plan row eligibility is true and not preAuth`() {
        mutablePlanRowEligibleFlow.value = true
        val viewModel = createViewModel()
        assertTrue(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )
    }

    @Test
    fun `Plan row should be hidden when plan row eligibility is false`() {
        mutablePlanRowEligibleFlow.value = false
        val viewModel = createViewModel()
        assertFalse(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )
    }

    @Test
    fun `Plan row should be hidden in preAuth mode`() {
        mutablePlanRowEligibleFlow.value = true
        val viewModel = createViewModel(isPreAuth = true)
        assertFalse(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )
    }

    @Test
    fun `Plan row should appear between Appearance and Other in settings rows`() {
        mutablePlanRowEligibleFlow.value = true
        val viewModel = createViewModel()
        val rows = viewModel.stateFlow.value.settingRows
        val planIndex = rows.indexOf(Settings.PLAN)
        val appearanceIndex = rows.indexOf(Settings.APPEARANCE)
        val otherIndex = rows.indexOf(Settings.OTHER)
        assertTrue(planIndex > appearanceIndex)
        assertTrue(planIndex < otherIndex)
    }

    @Test
    fun `Plan row should update when plan row eligibility changes to true`() =
        runTest {
            mutablePlanRowEligibleFlow.value = false
            val viewModel = createViewModel()
            assertFalse(
                viewModel.stateFlow.value.settingRows
                    .contains(Settings.PLAN),
            )

            mutablePlanRowEligibleFlow.value = true
            viewModel.stateFlow.test {
                assertTrue(
                    awaitItem().settingRows.contains(Settings.PLAN),
                )
            }
        }

    @Test
    fun `Upgraded to Premium card eligibility flow updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableUpgradedToPremiumCardEligibleFlow.value = true
            assertEquals(
                DEFAULT_STATE.copy(isUpgradedToPremiumCardEligible = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun `shouldShowUpgradedToPremiumCard is false in pre-auth even when eligible`() = runTest {
        mutableUpgradedToPremiumCardEligibleFlow.value = true
        val viewModel = createViewModel(isPreAuth = true)
        assertFalse(viewModel.stateFlow.value.shouldShowUpgradedToPremiumCard)
    }

    @Test
    fun `shouldShowUpgradedToPremiumCard is true post-auth when eligible`() = runTest {
        mutableUpgradedToPremiumCardEligibleFlow.value = true
        val viewModel = createViewModel(isPreAuth = false)
        assertTrue(viewModel.stateFlow.value.shouldShowUpgradedToPremiumCard)
    }

    @Test
    fun `UpgradedToPremiumCardClick dismisses card and emits NavigateToUrl`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.UpgradedToPremiumCardClick)
            assertEquals(
                SettingsEvent.NavigateToUrl(
                    url = "https://bitwarden.com/help/password-manager-plans/",
                ),
                awaitItem(),
            )
        }
        verify(exactly = 1) {
            premiumStateManager.dismissUpgradedToPremiumCard()
        }
    }

    @Test
    fun `UpgradedToPremiumCardDismiss dismisses card without navigating`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(SettingsAction.UpgradedToPremiumCardDismiss)
        verify(exactly = 1) {
            premiumStateManager.dismissUpgradedToPremiumCard()
        }
    }

    @Test
    fun `Plan row should be hidden when self-hosted status flow emits true`() {
        mutablePlanRowEligibleFlow.value = true
        mutableIsSelfHostedFlow.value = true
        val viewModel = createViewModel()
        assertFalse(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )
    }

    @Test
    fun `Plan row should update when self-hosted status flow flips to true`() = runTest {
        mutablePlanRowEligibleFlow.value = true
        val viewModel = createViewModel()
        assertTrue(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )

        mutableIsSelfHostedFlow.value = true
        viewModel.stateFlow.test {
            assertFalse(
                awaitItem().settingRows.contains(Settings.PLAN),
            )
        }
    }

    @Test
    fun `Plan row should re-appear when self-hosted status flow flips back to false`() = runTest {
        // Simulates the debug-disable flag being toggled on while self-hosted: the flow emits
        // false even though the environment is still self-hosted.
        mutablePlanRowEligibleFlow.value = true
        mutableIsSelfHostedFlow.value = true
        val viewModel = createViewModel()
        assertFalse(
            viewModel.stateFlow.value.settingRows
                .contains(Settings.PLAN),
        )

        mutableIsSelfHostedFlow.value = false
        viewModel.stateFlow.test {
            assertTrue(
                awaitItem().settingRows.contains(Settings.PLAN),
            )
        }
    }

    private fun createViewModel(isPreAuth: Boolean = false) = SettingsViewModel(
        firstTimeActionManager = firstTimeManager,
        specialCircumstanceManager = specialCircumstanceManager,
        premiumStateManager = premiumStateManager,
        savedStateHandle = SavedStateHandle().apply {
            every { toSettingsArgs() } returns SettingsArgs(isPreAuth = isPreAuth)
        },
    )
}

private val DEFAULT_STATE = SettingsState(
    isPreAuth = false,
    autoFillCount = 0,
    securityCount = 0,
    vaultCount = 0,
    isPlanRowEligible = false,
)
