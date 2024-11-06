package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PendingRequestsViewModelTest : BaseViewModelTest() {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val mutableAuthRequestsWithUpdatesFlow =
        bufferedMutableSharedFlow<AuthRequestsUpdatesResult>()
    private val authRepository = mockk<AuthRepository> {
        // This is called during init, anything that cares about this will handle it
        coEvery { getAuthRequestsWithUpdates() } returns mutableAuthRequestsWithUpdatesFlow
    }
    private val mutablePullToRefreshStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshStateFlow
    }

    @Test
    fun `init should call getAuthRequestsWithUpdates`() {
        createViewModel(state = null)
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = emptyList()),
        )
        coVerify {
            authRepository.getAuthRequestsWithUpdates()
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `getPendingResults success with content should update state with some requests filtered`() {
        val dateTimeFormatter = DateTimeFormatter
            .ofPattern("M/d/yy hh:mm a")
            .withZone(fixedClock.zone)
        val nowZonedDateTime = ZonedDateTime.now(fixedClock)
        val requestList = listOf(
            AuthRequest(
                id = "1",
                publicKey = "publicKey-1",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = nowZonedDateTime,
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = "pantry-overdue-survive-sleep-jab",
            ),
            AuthRequest(
                id = "2",
                publicKey = "publicKey-2",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = nowZonedDateTime,
                responseDate = null,
                requestApproved = true,
                originUrl = "www.bitwarden.com",
                fingerprint = "fingerprint",
            ),
            AuthRequest(
                id = "3",
                publicKey = "publicKey-3",
                platform = "iOS",
                ipAddress = "192.168.0.2",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = nowZonedDateTime.minusMinutes(10),
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = "erupt-anew-matchbook-disk-student",
            ),
            AuthRequest(
                id = "4",
                publicKey = "publicKey-4",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = nowZonedDateTime,
                responseDate = nowZonedDateTime,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = "fingerprint",
            ),
        )
        val expected = DEFAULT_STATE.copy(
            authRequests = requestList,
            viewState = PendingRequestsState.ViewState.Content(
                requests = listOf(
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "pantry-overdue-survive-sleep-jab",
                        platform = "Android",
                        timestamp = nowZonedDateTime.format(dateTimeFormatter),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = requestList),
        )
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `getPendingResults success with empty list should update state`() {
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = emptyList(),
        )
        val expected = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Empty,
        )
        val viewModel = createViewModel()
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `getPendingResults failure with error should update state`() {
        val expected = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Error,
        )
        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(AuthRequestsUpdatesResult.Error)
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PendingRequestsAction.CloseClick)
            assertEquals(PendingRequestsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on HideBottomSheet should make hideBottomSheet true`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(PendingRequestsAction.HideBottomSheet)
        assertEquals(DEFAULT_STATE.copy(hideBottomSheet = true), viewModel.stateFlow.value)
    }

    @Test
    fun `on RefreshPull should make auth request`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(PendingRequestsAction.RefreshPull)
        coVerify(exactly = 2) {
            // This should be called twice since we also call it on init
            authRepository.getAuthRequestsWithUpdates()
        }
    }

    @Test
    fun `updates to pull-to-refresh enabled state should update isPullToRefreshEnabled`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                mutablePullToRefreshStateFlow.value = true
                assertEquals(DEFAULT_STATE.copy(isPullToRefreshSettingEnabled = true), awaitItem())
                mutablePullToRefreshStateFlow.value = false
                assertEquals(DEFAULT_STATE.copy(isPullToRefreshSettingEnabled = false), awaitItem())
            }
        }

    @Test
    fun `on PendingRequestRowClick should emit NavigateToLoginApproval`() = runTest {
        val fingerprint = "fingerprint"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                PendingRequestsAction.PendingRequestRowClick(fingerprint),
            )
            assertEquals(PendingRequestsEvent.NavigateToLoginApproval(fingerprint), awaitItem())
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `on DeclineAllRequestsConfirm should update all auth requests to declined`() = runTest {
        val authRequest1 = AuthRequest(
            id = "2",
            publicKey = "publicKey-2",
            platform = "iOS",
            ipAddress = "192.168.0.2",
            key = "publicKey",
            masterPasswordHash = "verySecureHash",
            creationDate = ZonedDateTime.now().minusMinutes(5),
            responseDate = null,
            requestApproved = false,
            originUrl = "www.bitwarden.com",
            fingerprint = "erupt-anew-matchbook-disk-student",
        )
        val authRequest2 = AuthRequest(
            id = "3",
            publicKey = "publicKey-3",
            platform = "Android",
            ipAddress = "192.168.0.3",
            key = "publicKey",
            masterPasswordHash = "verySecureHash",
            creationDate = ZonedDateTime.now(),
            responseDate = null,
            requestApproved = false,
            originUrl = "www.bitwarden.com",
            fingerprint = "pantry-overdue-survive-sleep-jab",
        )
        coEvery {
            authRepository.updateAuthRequest(
                requestId = "2",
                masterPasswordHash = "verySecureHash",
                publicKey = "publicKey-2",
                isApproved = false,
            )
        } returns AuthRequestResult.Success(
            authRequest1.copy(
                responseDate = ZonedDateTime.now(),
            ),
        )
        coEvery {
            authRepository.updateAuthRequest(
                requestId = "3",
                masterPasswordHash = "verySecureHash",
                publicKey = "publicKey-3",
                isApproved = false,
            )
        } returns AuthRequestResult.Success(
            authRequest2.copy(
                responseDate = ZonedDateTime.now(),
            ),
        )
        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = listOf(authRequest1, authRequest2)),
        )
        viewModel.trySendAction(PendingRequestsAction.DeclineAllRequestsConfirm)

        coVerify {
            authRepository.updateAuthRequest(
                requestId = "2",
                masterPasswordHash = "verySecureHash",
                publicKey = "publicKey-2",
                isApproved = false,
            )
            authRepository.updateAuthRequest(
                requestId = "3",
                masterPasswordHash = "verySecureHash",
                publicKey = "publicKey-3",
                isApproved = false,
            )
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `on LifecycleResume should update state`() = runTest {
        val dateTimeFormatter = DateTimeFormatter
            .ofPattern("M/d/yy hh:mm a")
            .withZone(fixedClock.zone)
        val nowZonedDateTime = ZonedDateTime.now()
        val fiveMinZonedDateTime = ZonedDateTime.now().minusMinutes(5)
        val sixMinZonedDateTime = ZonedDateTime.now().minusMinutes(6)
        val requestList = listOf(
            AuthRequest(
                id = "1",
                publicKey = "publicKey-1",
                platform = "Android",
                ipAddress = "192.168.0.1",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = sixMinZonedDateTime,
                responseDate = null,
                requestApproved = true,
                originUrl = "www.bitwarden.com",
                fingerprint = "fingerprint",
            ),
            AuthRequest(
                id = "2",
                publicKey = "publicKey-2",
                platform = "iOS",
                ipAddress = "192.168.0.2",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = fiveMinZonedDateTime,
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = "erupt-anew-matchbook-disk-student",
            ),
            AuthRequest(
                id = "3",
                publicKey = "publicKey-3",
                platform = "Android",
                ipAddress = "192.168.0.3",
                key = "publicKey",
                masterPasswordHash = "verySecureHash",
                creationDate = nowZonedDateTime,
                responseDate = null,
                requestApproved = false,
                originUrl = "www.bitwarden.com",
                fingerprint = "pantry-overdue-survive-sleep-jab",
            ),
        )
        val viewModel = createViewModel()
        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = emptyList()),
        )
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableAuthRequestsWithUpdatesFlow.tryEmit(
            AuthRequestsUpdatesResult.Update(authRequests = requestList),
        )
        val expected = DEFAULT_STATE.copy(
            authRequests = requestList,
            viewState = PendingRequestsState.ViewState.Content(
                requests = listOf(
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "pantry-overdue-survive-sleep-jab",
                        platform = "Android",
                        timestamp = nowZonedDateTime.format(dateTimeFormatter),
                    ),
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "erupt-anew-matchbook-disk-student",
                        platform = "iOS",
                        timestamp = fiveMinZonedDateTime.format(dateTimeFormatter),
                    ),
                ),
            ),
        )
        viewModel.trySendAction(PendingRequestsAction.LifecycleResume)
        assertEquals(expected, viewModel.stateFlow.value)

        coVerify(exactly = 2) {
            authRepository.getAuthRequestsWithUpdates()
        }
    }

    private fun createViewModel(
        state: PendingRequestsState? = DEFAULT_STATE,
    ): PendingRequestsViewModel = PendingRequestsViewModel(
        clock = fixedClock,
        authRepository = authRepository,
        settingsRepository = settingsRepository,
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: PendingRequestsState = PendingRequestsState(
    authRequests = emptyList(),
    viewState = PendingRequestsState.ViewState.Empty,
    isPullToRefreshSettingEnabled = false,
    isRefreshing = false,
    hideBottomSheet = false,
)
