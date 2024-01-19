package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
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

    private val savedStateHandle = SavedStateHandle().also {
        it["email_address"] = "test@gmail.com"
    }
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        coEvery {
            getIsKnownDevice("test@gmail.com")
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
        mockkStatic(LOGIN_RESULT_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LOGIN_RESULT_PATH)
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
                    organizations = emptyList(),
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
        savedStateHandle["state"] = expectedState
        val viewModel = createViewModel()
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
            authRepository.getIsKnownDevice("test@gmail.com")
        } returns KnownDeviceResult.Success(true)
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should have default state when isKnownDevice returns error`() = runTest {
        coEvery {
            authRepository.getIsKnownDevice("test@gmail.com")
        } returns KnownDeviceResult.Error
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
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
            viewModel.actionChannel.trySend(LoginAction.CloseButtonClick)
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
                email = "test@gmail.com",
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
                    loadingDialogState = LoadingDialogState.Shown(
                        text = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = R.string.an_error_has_occurred.asText(),
                        message = "mock_error".asText(),
                    ),
                    loadingDialogState = LoadingDialogState.Hidden,
                ),
                awaitItem(),
            )
        }
        coVerify {
            authRepository.login(email = "test@gmail.com", password = "", captchaToken = null)
        }
    }

    @Test
    fun `LoginButtonClick login returns success should update loadingDialogState`() = runTest {
        coEvery {
            authRepository.login(
                email = "test@gmail.com",
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
                    loadingDialogState = LoadingDialogState.Shown(
                        text = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(loadingDialogState = LoadingDialogState.Hidden),
                awaitItem(),
            )
        }
        coVerify {
            authRepository.login(email = "test@gmail.com", password = "", captchaToken = null)
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
                    email = "test@gmail.com",
                    password = "",
                    captchaToken = null,
                )
            } returns LoginResult.CaptchaRequired(captchaId = "mock_captcha_id")
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(LoginAction.LoginButtonClick)
                assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
                assertEquals(LoginEvent.NavigateToCaptcha(uri = mockkUri), awaitItem())
            }
            coVerify {
                authRepository.login(email = "test@gmail.com", password = "", captchaToken = null)
            }
        }

    @Test
    fun `MasterPasswordHintClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.MasterPasswordHintClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Test
    fun `LoginWithDeviceButtonClick should emit NavigateToLoginWithDevice`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.LoginWithDeviceButtonClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.NavigateToLoginWithDevice,
                awaitItem(),
            )
        }
    }

    @Test
    fun `SingleSignOnClick should emit NavigateToEnterpriseSignOn`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.SingleSignOnClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginEvent.NavigateToEnterpriseSignOn,
                awaitItem(),
            )
        }
    }

    @Test
    fun `NotYouButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.NotYouButtonClick)
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
                viewModel.actionChannel.trySend(LoginAction.PasswordInputChanged(input))
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
                viewModel.actionChannel.trySend(LoginAction.PasswordInputChanged(input))
                assertEquals(
                    DEFAULT_STATE.copy(
                        passwordInput = input,
                        isLoginButtonEnabled = true,
                    ),
                    viewModel.stateFlow.value,
                )

                // set isLoginButtonEnabled to false
                viewModel.actionChannel.trySend(LoginAction.PasswordInputChanged(""))
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
                email = "test@gmail.com",
                password = "",
                captchaToken = "token",
            )
        } returns LoginResult.Success
        createViewModel()
        mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success("token"))
        coVerify {
            authRepository.login(email = "test@gmail.com", password = "", captchaToken = "token")
        }
    }

    private fun createViewModel(): LoginViewModel =
        LoginViewModel(
            authRepository = authRepository,
            environmentRepository = fakeEnvironmentRepository,
            vaultRepository = vaultRepository,
            savedStateHandle = savedStateHandle,
        )

    companion object {
        private val DEFAULT_STATE = LoginState(
            emailAddress = "test@gmail.com",
            passwordInput = "",
            isLoginButtonEnabled = false,
            environmentLabel = Environment.Us.label,
            loadingDialogState = LoadingDialogState.Hidden,
            errorDialogState = BasicDialogState.Hidden,
            captchaToken = null,
            accountSummaries = emptyList(),
            shouldShowLoginWithDevice = false,
        )

        private const val LOGIN_RESULT_PATH =
            "com.x8bit.bitwarden.data.auth.repository.util.CaptchaUtilsKt"
    }
}
