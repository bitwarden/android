package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestsResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
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

    @Test
    fun `getPendingResults success with content should update state`() {
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = listOf(
                AuthRequest(
                    id = "1",
                    publicKey = "publicKey-1",
                    platform = "Android",
                    ipAddress = "192.168.0.1",
                    key = "publicKey",
                    masterPasswordHash = "verySecureHash",
                    creationDate = ZonedDateTime.parse("2023-08-24T17:11Z"),
                    responseDate = null,
                    requestApproved = true,
                    originUrl = "www.bitwarden.com",
                    fingerprint = "pantry-overdue-survive-sleep-jab",
                ),
                AuthRequest(
                    id = "2",
                    publicKey = "publicKey-2",
                    platform = "iOS",
                    ipAddress = "192.168.0.2",
                    key = "publicKey",
                    masterPasswordHash = "verySecureHash",
                    creationDate = ZonedDateTime.parse("2023-08-21T15:43Z"),
                    responseDate = null,
                    requestApproved = false,
                    originUrl = "www.bitwarden.com",
                    fingerprint = "erupt-anew-matchbook-disk-student",
                ),
            ),
        )
        val expected = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Content(
                requests = listOf(
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "pantry-overdue-survive-sleep-jab",
                        platform = "Android",
                        timestamp = "8/24/23 05:11 PM",
                    ),
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "erupt-anew-matchbook-disk-student",
                        platform = "iOS",
                        timestamp = "8/21/23 03:43 PM",
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

    @Test
    fun `on DeclineAllRequestsClick should send ShowToast event`() = runTest {
        coEvery {
            authRepository.getAuthRequests()
        } returns AuthRequestsResult.Success(
            authRequests = emptyList(),
        )
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(PendingRequestsAction.DeclineAllRequestsClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                PendingRequestsEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
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
            viewState = PendingRequestsState.ViewState.Empty,
        )
    }
}
