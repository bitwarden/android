package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_0
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_1
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_2
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_3
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_4
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.Internal.ReceivePasswordStrengthResult
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
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
        mockkStatic(::generateUriForCaptcha)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::generateUriForCaptcha)
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
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.invalid_email.asText(),
            ),
        )
        viewModel.trySendAction(CreateAccountAction.SubmitClick)
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
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required
                    .asText(R.string.email_address.asText()),
            ),
        )
        viewModel.trySendAction(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with password below 12 chars should show password length dialog`() = runTest {
        val input = "abcdefghikl"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", input)
        } returns PasswordStrengthResult.Error
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
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.master_password_length_val_message_x.asText(12),
            ),
        )
        viewModel.trySendAction(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with passwords not matching should show password match dialog`() = runTest {
        val input = "testtesttesttest"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", input)
        } returns PasswordStrengthResult.Error
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
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.master_password_confirmation_val_message.asText(),
            ),
        )
        viewModel.trySendAction(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick without policies accepted should show accept policies error`() = runTest {
        val password = "testtesttesttest"
        coEvery {
            mockAuthRepository.getPasswordStrength("test@test.com", password)
        } returns PasswordStrengthResult.Error
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
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.accept_policies_error.asText(),
            ),
        )
        viewModel.trySendAction(CreateAccountAction.SubmitClick)
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
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success(captchaToken = "mock_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
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
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Error(errorMessage = "mock_error")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.stateFlow.test {
            assertEquals(VALID_INPUT_STATE, awaitItem())
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = CreateAccountDialog.Loading),
                awaitItem(),
            )
            assertEquals(
                VALID_INPUT_STATE.copy(
                    dialog = CreateAccountDialog.Error(
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
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.CaptchaRequired(captchaId = "mock_captcha_id")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
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
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success(captchaToken = "mock_captcha_token")
        }
        val viewModel = CreateAccountViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
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
                    isMasterPasswordStrong = true,
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
                isMasterPasswordStrong = true,
            )
        }
    }

    @Test
    fun `SubmitClick register returns ShowDataBreaches should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                every { captchaTokenResultFlow } returns flowOf()
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                        isMasterPasswordStrong = true,
                    )
                } returns RegisterResult.DataBreachFound
            }
            val initialState = VALID_INPUT_STATE.copy(
                isCheckDataBreachesToggled = true,
            )
            val viewModel = createCreateAccountViewModel(createAccountState = initialState)
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(
                        dialog = createHaveIBeenPwned(
                            title = R.string.exposed_master_password.asText(),
                            message = R.string.password_found_in_a_data_breach_alert_description
                                .asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SubmitClick register returns DataBreachAndWeakPassword should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                every { captchaTokenResultFlow } returns emptyFlow()
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                        isMasterPasswordStrong = false,
                    )
                } returns RegisterResult.DataBreachAndWeakPassword
            }
            val initialState = VALID_INPUT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.WEAK_1,
                isCheckDataBreachesToggled = true,
            )

            val viewModel = createCreateAccountViewModel(createAccountState = initialState)
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(dialog = createHaveIBeenPwned()),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SubmitClick register returns WeakPassword should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                every { captchaTokenResultFlow } returns flowOf()
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                        isMasterPasswordStrong = false,
                    )
                } returns RegisterResult.WeakPassword
            }
            val initialState = VALID_INPUT_STATE
                .copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                    isCheckDataBreachesToggled = true,
                )
            val viewModel = createCreateAccountViewModel(createAccountState = initialState)
            viewModel.trySendAction(CreateAccountAction.SubmitClick)
            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(
                        dialog = createHaveIBeenPwned(
                            title = R.string.weak_master_password.asText(),
                            message = R.string.weak_password_identified_use_a_strong_password_to_protect_your_account.asText(),
                        ),
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
            viewModel.trySendAction(CloseClick)
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
            viewModel.trySendAction(CreateAccountAction.PrivacyPolicyClick)
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
            viewModel.trySendAction(CreateAccountAction.TermsClick)
            assertEquals(CreateAccountEvent.NavigateToTerms, awaitItem())
        }
    }

    @Test
    fun `ConfirmPasswordInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(ConfirmPasswordInputChange("input"))
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
        viewModel.trySendAction(EmailInputChange("input"))
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
        viewModel.trySendAction(PasswordHintChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordHintInput = "input"), awaitItem())
        }
    }

    @Test
    fun `PasswordInputChange update passwordInput and call getPasswordStrength`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength("", "input")
        } returns PasswordStrengthResult.Error
        val viewModel = CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
        )
        viewModel.trySendAction(PasswordInputChange("input"))
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

            viewModel.trySendAction(
                ReceivePasswordStrengthResult(PasswordStrengthResult.Success(LEVEL_0)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ReceivePasswordStrengthResult(PasswordStrengthResult.Success(LEVEL_1)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_2,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ReceivePasswordStrengthResult(PasswordStrengthResult.Success(LEVEL_2)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_3,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ReceivePasswordStrengthResult(PasswordStrengthResult.Success(LEVEL_3)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.GOOD,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ReceivePasswordStrengthResult(PasswordStrengthResult.Success(LEVEL_4)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.STRONG,
                ),
                awaitItem(),
            )
        }
    }

    private fun createCreateAccountViewModel(
        createAccountState: CreateAccountState? = null,
        authRepository: AuthRepository = mockAuthRepository,
    ): CreateAccountViewModel =
        CreateAccountViewModel(
            savedStateHandle = SavedStateHandle(mapOf("state" to createAccountState)),
            authRepository = authRepository,
        )

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
    }
}
