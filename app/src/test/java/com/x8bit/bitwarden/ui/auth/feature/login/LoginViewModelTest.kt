package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginViewModelTest : BaseViewModelTest() {

    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        coEvery {
            getIsKnownDevice(EMAIL)
        } returns KnownDeviceResult.Success(false)
        every { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
        every { userStateFlow } returns mutableUserStateFlow
        every { logout(any()) } just runs
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true) {
        every { lockVault(any()) } just runs
    }
    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    @BeforeEach
    fun setUp() {
        mockkStatic(::generateUriForCaptcha)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::generateUriForCaptcha)
    }

    @Test
    fun `initial state should be correct for non-custom Environments`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should be correct for custom Environments with empty base URLs`() = runTest {
        fakeEnvironmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(
                base = "",
            ),
        )
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    environmentLabel = "",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial state should be correct for custom Environments with non-empty base URLs`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.SelfHosted(
                environmentUrlData = EnvironmentUrlDataJson(
                    base = "https://abc.com/path-1/path-2",
                ),
            )
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(
                        environmentLabel = "abc.com",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `initial state should set the account summaries based on the UserState`() {
        val userState = UserState(
            activeUserId = "activeUserId",
            accounts = listOf(
                UserState.Account(
                    userId = "activeUserId",
                    name = "name",
                    email = "email",
                    avatarColorHex = "avatarColorHex",
                    environment = Environment.Us,
                    isPremium = true,
                    isLoggedIn = true,
                    isVaultUnlocked = true,
                    needsPasswordReset = false,
                    isBiometricsEnabled = false,
                    organizations = emptyList(),
                    needsMasterPassword = false,
                    trustedDevice = null,
                    hasMasterPassword = true,
                    isUsingKeyConnector = false,
                    onboardingStatus = OnboardingStatus.COMPLETE,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
            ),
        )
        mutableUserStateFlow.value = userState
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                accountSummaries = userState.toAccountSummaries(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            passwordInput = "input",
            isLoginButtonEnabled = true,
        )
        val viewModel = createViewModel(expectedState)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should set shouldShowLoginWithDevice when isKnownDevice returns true`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            shouldShowLoginWithDevice = true,
        )
        coEvery {
            authRepository.getIsKnownDevice(EMAIL)
        } returns KnownDeviceResult.Success(true)
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should have default state when isKnownDevice returns error`() = runTest {
        coEvery {
            authRepository.getIsKnownDevice(EMAIL)
        } returns KnownDeviceResult.Error
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `on AddAccountClick should send NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.AddAccountClick)
            assertEquals(LoginEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LoginAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId) }
    }

    @Test
    fun `LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LoginAction.LogoutAccountClick(accountSummary))

        verify { authRepository.logout(userId = accountUserId) }
    }

    @Test
    fun `SwitchAccountClick should call switchAccount for the given account`() {
        val matchingAccountUserId = "matchingAccountUserId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns matchingAccountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LoginAction.SwitchAccountClick(accountSummary))

        verify { authRepository.switchAccount(userId = matchingAccountUserId) }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.CloseButtonClick)
            assertEquals(
                LoginEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `LoginButtonClick login returns error should update errorDialogState`() = runTest {
        coEvery {
            authRepository.login(
                email = EMAIL,
                password = "",
                captchaToken = null,
            )
        } returns LoginResult.Error(errorMessage = "mock_error")
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(LoginAction.LoginButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = LoginState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = LoginState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = "mock_error".asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify {
            authRepository.login(email = EMAIL, password = "", captchaToken = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `LoginButtonClick login returns UnofficialServerError should update errorDialogState`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    password = "",
                    captchaToken = null,
                )
            } returns LoginResult.UnofficialServerError
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(LoginAction.LoginButtonClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = LoginState.DialogState.Loading(
                            message = R.string.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify {
                authRepository.login(email = EMAIL, password = "", captchaToken = null)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LoginButtonClick login returns CertificateError should update errorDialogState`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    password = "",
                    captchaToken = null,
                )
            } returns LoginResult.CertificateError
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(LoginAction.LoginButtonClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = LoginState.DialogState.Loading(
                            message = R.string.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify {
                authRepository.login(email = EMAIL, password = "", captchaToken = null)
            }
        }

    @Test
    fun `LoginButtonClick login returns success should update loadingDialogState`() = runTest {
        coEvery {
            authRepository.login(
                email = EMAIL,
                password = "",
                captchaToken = null,
            )
        } returns LoginResult.Success
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(LoginAction.LoginButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = LoginState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(dialogState = null),
                awaitItem(),
            )
        }
        coVerify {
            authRepository.login(email = EMAIL, password = "", captchaToken = null)
        }
    }

    @Test
    fun `LoginButtonClick login returns CaptchaRequired should emit NavigateToCaptcha`() =
        runTest {
            val mockkUri = mockk<Uri>()
            every {
                generateUriForCaptcha(captchaId = "mock_captcha_id")
            } returns mockkUri
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    password = "",
                    captchaToken = null,
                )
            } returns LoginResult.CaptchaRequired(captchaId = "mock_captcha_id")
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(LoginAction.LoginButtonClick)
                assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
                assertEquals(LoginEvent.NavigateToCaptcha(uri = mockkUri), awaitItem())
            }
            coVerify {
                authRepository.login(email = EMAIL, password = "", captchaToken = null)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LoginButtonClick login returns TwoFactorRequired should base64 URL encode password and emit NavigateToTwoFactorLogin`() =
        runTest {
            val password = "password"
            coEvery {
                authRepository.login(
                    email = EMAIL,
                    password = password,
                    captchaToken = null,
                )
            } returns LoginResult.TwoFactorRequired

            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    passwordInput = password,
                ),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(LoginAction.LoginButtonClick)
                assertEquals(
                    DEFAULT_STATE.copy(passwordInput = password),
                    viewModel.stateFlow.value,
                )
                assertEquals(
                    LoginEvent.NavigateToTwoFactorLogin(
                        emailAddress = EMAIL,
                        password = password,
                    ),
                    awaitItem(),
                )
            }
            coVerify {
                authRepository.login(
                    email = EMAIL,
                    password = password,
                    captchaToken = null,
                )
            }
        }

    @Test
    fun `MasterPasswordHintClick should emit NavigateToMasterPasswordHint`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.MasterPasswordHintClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.NavigateToMasterPasswordHint(EMAIL),
                awaitItem(),
            )
        }
    }

    @Test
    fun `LoginWithDeviceButtonClick should emit NavigateToLoginWithDevice`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.LoginWithDeviceButtonClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.NavigateToLoginWithDevice(EMAIL),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SingleSignOnClick should emit NavigateToEnterpriseSignOn`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.SingleSignOnClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.NavigateToEnterpriseSignOn("test@gmail.com"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `NotYouButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LoginAction.NotYouButtonClick)
            assertEquals(
                LoginEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `PasswordInputChanged should update input and enable button if password is not blank`() =
        runTest {
            val input = "input"
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(LoginAction.PasswordInputChanged(input))
                assertEquals(
                    DEFAULT_STATE.copy(
                        passwordInput = input,
                        isLoginButtonEnabled = true,
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `PasswordInputChanged should update input and disable button if password is blank`() =
        runTest {
            val input = "input"
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                // set isLoginButtonEnabled to true
                viewModel.trySendAction(LoginAction.PasswordInputChanged(input))
                assertEquals(
                    DEFAULT_STATE.copy(
                        passwordInput = input,
                        isLoginButtonEnabled = true,
                    ),
                    viewModel.stateFlow.value,
                )

                // set isLoginButtonEnabled to false
                viewModel.trySendAction(LoginAction.PasswordInputChanged(""))
                assertEquals(
                    DEFAULT_STATE.copy(
                        passwordInput = "",
                        isLoginButtonEnabled = false,
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `captchaTokenFlow success update should trigger a login`() = runTest {
        coEvery {
            authRepository.login(
                email = EMAIL,
                password = "",
                captchaToken = "token",
            )
        } returns LoginResult.Success
        createViewModel()
        mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success("token"))
        coVerify {
            authRepository.login(email = EMAIL, password = "", captchaToken = "token")
        }
    }

    private fun createViewModel(state: LoginState? = null): LoginViewModel =
        LoginViewModel(
            authRepository = authRepository,
            environmentRepository = fakeEnvironmentRepository,
            vaultRepository = vaultRepository,
            savedStateHandle = SavedStateHandle().also {
                it["email_address"] = EMAIL
                it["state"] = state
            },
        )

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = LoginState(
            emailAddress = EMAIL,
            passwordInput = "",
            isLoginButtonEnabled = false,
            environmentLabel = Environment.Us.label,
            dialogState = null,
            captchaToken = null,
            accountSummaries = emptyList(),
            shouldShowLoginWithDevice = false,
        )
    }
}
