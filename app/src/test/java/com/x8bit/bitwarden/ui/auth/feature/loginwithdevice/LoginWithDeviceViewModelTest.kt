package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import com.x8bit.bitwarden.data.auth.manager.model.CreateAuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@Suppress("LargeClass")
class LoginWithDeviceViewModelTest : BaseViewModelTest() {

    private val mutableCreateAuthRequestWithUpdatesFlow =
        bufferedMutableSharedFlow<CreateAuthRequestResult>()
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val authRepository = mockk<AuthRepository> {
        coEvery {
            createAuthRequestWithUpdates(email = EMAIL, authRequestType = any())
        } returns mutableCreateAuthRequestWithUpdatesFlow
        coEvery { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel(state = null)
        assertEquals(
            DEFAULT_STATE.copy(viewState = LoginWithDeviceState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            authRepository.createAuthRequestWithUpdates(
                email = EMAIL,
                authRequestType = AuthRequestType.OTHER_DEVICE,
            )
        }
    }

    @Test
    fun `initial state should be correct when set`() {
        val newEmail = "newEmail@gmail.com"
        val state = DEFAULT_STATE.copy(emailAddress = newEmail)
        coEvery {
            authRepository.createAuthRequestWithUpdates(
                email = newEmail,
                authRequestType = AuthRequestType.OTHER_DEVICE,
            )
        } returns mutableCreateAuthRequestWithUpdatesFlow
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
        coVerify(exactly = 1) {
            authRepository.createAuthRequestWithUpdates(
                email = newEmail,
                authRequestType = AuthRequestType.OTHER_DEVICE,
            )
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginWithDeviceAction.CloseButtonClick)
            assertEquals(
                LoginWithDeviceEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialog should clear the dialog state`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = LoginWithDeviceState.DialogState.Error(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.generic_error_message.asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        viewModel.trySendAction(LoginWithDeviceAction.DismissDialog)
        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `ResendNotificationClick should create new auth request and update state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(LoginWithDeviceAction.ResendNotificationClick)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    isResendNotificationLoading = true,
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 2) {
            authRepository.createAuthRequestWithUpdates(
                email = EMAIL,
                authRequestType = AuthRequestType.OTHER_DEVICE,
            )
        }
    }

    @Test
    fun `ViewAllLogInOptionsClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginWithDeviceAction.ViewAllLogInOptionsClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginWithDeviceEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `on createAuthRequestWithUpdates Update received should show content`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
            CreateAuthRequestResult.Update(AUTH_REQUEST),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    fingerprintPhrase = FINGERPRINT,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on createAuthRequestWithUpdates Success and login success should update the state`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    requestId = DEFAULT_LOGIN_DATA.requestId,
                    accessCode = DEFAULT_LOGIN_DATA.accessCode,
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                    masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                    captchaToken = null,
                )
            } returns LoginResult.Success
            val viewModel = createViewModel()
            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
                mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                    CreateAuthRequestResult.Success(
                        authRequest = AUTH_REQUEST,
                        privateKey = AUTH_REQUEST_PRIVATE_KEY,
                        accessCode = AUTH_REQUEST_ACCESS_CODE,
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                            fingerprintPhrase = "",
                        ),
                        loginData = DEFAULT_LOGIN_DATA,
                        dialogState = LoginWithDeviceState.DialogState.Loading(
                            message = R.string.logging_in.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                            fingerprintPhrase = "",
                        ),
                        dialogState = null,
                        loginData = DEFAULT_LOGIN_DATA,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    LoginWithDeviceEvent.ShowToast(R.string.login_approved.asText()),
                    eventFlow.awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = EMAIL,
                    requestId = AUTH_REQUEST.id,
                    accessCode = AUTH_REQUEST_ACCESS_CODE,
                    asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                    requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                    masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                    captchaToken = null,
                )
            }
        }

    @Test
    fun `on createAuthRequestWithUpdates Success with SSO_ADMIN_APPROVAL should emit toast`() =
        runTest {
            coEvery {
                authRepository.completeTdeLogin(
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                )
            } returns LoginResult.Success

            val initialViewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                loginWithDeviceType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
            )
            val initialState = DEFAULT_STATE.copy(
                viewState = initialViewState,
                loginWithDeviceType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
            )
            val viewModel = createViewModel(initialState)

            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(initialState, stateFlow.awaitItem())
                mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                    CreateAuthRequestResult.Success(
                        authRequest = AUTH_REQUEST,
                        privateKey = AUTH_REQUEST_PRIVATE_KEY,
                        accessCode = AUTH_REQUEST_ACCESS_CODE,
                    ),
                )
                assertEquals(
                    initialState.copy(
                        viewState = initialViewState.copy(
                            fingerprintPhrase = "",
                        ),
                        dialogState = LoginWithDeviceState.DialogState.Loading(
                            message = R.string.logging_in.asText(),
                        ),
                        loginData = DEFAULT_LOGIN_DATA,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        viewState = initialViewState.copy(
                            fingerprintPhrase = "",
                        ),
                        dialogState = null,
                        loginData = DEFAULT_LOGIN_DATA,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    LoginWithDeviceEvent.ShowToast(R.string.login_approved.asText()),
                    eventFlow.awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.completeTdeLogin(
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on createAuthRequestWithUpdates Success and login two factor required should emit NavigateToTwoFactorLogin`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    requestId = DEFAULT_LOGIN_DATA.requestId,
                    accessCode = DEFAULT_LOGIN_DATA.accessCode,
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                    masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                    captchaToken = null,
                )
            } returns LoginResult.TwoFactorRequired
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                    CreateAuthRequestResult.Success(
                        authRequest = AUTH_REQUEST,
                        privateKey = AUTH_REQUEST_PRIVATE_KEY,
                        accessCode = AUTH_REQUEST_ACCESS_CODE,
                    ),
                )
                assertEquals(
                    LoginWithDeviceEvent.NavigateToTwoFactorLogin(emailAddress = EMAIL),
                    awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = EMAIL,
                    requestId = AUTH_REQUEST.id,
                    accessCode = AUTH_REQUEST_ACCESS_CODE,
                    asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                    requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                    masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                    captchaToken = null,
                )
            }
        }

    @Test
    fun `on createAuthRequestWithUpdates Success and login error should should update the state`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    requestId = DEFAULT_LOGIN_DATA.requestId,
                    accessCode = DEFAULT_LOGIN_DATA.accessCode,
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                    masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                    captchaToken = null,
                )
            } returns LoginResult.Error(null)
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.stateFlow.test {
                    assertEquals(DEFAULT_STATE, awaitItem())
                    mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                        CreateAuthRequestResult.Success(
                            authRequest = AUTH_REQUEST,
                            privateKey = AUTH_REQUEST_PRIVATE_KEY,
                            accessCode = AUTH_REQUEST_ACCESS_CODE,
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                            dialogState = LoginWithDeviceState.DialogState.Loading(
                                message = R.string.logging_in.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            dialogState = LoginWithDeviceState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.generic_error_message.asText(),
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                        ),
                        awaitItem(),
                    )
                }
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = EMAIL,
                    requestId = AUTH_REQUEST.id,
                    accessCode = AUTH_REQUEST_ACCESS_CODE,
                    asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                    requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                    masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                    captchaToken = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on createAuthRequestWithUpdates Success and login UnofficialServerError should should update the state`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    requestId = DEFAULT_LOGIN_DATA.requestId,
                    accessCode = DEFAULT_LOGIN_DATA.accessCode,
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                    masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                    captchaToken = null,
                )
            } returns LoginResult.UnofficialServerError
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.stateFlow.test {
                    assertEquals(DEFAULT_STATE, awaitItem())
                    mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                        CreateAuthRequestResult.Success(
                            authRequest = AUTH_REQUEST,
                            privateKey = AUTH_REQUEST_PRIVATE_KEY,
                            accessCode = AUTH_REQUEST_ACCESS_CODE,
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                            dialogState = LoginWithDeviceState.DialogState.Loading(
                                message = R.string.logging_in.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            dialogState = LoginWithDeviceState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                        ),
                        awaitItem(),
                    )
                }
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = EMAIL,
                    requestId = AUTH_REQUEST.id,
                    accessCode = AUTH_REQUEST_ACCESS_CODE,
                    asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                    requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                    masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                    captchaToken = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on createAuthRequestWithUpdates Success and login CertificateError should should update the state`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    requestId = DEFAULT_LOGIN_DATA.requestId,
                    accessCode = DEFAULT_LOGIN_DATA.accessCode,
                    asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                    requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                    masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                    captchaToken = null,
                )
            } returns LoginResult.CertificateError
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.stateFlow.test {
                    assertEquals(DEFAULT_STATE, awaitItem())
                    mutableCreateAuthRequestWithUpdatesFlow.tryEmit(
                        CreateAuthRequestResult.Success(
                            authRequest = AUTH_REQUEST,
                            privateKey = AUTH_REQUEST_PRIVATE_KEY,
                            accessCode = AUTH_REQUEST_ACCESS_CODE,
                        ),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                            dialogState = LoginWithDeviceState.DialogState.Loading(
                                message = R.string.logging_in.asText(),
                            ),
                        ),
                        awaitItem(),
                    )
                    assertEquals(
                        DEFAULT_STATE.copy(
                            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                                fingerprintPhrase = "",
                            ),
                            dialogState = LoginWithDeviceState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.we_couldnt_verify_the_servers_certificate.asText(),
                            ),
                            loginData = DEFAULT_LOGIN_DATA,
                        ),
                        awaitItem(),
                    )
                }
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = EMAIL,
                    requestId = AUTH_REQUEST.id,
                    accessCode = AUTH_REQUEST_ACCESS_CODE,
                    asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                    requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                    masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                    captchaToken = null,
                )
            }
        }

    @Test
    fun `on captchaTokenResultFlow missing token should should display error dialog`() = runTest {
        val viewModel = createViewModel()
        mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.MissingToken)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = LoginWithDeviceState.DialogState.Error(
                    title = R.string.log_in_denied.asText(),
                    message = R.string.captcha_failed.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on captchaTokenResultFlow success should update the token`() = runTest {
        val captchaToken = "captchaToken"
        val initialState = DEFAULT_STATE.copy(loginData = DEFAULT_LOGIN_DATA)
        coEvery {
            authRepository.login(
                email = EMAIL,
                requestId = DEFAULT_LOGIN_DATA.requestId,
                accessCode = DEFAULT_LOGIN_DATA.accessCode,
                asymmetricalKey = DEFAULT_LOGIN_DATA.asymmetricalKey,
                requestPrivateKey = DEFAULT_LOGIN_DATA.privateKey,
                masterPasswordHash = DEFAULT_LOGIN_DATA.masterPasswordHash,
                captchaToken = captchaToken,
            )
        } just awaits
        val viewModel = createViewModel(initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success(captchaToken))
            assertEquals(
                initialState.copy(
                    loginData = DEFAULT_LOGIN_DATA.copy(captchaToken = captchaToken),
                    dialogState = LoginWithDeviceState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
        }

        coVerify(exactly = 1) {
            authRepository.login(
                email = EMAIL,
                requestId = AUTH_REQUEST.id,
                accessCode = AUTH_REQUEST_ACCESS_CODE,
                asymmetricalKey = requireNotNull(AUTH_REQUEST.key),
                requestPrivateKey = AUTH_REQUEST_PRIVATE_KEY,
                masterPasswordHash = AUTH_REQUEST.masterPasswordHash,
                captchaToken = captchaToken,
            )
        }
    }

    @Test
    fun `on createAuthRequestWithUpdates Error received should show content with error dialog`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        mutableCreateAuthRequestWithUpdatesFlow.tryEmit(CreateAuthRequestResult.Error)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    fingerprintPhrase = "",
                    isResendNotificationLoading = false,
                ),
                dialogState = LoginWithDeviceState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on createAuthRequestWithUpdates with OTHER_DEVICE, Declined received should show unchanged content`() {
        val initialState = DEFAULT_STATE.copy(
            loginWithDeviceType = LoginWithDeviceType.OTHER_DEVICE,
            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                loginWithDeviceType = LoginWithDeviceType.OTHER_DEVICE,
            ),
        )
        val viewModel = createViewModel(state = initialState)
        assertEquals(initialState, viewModel.stateFlow.value)
        mutableCreateAuthRequestWithUpdatesFlow.tryEmit(CreateAuthRequestResult.Declined)
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on createAuthRequestWithUpdates with SSO_ADMIN_APPROVAL, Declined received should show unchanged content`() {
        val initialState = DEFAULT_STATE.copy(
            loginWithDeviceType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
            viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                loginWithDeviceType = LoginWithDeviceType.SSO_ADMIN_APPROVAL,
            ),
        )
        val viewModel = createViewModel(initialState)
        assertEquals(initialState, viewModel.stateFlow.value)
        mutableCreateAuthRequestWithUpdatesFlow.tryEmit(CreateAuthRequestResult.Declined)
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `on createAuthRequestWithUpdates Expired received should show content with error dialog`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        mutableCreateAuthRequestWithUpdatesFlow.tryEmit(CreateAuthRequestResult.Expired)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    fingerprintPhrase = "",
                    isResendNotificationLoading = false,
                ),
                dialogState = LoginWithDeviceState.DialogState.Error(
                    title = null,
                    message = R.string.login_request_has_already_expired.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: LoginWithDeviceState? = DEFAULT_STATE,
    ): LoginWithDeviceViewModel =
        LoginWithDeviceViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
                set("email_address", state?.emailAddress ?: EMAIL)
                set("login_type", state?.loginWithDeviceType ?: LoginWithDeviceType.OTHER_DEVICE)
            },
        )
}

