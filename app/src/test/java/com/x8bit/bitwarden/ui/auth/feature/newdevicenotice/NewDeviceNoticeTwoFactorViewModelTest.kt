package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.TurnOnTwoFactorDialog
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NewDeviceNoticeTwoFactorViewModelTest : BaseViewModelTest() {
    private val environmentRepository = FakeEnvironmentRepository()
    private val authRepository = mockk<AuthRepository>(relaxed = true) {
        every { checkUserNeedsNewDeviceTwoFactorNotice() } returns true
    }

    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.NewDevicePermanentDismiss) } returns false
        every { getFeatureFlag(FlagKey.NewDeviceTemporaryDismiss) } returns true
    }

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private val vaultRepository = mockk<VaultRepository>(relaxed = true)

    @Test
    fun `initial state should be correct with NewDevicePermanentDismiss flag false`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should be correct with NewDevicePermanentDismiss flag true`() = runTest {
        every { featureFlagManager.getFeatureFlag(FlagKey.NewDevicePermanentDismiss) } returns true
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(shouldShowRemindMeLater = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `Init should not send events if user needs new device notice`() = runTest {
        every { authRepository.checkUserNeedsNewDeviceTwoFactorNotice() } returns true
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            expectNoEvents()
        }
    }

    @Test
    fun `Init should send NavigateBackToVault if user does not need new device notice`() = runTest {
        every { authRepository.checkUserNeedsNewDeviceTwoFactorNotice() } returns false
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            assertEquals(
                NewDeviceNoticeTwoFactorEvent.NavigateBackToVault,
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial state should be correct with email from state handle`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `ChangeAccountEmailClick should should change dialog state to ChangeAccountEmailDialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick)
                assertEquals(
                    DEFAULT_STATE.copy(dialogState = ChangeAccountEmailDialog),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `TurnOnTwoFactorClick should should change dialog state to TurnOnTwoFactorDialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick)
                assertEquals(
                    DEFAULT_STATE.copy(dialogState = TurnOnTwoFactorDialog),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `DismissDialogClick should should change dialog state to null`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick)
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.DismissDialogClick)
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `RemindMeLaterClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.RemindMeLaterClick)
            assertEquals(
                NewDeviceNoticeTwoFactorEvent.NavigateBackToVault,
                awaitItem(),
            )
            verify(exactly = 1) {
                authRepository.setNewDeviceNoticeState(
                    NewDeviceNoticeState(
                        displayStatus = NewDeviceNoticeDisplayStatus.HAS_SEEN,
                        lastSeenDate = ZonedDateTime.now(FIXED_CLOCK),
                    ),
                )
            }
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueDialogClick should emit NavigateToTurnOnTwoFactor if dialog state is TurnOnTwoFactorDialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick)
            viewModel.eventFlow.test {
                viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ContinueDialogClick)
                assertEquals(
                    NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor(
                        url = "https://vault.bitwarden.com/#/settings/security/two-factor",
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE,
                    viewModel.stateFlow.value,
                )
                verify(exactly = 1) {
                    settingsRepository.vaultLastSync = null
                }
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueDialogClick should emit NavigateToChangeAccountEmail if dialog state is ChangeAccountEmailClick`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick)
            viewModel.eventFlow.test {
                viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ContinueDialogClick)
                assertEquals(
                    NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail(
                        url = "https://vault.bitwarden.com/#/settings/account",
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE,
                    viewModel.stateFlow.value,
                )
                verify(exactly = 1) {
                    settingsRepository.vaultLastSync = null
                }
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueDialogClick should return if dialog state is null`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ContinueDialogClick)
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
        }
    }

    private fun createViewModel(): NewDeviceNoticeTwoFactorViewModel =
        NewDeviceNoticeTwoFactorViewModel(
            authRepository = authRepository,
            environmentRepository = environmentRepository,
            featureFlagManager = featureFlagManager,
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
            clock = FIXED_CLOCK,
        )
}

private val DEFAULT_STATE =
    NewDeviceNoticeTwoFactorState(
        shouldShowRemindMeLater = true,
        dialogState = null,
    )

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
