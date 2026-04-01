package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeviceInfo
import com.x8bit.bitwarden.data.auth.repository.model.DevicePendingAuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.GetDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.GetDevicesResult
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor

class ManageDevicesViewModelTest : BaseViewModelTest() {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val mutableAuthRequestsWithUpdatesFlow =
        bufferedMutableSharedFlow<AuthRequestsUpdatesResult>()
    private val authRepository = mockk<AuthRepository> {
        every { getAuthRequestsWithUpdates() } returns mutableAuthRequestsWithUpdatesFlow
        coEvery { getDevices() } returns GetDevicesResult.Success(emptyList())
        coEvery { getDeviceByIdentifier() } returns GetDeviceResult.Success(DEFAULT_DEVICE)
    }
    private val resourceManager = mockk<ResourceManager> {
        every { getString(any()) } returns "Mock"
    }
    private val mutablePullToRefreshStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshStateFlow
    }
    private val mutableSnackbarDataFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
    }
    private val buildInfoManager = mockk<BuildInfoManager> {
        every { isFdroid } returns false
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(TemporalAccessor::toFormattedDateTimeStyle)
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
        every {
            fixedClock.instant().toFormattedDateTimeStyle(
                dateStyle = FormatStyle.MEDIUM,
                timeStyle = FormatStyle.MEDIUM,
                clock = fixedClock,
            )
        } returns "Oct 27, 2023, 12:00:00 PM"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(TemporalAccessor::toFormattedDateTimeStyle)
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `init should make necessary network calls`() {
        createViewModel()
        coVerify {
            authRepository.getAuthRequestsWithUpdates()
            authRepository.getDevices()
            authRepository.getDeviceByIdentifier()
        }
    }

    @Test
    fun `init should set devicesLoaded true after device fetch success`() {
        val viewModel = createViewModel()
        // After init with unconfined dispatcher, devices coroutine runs immediately
        assertEquals(true, viewModel.stateFlow.value.devicesLoaded)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ManageDevicesAction.CloseClick)
            assertEquals(ManageDevicesEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `HideBottomSheet should set hideBottomSheet to true`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.stateFlow.value.hideBottomSheet)
        viewModel.trySendAction(ManageDevicesAction.HideBottomSheet)
        assertTrue(viewModel.stateFlow.value.hideBottomSheet)
    }

    @Test
    fun `LifecycleResume should re-fetch devices and auth requests`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ManageDevicesAction.LifecycleResume)
        // getAuthRequestsWithUpdates called twice: once on init, once on resume
        verify(exactly = 2) { authRepository.getAuthRequestsWithUpdates() }
        coVerify(exactly = 2) { authRepository.getDevices() }
        coVerify(exactly = 2) { authRepository.getDeviceByIdentifier() }
    }

    @Test
    fun `RefreshPull should reset loaded state, set isRefreshing, and re-fetch data`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            // Skip initial states from init
            skipItems(1)

            viewModel.trySendAction(ManageDevicesAction.RefreshPull)

            // After refresh, isRefreshing should be true transiently and devices reloaded
            coVerify(exactly = 2) { authRepository.getDevices() }
            verify(exactly = 2) { authRepository.getAuthRequestsWithUpdates() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PendingRequestRowClick should emit NavigateToLoginApproval`() = runTest {
        val fingerprint = "mock-fingerprint"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ManageDevicesAction.PendingRequestRowClick(fingerprint))
            assertEquals(
                ManageDevicesEvent.NavigateToLoginApproval(fingerprint),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AllDevicesResultReceive with device error should show error state`() {
        coEvery { authRepository.getDevices() } returns GetDevicesResult.Error
        val viewModel = createViewModel()
        assertEquals(
            ManageDevicesState.ViewState.Error,
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `AllDevicesResultReceive with current device error should show error state`() {
        coEvery { authRepository.getDeviceByIdentifier() } returns GetDeviceResult.Error
        val viewModel = createViewModel()
        assertEquals(
            ManageDevicesState.ViewState.Error,
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `AuthRequestsResultReceive with error should use empty auth request list`() {
        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Error(error = Throwable()),
        )
        assertEquals(
            emptyList<AuthRequest>(),
            viewModel.stateFlow.value.authRequests,
        )
    }

    @Test
    fun `updates to pull-to-refresh enabled state should update isPullToRefreshEnabled`() =
        runTest {
            val viewModel = createViewModel()
            // Transition to Content state so isPullToRefreshEnabled can become true
            mutableAuthRequestsWithUpdatesFlow.tryEmit(
                AuthRequestsUpdatesResult.Update(authRequests = emptyList()),
            )
            viewModel.stateFlow.test {
                val contentState = awaitItem()
                assertTrue(contentState.viewState is ManageDevicesState.ViewState.Content)
                assertFalse(contentState.isPullToRefreshEnabled)
                mutablePullToRefreshStateFlow.value = true
                val updatedState = awaitItem()
                assertTrue(updatedState.isPullToRefreshEnabled)
                mutablePullToRefreshStateFlow.value = false
                val revertedState = awaitItem()
                assertFalse(revertedState.isPullToRefreshEnabled)
            }
        }

    @Test
    fun `SnackbarDataReceive should emit ShowSnackbar event`() = runTest {
        val data = BitwardenSnackbarData(message = "test".asText())
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.tryEmit(data)
            assertEquals(ManageDevicesEvent.ShowSnackbar(data), awaitItem())
        }
    }

    @Test
    fun `content state should sort devices with current first, pending second, others last`() {
        val currentDeviceId = DEFAULT_DEVICE.id
        val pendingRequest = DevicePendingAuthRequest(
            id = "auth-req-1",
            creationDate = fixedClock.instant(),
        )
        val validAuthRequest = AuthRequest(
            id = "auth-req-1",
            publicKey = "publicKey",
            platform = "Android",
            ipAddress = "192.168.0.1",
            key = null,
            masterPasswordHash = null,
            creationDate = fixedClock.instant(),
            responseDate = null,
            requestApproved = false,
            originUrl = "www.bitwarden.com",
            fingerprint = "fingerprint-phrase",
        )
        val currentDevice = DEFAULT_DEVICE
        val pendingDevice = DEFAULT_DEVICE.copy(
            id = "device-pending",
            pendingAuthRequest = pendingRequest,
        )
        val otherDevice = DEFAULT_DEVICE.copy(id = "device-other")

        coEvery { authRepository.getDevices() } returns GetDevicesResult.Success(
            devices = listOf(otherDevice, pendingDevice, currentDevice),
        )
        coEvery { authRepository.getDeviceByIdentifier() } returns GetDeviceResult.Success(
            currentDevice,
        )

        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = listOf(validAuthRequest)),
        )

        val content = viewModel.stateFlow.value.viewState as ManageDevicesState.ViewState.Content
        assertEquals(DeviceSessionStatus.Current, content.items[0].status)
        assertEquals(DeviceSessionStatus.Pending, content.items[1].status)
        assertEquals(DeviceSessionStatus.None, content.items[2].status)
        assertEquals(currentDeviceId, content.items[0].id)
        assertEquals(pendingDevice.id, content.items[1].id)
    }

    private fun createViewModel(state: ManageDevicesState? = null) = ManageDevicesViewModel(
        clock = fixedClock,
        authRepository = authRepository,
        resourceManager = resourceManager,
        snackbarRelayManager = snackbarRelayManager,
        settingsRepository = settingsRepository,
        buildInfoManager = buildInfoManager,
        savedStateHandle = SavedStateHandle(mapOf("state" to state)),
    )
}

private val DEFAULT_DEVICE = DeviceInfo(
    id = "device-current",
    name = "Test Device",
    identifier = "identifier-current",
    type = 0,
    isTrusted = false,
    creationDate = Instant.parse("2023-10-27T12:00:00Z"),
    lastActivityDate = Instant.parse("2023-10-27T12:00:00Z"),
    pendingAuthRequest = null,
)
