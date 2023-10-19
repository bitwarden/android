package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateAccountViewModelTest : BaseViewModelTest() {

    private val mockAuthRepository = mockk<AuthRepository> {
        every { captchaTokenResultFlow } returns flowOf()
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(LOGIN_RESULT_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LOGIN_RESULT_PATH)
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should pull from saved state handle when present`() {
        val savedState = CreateAccountState(
            emailInput = "email",
            passwordInput = "password",
            confirmPasswordInput = "confirmPassword",
            passwordHintInput = "hint",
            isCheckDataBreachesToggled = false,
            isAcceptPoliciesToggled = false,
            errorDialogState = BasicDialogState.Hidden,
            loadingDialogState = LoadingDialogState.Hidden,
        )
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = CreateAccountViewModel(
            savedStateHandle = handle,
            authRepository = mockAuthRepository,
        )
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SubmitClick with password below 12 chars should show password length dialog`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        val input = "abcdefghikl"
        viewModel.trySendAction(PasswordInputChange("abcdefghikl"))
        val expectedState = DEFAULT_STATE.copy(
            passwordInput = input,
            errorDialogState = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.master_password_length_val_message_x.asText(12),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with long enough password should show and hide loading dialog`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = "",
                    masterPassword = "longenoughpassword",
                    masterPasswordHint = null,
                    captchaToken = null,
                )
            } returns RegisterResult.Success(captchaToken = "mock_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = repo,
        )

        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            assertEquals(
                DEFAULT_STATE,
                stateFlow.awaitItem(),
            )
            viewModel.trySendAction(PasswordInputChange("longenoughpassword"))
            assertEquals(
                DEFAULT_STATE.copy(passwordInput = "longenoughpassword"),
                stateFlow.awaitItem(),
            )
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordInput = "longenoughpassword",
                    loadingDialogState = LoadingDialogState.Shown(
                        text = R.string.creating_account.asText(),
                    ),
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(
                CreateAccountEvent.NavigateToLogin(
                    email = "",
                    captchaToken = "mock_token",
                ),
                eventFlow.awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordInput = "longenoughpassword",
                    loadingDialogState = LoadingDialogState.Hidden,
                ),
                stateFlow.awaitItem(),
            )
        }
    }

    @Test
    fun `SubmitClick register returns error should update errorDialogState`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = "",
                    masterPassword = "longenoughpassword",
                    masterPasswordHint = null,
                    captchaToken = null,
                )
            } returns RegisterResult.Error(errorMessage = "mock_error")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = repo,
        )
        viewModel.trySendAction(PasswordInputChange("longenoughpassword"))
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(passwordInput = "longenoughpassword"),
                awaitItem(),
            )
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordInput = "longenoughpassword",
                    loadingDialogState = LoadingDialogState.Shown(
                        text = R.string.creating_account.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordInput = "longenoughpassword",
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Shown(
                        title = R.string.an_error_has_occurred.asText(),
                        message = "mock_error".asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SubmitClick register returns CaptchaRequired should emit NavigateToCaptcha`() = runTest {
        val mockkUri = mockk<Uri>()
        every {
            generateUriForCaptcha(captchaId = "mock_captcha_id")
        } returns mockkUri
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = "",
                    masterPassword = "longenoughpassword",
                    masterPasswordHint = null,
                    captchaToken = null,
                )
            } returns RegisterResult.CaptchaRequired(captchaId = "mock_captcha_id")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = repo,
        )
        viewModel.trySendAction(PasswordInputChange("longenoughpassword"))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                CreateAccountEvent.NavigateToCaptcha(uri = mockkUri),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SubmitClick register returns Success should emit NavigateToLogin`() = runTest {
        val mockkUri = mockk<Uri>()
        every {
            generateUriForCaptcha(captchaId = "mock_captcha_id")
        } returns mockkUri
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = "",
                    masterPassword = "longenoughpassword",
                    masterPasswordHint = null,
                    captchaToken = null,
                )
            } returns RegisterResult.Success(captchaToken = "mock_captcha_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = repo,
        )
        viewModel.trySendAction(PasswordInputChange("longenoughpassword"))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                CreateAccountEvent.NavigateToLogin(
                    email = "",
                    captchaToken = "mock_captcha_token",

                    ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CloseClick)
            assertEquals(CreateAccountEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `PrivacyPolicyClick should emit NavigatePrivacyPolicy`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.PrivacyPolicyClick)
            assertEquals(CreateAccountEvent.NavigateToPrivacyPolicy, awaitItem())
        }
    }

    @Test
    fun `TermsClick should emit NavigateToTerms`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.TermsClick)
            assertEquals(CreateAccountEvent.NavigateToTerms, awaitItem())
        }
    }

    @Test
    fun `ConfirmPasswordInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.actionChannel.trySend(ConfirmPasswordInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(confirmPasswordInput = "input"), awaitItem())
        }
    }

    @Test
    fun `EmailInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.actionChannel.trySend(EmailInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(emailInput = "input"), awaitItem())
        }
    }

    @Test
    fun `PasswordHintChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.actionChannel.trySend(PasswordHintChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordHintInput = "input"), awaitItem())
        }
    }

    @Test
    fun `PasswordInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.actionChannel.trySend(PasswordInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordInput = "input"), awaitItem())
        }
    }

    @Test
    fun `CheckDataBreachesToggle should change isCheckDataBreachesToggled`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(CreateAccountAction.CheckDataBreachesToggle(true))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(isCheckDataBreachesToggled = true), awaitItem())
        }
    }

    @Test
    fun `AcceptPoliciesToggle should change isAcceptPoliciesToggled`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(CreateAccountAction.AcceptPoliciesToggle(true))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(isAcceptPoliciesToggled = true), awaitItem())
        }
    }

    companion object {
        private val DEFAULT_STATE = CreateAccountState(
            passwordInput = "",
            emailInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isCheckDataBreachesToggled = false,
            isAcceptPoliciesToggled = false,
            errorDialogState = BasicDialogState.Hidden,
            loadingDialogState = LoadingDialogState.Hidden,
        )
        private const val LOGIN_RESULT_PATH =
            "com.x8bit.bitwarden.data.auth.repository.util.CaptchaUtilsKt"
    }
}
