package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_0
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_1
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_2
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_3
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_4
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.Internal.ReceivePasswordStrengthResult
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import io.mockk.coEvery
import io.mockk.coVerify
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

    /**
     * Saved state handle that has valid inputs. Useful for tests that want to test things
     * after the user has entered all valid inputs.
     */
    private val validInputHandle = SavedStateHandle(mapOf("state" to VALID_INPUT_STATE))

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
            dialog = null,
            passwordStrengthState = PasswordStrengthState.NONE,
        )
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = CreateAccountViewModel(
            savedStateHandle = handle,
            authRepository = mockAuthRepository,
        )
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SubmitClick with blank email should show email required`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        val input = "a"
        viewModel.trySendAction(EmailInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = input,
            dialog = CreateAccountDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.invalid_email.asText(),
                ),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with invalid email should show invalid email`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        val input = " "
        viewModel.trySendAction(EmailInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = input,
            dialog = CreateAccountDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required
                        .asText(R.string.email_address.asText()),
                ),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with password below 12 chars should show password length dialog`() = runTest {
        val input = "abcdefghikl"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", input)
        } returns Throwable().asFailure()
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(EmailInputChange(EMAIL))
        viewModel.trySendAction(PasswordInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = EMAIL,
            passwordInput = input,
            dialog = CreateAccountDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_length_val_message_x.asText(12),
                ),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with passwords not matching should show password match dialog`() = runTest {
        val input = "testtesttesttest"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", input)
        } returns Throwable().asFailure()
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(EmailInputChange("test@test.com"))
        viewModel.trySendAction(PasswordInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = "test@test.com",
            passwordInput = input,
            dialog = CreateAccountDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_confirmation_val_message.asText(),
                ),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick without policies accepted should show accept policies error`() = runTest {
        val password = "testtesttesttest"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", password)
        } returns Throwable().asFailure()
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(EmailInputChange("test@test.com"))
        viewModel.trySendAction(PasswordInputChange(password))
        viewModel.trySendAction(ConfirmPasswordInputChange(password))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = "test@test.com",
            passwordInput = password,
            confirmPasswordInput = password,
            dialog = CreateAccountDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.accept_policies_error.asText(),
                ),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with all inputs valid should show and hide loading dialog`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                )
            } returns RegisterResult.Success(captchaToken = "mock_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = CreateAccountDialog.Loading),
                stateFlow.awaitItem(),
            )
            assertEquals(
                CreateAccountEvent.NavigateToLogin(
                    email = EMAIL,
                    captchaToken = "mock_token",
                ),
                eventFlow.awaitItem(),
            )
            // Make sure loading dialog is hidden:
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
        }
    }

    @Test
    fun `SubmitClick register returns error should update errorDialogState`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                )
            } returns RegisterResult.Error(errorMessage = "mock_error")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.stateFlow.test {
            assertEquals(VALID_INPUT_STATE, awaitItem())
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = CreateAccountDialog.Loading),
                awaitItem(),
            )
            assertEquals(
                VALID_INPUT_STATE.copy(
                    dialog = CreateAccountDialog.Error(
                        BasicDialogState.Shown(
                            title = R.string.an_error_has_occurred.asText(),
                            message = "mock_error".asText(),
                        ),
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
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                )
            } returns RegisterResult.CaptchaRequired(captchaId = "mock_captcha_id")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
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
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                )
            } returns RegisterResult.Success(captchaToken = "mock_captcha_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assertEquals(
                CreateAccountEvent.NavigateToLogin(
                    email = EMAIL,
                    captchaToken = "mock_captcha_token",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueWithBreachedPasswordClick should call repository with checkDataBreaches false`() {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                )
            } returns RegisterResult.Error(null)
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.trySendAction(CreateAccountAction.ContinueWithBreachedPasswordClick)
        coVerify {
            repo.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                captchaToken = null,
                shouldCheckDataBreaches = false,
            )
        }
    }

    @Test
    fun `SubmitClick register returns ShowDataBreaches should show HaveIBeenPwned dialog`() =
        runTest {
            val repo = mockk<AuthRepository> {
                every { captchaTokenResultFlow } returns flowOf()
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                    )
                } returns RegisterResult.DataBreachFound
            }
            val viewModel = CreateAccountViewModel(
                savedStateHandle = validInputHandle,
                authRepository = repo,
            )
            viewModel.actionChannel.trySend(CreateAccountAction.CheckDataBreachesToggle(true))
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            viewModel.stateFlow.test {
                assertEquals(
                    VALID_INPUT_STATE.copy(
                        isCheckDataBreachesToggled = true,
                        dialog = CreateAccountDialog.HaveIBeenPwned,
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
    fun `PasswordInputChange update passwordInput and call getPasswordStrength`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength("", "input")
        } returns Result.failure(Throwable())
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.actionChannel.trySend(PasswordInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordInput = "input"), awaitItem())
        }
        coVerify { mockAuthRepository.getPasswordStrength("", "input") }
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
        viewModel.trySendAction(AcceptPoliciesToggle(true))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(isAcceptPoliciesToggled = true), awaitItem())
        }
    }

    @Test
    fun `ReceivePasswordStrengthResult should update password strength state`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.NONE,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ReceivePasswordStrengthResult(LEVEL_0.asSuccess()))
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ReceivePasswordStrengthResult(LEVEL_1.asSuccess()))
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_2,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ReceivePasswordStrengthResult(LEVEL_2.asSuccess()))
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_3,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ReceivePasswordStrengthResult(LEVEL_3.asSuccess()))
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.GOOD,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ReceivePasswordStrengthResult(LEVEL_4.asSuccess()))
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.STRONG,
                ),
                awaitItem(),
            )
        }
    }

    companion object {
        private const val PASSWORD = "longenoughtpassword"
        private const val EMAIL = "test@test.com"
        private val DEFAULT_STATE = CreateAccountState(
            passwordInput = "",
            emailInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isCheckDataBreachesToggled = true,
            isAcceptPoliciesToggled = false,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.NONE,
        )
        private val VALID_INPUT_STATE = CreateAccountState(
            passwordInput = PASSWORD,
            emailInput = EMAIL,
            confirmPasswordInput = PASSWORD,
            passwordHintInput = "",
            isCheckDataBreachesToggled = false,
            isAcceptPoliciesToggled = true,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.GOOD,
        )
        private const val LOGIN_RESULT_PATH =
            "com.x8bit.bitwarden.data.auth.repository.util.CaptchaUtilsKt"
    }
}