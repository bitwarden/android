package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestsResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class PendingRequestsViewModelTest : BaseViewModelTest() {

    private val authRepository = mockk<AuthRepository>()

    @BeforeEach
    fun setup() {
        // Setting the timezone so the tests pass consistently no matter the environment.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterEach
    fun tearDown() {
        // Clearing the timezone after the test.
        TimeZone.setDefault(null)
    }

    @Test
    fun `initial state should be correct and trigger a getAuthRequests call`() {
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = emptyList(),
        )
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        coVerify {
            authRepository.getAuthRequests()
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `getPendingResults success with content should update state with some requests filtered`() {
        val dateTimeFormatter = DateTimeFormatter
            .ofPattern("M/d/yy hh:mm a")
            .withZone(TimeZone.getDefault().toZoneId())
        val nowZonedDateTime = ZonedDateTime.now()
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
                creationDate = ZonedDateTime.now().minusMinutes(10),
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
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = requestList,
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
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Error
        val expected = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Error,
        )
        val viewModel = createViewModel()
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = emptyList(),
        )
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PendingRequestsAction.CloseClick)
            assertEquals(PendingRequestsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on PendingRequestRowClick should emit NavigateToLoginApproval`() = runTest {
        val fingerprint = "fingerprint"
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = emptyList(),
        )
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
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = listOf(
                authRequest1,
                authRequest2,
            ),
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
        viewModel.actionChannel.trySend(PendingRequestsAction.DeclineAllRequestsConfirm)

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
            .withZone(TimeZone.getDefault().toZoneId())
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
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(emptyList())
        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = requestList,
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
            authRepository.getAuthRequests()
        }
    }

    private fun createViewModel(
        state: PendingRequestsState? = DEFAULT_STATE,
    ): PendingRequestsViewModel = PendingRequestsViewModel(
        authRepository = authRepository,
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )

    companion object {
        val DEFAULT_STATE: PendingRequestsState = PendingRequestsState(
            authRequests = emptyList(),
            viewState = PendingRequestsState.ViewState.Empty,
        )
    }
}
