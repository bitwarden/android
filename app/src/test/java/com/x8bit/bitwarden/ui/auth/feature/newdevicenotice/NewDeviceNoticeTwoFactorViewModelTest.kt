package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.TurnOnTwoFactorDialog
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewDeviceNoticeTwoFactorViewModelTest : BaseViewModelTest() {
    private val environmentRepository = FakeEnvironmentRepository()
    private val authRepository = mockk<AuthRepository> {
        every { getNewDeviceNoticeState() } returns NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN,
            lastSeenDate = null,
        )
        every { setNewDeviceNoticeState(any()) } just runs
    }

    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.NewDevicePermanentDismiss) } returns false
        every { getFeatureFlag(FlagKey.NewDeviceTemporaryDismiss) } returns true
    }

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
        )
}

private val DEFAULT_STATE =
    NewDeviceNoticeTwoFactorState(
        shouldShowRemindMeLater = true,
        dialogState = null,
    )
