package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
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

class LoginApprovalViewModelTest : BaseViewModelTest() {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val mockSpecialCircumstanceManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance } returns null
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mutableAuthRequestSharedFlow = bufferedMutableSharedFlow<AuthRequestUpdatesResult>()
    private val mockAuthRepository = mockk<AuthRepository> {
        coEvery {
            getAuthRequestByFingerprintFlow(FINGERPRINT)
        } returns mutableAuthRequestSharedFlow
        coEvery { getAuthRequestByIdFlow(REQUEST_ID) } returns mutableAuthRequestSharedFlow
        every { userStateFlow } returns mutableUserStateFlow
    }

    @Test
    fun `init should call getAuthRequestById when special circumstance is absent`() {
        createViewModel(state = null)
        coVerify {
            mockAuthRepository.getAuthRequestByFingerprintFlow(FINGERPRINT)
        }
    }

    @Test
    fun `init should call getAuthRequest when special circumstance is present`() {
        every {
            mockSpecialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.PasswordlessRequest(
            passwordlessRequestData = PasswordlessRequestData(
                loginRequestId = REQUEST_ID,
                userId = USER_ID,
            ),
            shouldFinishWhenComplete = false,
        )
        createViewModel(state = null)
        coVerify {
            mockAuthRepository.getAuthRequestByIdFlow(REQUEST_ID)
        }
    }

    @Test
    fun `getAuthRequest update should update state`() {
        val expected = DEFAULT_STATE.copy(
            fingerprint = FINGERPRINT,
            masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
            publicKey = AUTH_REQUEST.publicKey,
            requestId = AUTH_REQUEST.id,
            viewState = LoginApprovalState.ViewState.Content(
                deviceType = AUTH_REQUEST.platform,
                domainUrl = AUTH_REQUEST.originUrl,
                email = EMAIL,
                fingerprint = AUTH_REQUEST.fingerprint,
                ipAddress = AUTH_REQUEST.ipAddress,
                time = "9/13/24 12:00 AM",
            ),
        )
        val viewModel = createViewModel()
        mutableAuthRequestSharedFlow.tryEmit(AuthRequestUpdatesResult.Update(AUTH_REQUEST))
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `getAuthRequest approved should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            mutableAuthRequestSharedFlow.tryEmit(AuthRequestUpdatesResult.Approved)
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `getAuthRequest declined should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            mutableAuthRequestSharedFlow.tryEmit(AuthRequestUpdatesResult.Declined)
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `getAuthRequest expired should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            mutableAuthRequestSharedFlow.tryEmit(AuthRequestUpdatesResult.Expired)
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `getAuthRequest failure should update state`() {
        val expected = DEFAULT_STATE.copy(
            viewState = LoginApprovalState.ViewState.Error,
        )
        val viewModel = createViewModel()
        mutableAuthRequestSharedFlow.tryEmit(AuthRequestUpdatesResult.Error)
        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack when shouldFinishWhenComplete is false`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(LoginApprovalAction.CloseClick)
                assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `on CloseClick should emit ExitApp when shouldFinishWhenComplete is true`() = runTest {
        every {
            mockSpecialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.PasswordlessRequest(
            passwordlessRequestData = PasswordlessRequestData(
                loginRequestId = REQUEST_ID,
                userId = USER_ID,
            ),
            shouldFinishWhenComplete = true,
        )
        val viewModel = createViewModel(state = null)
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginApprovalAction.CloseClick)
            assertEquals(LoginApprovalEvent.ExitApp, awaitItem())
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

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginApprovalAction.ApproveRequestClick)
            assertEquals(
                LoginApprovalEvent.ShowToast(R.string.login_approved.asText()),
                awaitItem(),
            )
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }

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

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginApprovalAction.DeclineRequestClick)
            assertEquals(
                LoginApprovalEvent.ShowToast(R.string.log_in_denied.asText()),
                awaitItem(),
            )
            assertEquals(LoginApprovalEvent.NavigateBack, awaitItem())
        }

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
        specialCircumstanceManager: SpecialCircumstanceManager = mockSpecialCircumstanceManager,
        state: LoginApprovalState? = DEFAULT_STATE,
    ): LoginApprovalViewModel = LoginApprovalViewModel(
        clock = fixedClock,
        authRepository = authRepository,
        specialCircumstanceManager = specialCircumstanceManager,
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
    specialCircumstance = null,
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
            needsPasswordReset = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
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
