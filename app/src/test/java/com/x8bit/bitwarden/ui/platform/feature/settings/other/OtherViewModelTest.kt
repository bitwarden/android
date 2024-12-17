package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OtherViewModelTest : BaseViewModelTest() {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val mutablePullToRefreshStateFlow = MutableStateFlow(false)
    private val instant: Instant = Instant.parse("2023-10-26T12:00:00Z")
    private val isAllowed: Boolean = false
    private val mutableVaultLastSyncStateFlow = MutableStateFlow(instant)
    private val mutableScreenCaptureAllowedStateFlow = MutableStateFlow(isAllowed)
    private val settingsRepository = mockk<SettingsRepository> {
        every { clearClipboardFrequency } returns ClearClipboardFrequency.NEVER
        every { clearClipboardFrequency = any() } just runs
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshStateFlow
        every { vaultLastSyncStateFlow } returns mutableVaultLastSyncStateFlow
        every { vaultLastSync } returns instant
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedStateFlow
        every { isScreenCaptureAllowed } answers { mutableScreenCaptureAllowedStateFlow.value }
        every { isScreenCaptureAllowed = any() } just runs
    }
    private val vaultRepository = mockk<VaultRepository>()
    private val networkConnectionManager = mockk<NetworkConnectionManager> {
        every { isNetworkConnected } returns true
    }

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            clearClipboardFrequency = ClearClipboardFrequency.FIVE_MINUTES,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on AllowScreenCaptureToggled should update value in state and SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()
            val newScreenCaptureAllowedValue = true

            viewModel.trySendAction(
                OtherAction.AllowScreenCaptureToggle(
                    newScreenCaptureAllowedValue,
                ),
            )

            verify(exactly = 1) {
                settingsRepository.isScreenCaptureAllowed = newScreenCaptureAllowedValue
            }

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(allowScreenCapture = true),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on AllowSyncToggled should update value in state`() {
        every {
            settingsRepository.storePullToRefreshEnabled(isPullToRefreshEnabled = true)
        } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(OtherAction.AllowSyncToggle(true))
        assertEquals(
            DEFAULT_STATE.copy(allowSyncOnRefresh = true),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            settingsRepository.storePullToRefreshEnabled(isPullToRefreshEnabled = true)
        }
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(OtherAction.BackClick)
            assertEquals(OtherEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on ClearClipboardFrequencyChange should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(
                OtherAction.ClearClipboardFrequencyChange(
                    clearClipboardFrequency = ClearClipboardFrequency.ONE_MINUTE,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    clearClipboardFrequency = ClearClipboardFrequency.ONE_MINUTE,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on VaultLastSyncReceive should sync repo`() = runTest {
        val newSyncTime = Instant.parse("2023-10-27T12:00:00Z")
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableVaultLastSyncStateFlow.tryEmit(newSyncTime)
            assertEquals(
                DEFAULT_STATE.copy(
                    lastSyncTime = "10/27/2023 12:00 PM",
                    dialogState = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SyncNowButtonClick should sync repo`() = runTest {
        every { vaultRepository.sync(forced = true) } just runs
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(OtherAction.SyncNowButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = OtherState.DialogState.Loading(
                        message = R.string.syncing.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        verify { vaultRepository.sync(forced = true) }
    }

    @Test
    fun `ManualVaultSyncReceive should emit ShowToast`() = runTest {
        val newSyncTime = Instant.parse("2023-10-27T12:00:00Z")
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            mutableVaultLastSyncStateFlow.tryEmit(newSyncTime)
            assertEquals(
                OtherEvent.ShowToast(R.string.syncing_complete.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SyncNowButtonClick should show error dialog if no network connection`() = runTest {
        every { networkConnectionManager.isNetworkConnected } returns false
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(OtherAction.SyncNowButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = OtherState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        verify(exactly = 0) { vaultRepository.sync(forced = true) }
    }

    private fun createViewModel(
        state: OtherState? = null,
    ) = OtherViewModel(
        clock = clock,
        settingsRepo = settingsRepository,
        vaultRepo = vaultRepository,
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
        networkConnectionManager = networkConnectionManager,
    )

    companion object {
        private val DEFAULT_STATE = OtherState(
            allowScreenCapture = false,
            allowSyncOnRefresh = false,
            clearClipboardFrequency = ClearClipboardFrequency.NEVER,
            lastSyncTime = "10/26/2023 12:00 PM",
            dialogState = null,
        )
    }
}
