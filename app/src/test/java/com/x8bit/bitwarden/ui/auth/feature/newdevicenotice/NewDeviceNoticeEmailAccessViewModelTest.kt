package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewDeviceNoticeEmailAccessViewModelTest : BaseViewModelTest() {
    private val authRepository = mockk<AuthRepository> {
        every { getNewDeviceNoticeState() } returns NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN,
            lastSeenDate = null,
        )
        every { setNewDeviceNoticeState(any()) } just runs
        every { checkUserNeedsNewDeviceTwoFactorNotice() } returns true
    }

    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.NewDevicePermanentDismiss) } returns true
        every { getFeatureFlag(FlagKey.NewDeviceTemporaryDismiss) } returns true
    }

    @Test
    fun `initial state should be correct with email from state handle`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `EmailAccessToggle should update value of isEmailAccessEnabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.EmailAccessToggle(true))
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isEmailAccessEnabled = true),
            )
        }
    }

    @Test
    fun `ContinueClick should emit NavigateBackToVault if isEmailAccessEnabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.EmailAccessToggle(true))
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.ContinueClick)
            assertEquals(
                NewDeviceNoticeEmailAccessEvent.NavigateBackToVault,
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueClick should emit NavigateBackToVault if isEmailAccessEnabled and NewDevicePermanentDismiss flag is off`() = runTest {
        every { featureFlagManager.getFeatureFlag(FlagKey.NewDevicePermanentDismiss) } returns false

        val viewModel = createViewModel()
        viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.EmailAccessToggle(true))
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.ContinueClick)
            assertEquals(
                NewDeviceNoticeEmailAccessEvent.NavigateBackToVault,
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueClick should emit NavigateToTwoFactorOptions if isEmailAccessEnabled is false`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.ContinueClick)
            assertEquals(
                NewDeviceNoticeEmailAccessEvent.NavigateToTwoFactorOptions,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle().also {
            it["email_address"] = EMAIL
        },
    ): NewDeviceNoticeEmailAccessViewModel = NewDeviceNoticeEmailAccessViewModel(
        authRepository = authRepository,
        featureFlagManager = featureFlagManager,
        savedStateHandle = savedStateHandle,
    )
}

private const val EMAIL = "active@bitwarden.com"

private val DEFAULT_STATE =
    NewDeviceNoticeEmailAccessState(
        email = EMAIL,
        isEmailAccessEnabled = false,
    )