private const val EMAIL = "test@gmail.com"
private const val FINGERPRINT = "fingerprint"

private val DEFAULT_CONTENT_VIEW_STATE = LoginWithDeviceState.ViewState.Content(
    fingerprintPhrase = FINGERPRINT,
    isResendNotificationLoading = false,
    loginWithDeviceType = LoginWithDeviceType.OTHER_DEVICE,
)

private val DEFAULT_STATE = LoginWithDeviceState(
    emailAddress = EMAIL,
    viewState = DEFAULT_CONTENT_VIEW_STATE,
    dialogState = null,
    loginData = null,
    loginWithDeviceType = LoginWithDeviceType.OTHER_DEVICE,
)

private val AUTH_REQUEST = AuthRequest(
    id = "1",
    publicKey = "2",
    platform = "Android",
    ipAddress = "192.168.0.1",
    key = "public",
    masterPasswordHash = "verySecureHash",
    creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
    responseDate = null,
    requestApproved = true,
    originUrl = "www.bitwarden.com",
    fingerprint = FINGERPRINT,
)

private const val AUTH_REQUEST_ACCESS_CODE = "accessCode"
private const val AUTH_REQUEST_PRIVATE_KEY = "private_key"

private val DEFAULT_LOGIN_DATA = LoginWithDeviceState.LoginData(
    accessCode = "accessCode",
    requestId = "1",
    masterPasswordHash = "verySecureHash",
    asymmetricalKey = "public",
    privateKey = "private_key",
    captchaToken = null,
)
