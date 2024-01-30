package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.TimeZone

class LoginApprovalViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mockAuthRepository = mockk<AuthRepository> {
        coEvery {
            getAuthRequest(FINGERPRINT)
        } returns AuthRequestResult.Success(AUTH_REQUEST)
        every { userStateFlow } returns mutableUserStateFlow
    }

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
    fun `initial state should be correct and trigger a getAuthRequest call`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        coVerify {
            mockAuthRepository.getAuthRequest(FINGERPRINT)
        }
        verify {
            mockAuthRepository.userStateFlow
        }
    }

    @Test
    fun `getAuthRequest failure should update state`() {
        val authRepository = mockk<AuthRepository> {
            coEvery {
                getAuthRequest(FINGERPRINT)
            } returns AuthRequestResult.Error
            every { userStateFlow } returns mutableUserStateFlow
        }
        val expected = DEFAULT_STATE.copy(
            viewState = LoginApprovalState.ViewState.Error,
        )
        val viewModel = createViewModel(authRepository = authRepository)
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginApprovalAction.CloseClick)
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on ApproveRequestClick should approve auth request`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            mockAuthRepository.updateAuthRequest(
                requestId = REQUEST_ID,
                masterPasswordHash = PASSWORD_HASH,
                publicKey = PUBLIC_KEY,
                isApproved = true,
            )
        } returns AuthRequestResult.Success(AUTH_REQUEST)

        viewModel.trySendAction(LoginApprovalAction.ApproveRequestClick)

        coVerify {
            mockAuthRepository.updateAuthRequest(
                requestId = REQUEST_ID,
                masterPasswordHash = PASSWORD_HASH,
                publicKey = PUBLIC_KEY,
                isApproved = true,
            )
        }
    }

    @Test
    fun `on DeclineRequestClick should deny auth request`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            mockAuthRepository.updateAuthRequest(
                requestId = REQUEST_ID,
                masterPasswordHash = PASSWORD_HASH,
                publicKey = PUBLIC_KEY,
                isApproved = false,
            )
        } returns AuthRequestResult.Success(AUTH_REQUEST)

        viewModel.trySendAction(LoginApprovalAction.DeclineRequestClick)

        coVerify {
            mockAuthRepository.updateAuthRequest(
                requestId = REQUEST_ID,
                masterPasswordHash = PASSWORD_HASH,
                publicKey = PUBLIC_KEY,
                isApproved = false,
            )
        }
    }

    @Test
    fun `on ErrorDialogDismiss should update state`() = runTest {
        val viewModel = createViewModel()
        coEvery {
            mockAuthRepository.updateAuthRequest(
                requestId = REQUEST_ID,
                masterPasswordHash = PASSWORD_HASH,
                publicKey = PUBLIC_KEY,
                isApproved = false,
            )
        } returns AuthRequestResult.Error
        viewModel.trySendAction(LoginApprovalAction.DeclineRequestClick)

        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE.copy(shouldShowErrorDialog = true))
        viewModel.trySendAction(LoginApprovalAction.ErrorDialogDismiss)

        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE.copy(shouldShowErrorDialog = false))
    }

    private fun createViewModel(
        authRepository: AuthRepository = mockAuthRepository,
        state: LoginApprovalState? = DEFAULT_STATE,
    ): LoginApprovalViewModel = LoginApprovalViewModel(
        authRepository = authRepository,
        savedStateHandle = SavedStateHandle()
            .also { it["fingerprint"] = FINGERPRINT }
            .apply { set("state", state) },
    )
}

private const val EMAIL = "test@bitwarden.com"
private const val FINGERPRINT = "fingerprint"
private const val PASSWORD_HASH = "verySecureHash"
private const val PUBLIC_KEY = "publicKey"
private const val REQUEST_ID = "requestId"
private val DEFAULT_STATE: LoginApprovalState = LoginApprovalState(
    fingerprint = FINGERPRINT,
    masterPasswordHash = PASSWORD_HASH,
    publicKey = PUBLIC_KEY,
    requestId = REQUEST_ID,
    shouldShowErrorDialog = false,
    viewState = LoginApprovalState.ViewState.Content(
        deviceType = "Android",
        domainUrl = "www.bitwarden.com",
        email = EMAIL,
        fingerprint = FINGERPRINT,
        ipAddress = "1.0.0.1",
        time = "9/13/24 12:00 AM",
    ),
)
private const val USER_ID = "userID"
private val DEFAULT_USER_STATE = UserState(
    activeUserId = USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = USER_ID,
            name = "Active User",
            email = EMAIL,
            environment = Environment.Us,
            avatarColorHex = "#aa00aa",
            isBiometricsEnabled = false,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            isVaultPendingUnlock = false,
            organizations = emptyList(),
        ),
    ),
)
private val AUTH_REQUEST = AuthRequest(
    id = REQUEST_ID,
    publicKey = PUBLIC_KEY,
    platform = "Android",
    ipAddress = "1.0.0.1",
    key = "public",
    masterPasswordHash = PASSWORD_HASH,
    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
    responseDate = null,
    requestApproved = true,
    originUrl = "www.bitwarden.com",
    fingerprint = FINGERPRINT,
)
