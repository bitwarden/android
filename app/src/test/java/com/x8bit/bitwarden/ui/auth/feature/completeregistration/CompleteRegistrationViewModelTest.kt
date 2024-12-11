package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_0
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_1
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_2
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_3
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_4
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance.RegistrationEvent
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.BackClick
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.Internal.ReceivePasswordStrengthResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class CompleteRegistrationViewModelTest : BaseViewModelTest() {

    /**
     * Saved state handle that has valid inputs. Useful for tests that want to test things
     * after the user has entered all valid inputs.
     */
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val mockAuthRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        coEvery {
            login(
                email = any(),
                password = any(),
                captchaToken = any(),
            )
        } returns LoginResult.Success

        coEvery {
            register(
                email = any(),
                masterPassword = any(),
                masterPasswordHint = any(),
                emailVerificationToken = any(),
                captchaToken = any(),
                shouldCheckDataBreaches = any(),
                isMasterPasswordStrong = any(),
            )
        } returns RegisterResult.Success(captchaToken = CAPTCHA_BYPASS_TOKEN)
    }

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val mutableFeatureFlagFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.OnboardingFlow) } returns false
        every { getFeatureFlagFlow(FlagKey.OnboardingFlow) } returns mutableFeatureFlagFlow
    }
    private val mutableGeneratorResultFlow = bufferedMutableSharedFlow<GeneratorResult>()
    private val mockCompleteRegistrationCircumstance =
        mockk<RegistrationEvent.CompleteRegistration>()
    private val generatorRepository = mockk<GeneratorRepository>(relaxed = true) {
        every { generatorResultFlow } returns mutableGeneratorResultFlow
    }

    @BeforeEach
    fun setUp() {
        specialCircumstanceManager.specialCircumstance = mockCompleteRegistrationCircumstance
        mockkStatic(::generateUriForCaptcha)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::generateUriForCaptcha)
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createCompleteRegistrationViewModel(DEFAULT_STATE)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `Password below 12 chars should have non-valid state`() = runTest {
        val input = "abcdefghikl"
        coEvery {
            mockAuthRepository.getPasswordStrength(EMAIL, input)
        } returns PasswordStrengthResult.Error
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(PasswordInputChange(input))

        assertFalse(viewModel.stateFlow.value.validSubmissionReady)
    }

    @Test
    fun `Passwords not matching should have non-valid state`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
        } returns PasswordStrengthResult.Error
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(PasswordInputChange(PASSWORD))

        assertFalse(viewModel.stateFlow.value.validSubmissionReady)
    }

    @Test
    fun `CallToActionClick with all inputs valid should account created toast and hide dialog`() =
        runTest {
            coEvery {
                mockAuthRepository.register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    emailVerificationToken = TOKEN,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success(captchaToken = CAPTCHA_BYPASS_TOKEN)
            val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
                viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
                assertEquals(
                    VALID_INPUT_STATE.copy(dialog = CompleteRegistrationDialog.Loading),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    CompleteRegistrationEvent.ShowToast(R.string.account_created_success.asText()),
                    eventFlow.awaitItem(),
                )
                // Make sure loading dialog is hidden:
                assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
            }
        }

    @Test
    fun `CallToActionClick register returns error should update errorDialogState`() = runTest {
        coEvery {
            mockAuthRepository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                emailVerificationToken = TOKEN,
                captchaToken = null,
                shouldCheckDataBreaches = false,
                isMasterPasswordStrong = true,
            )
        } returns RegisterResult.Error(errorMessage = "mock_error")
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.stateFlow.test {
            assertEquals(VALID_INPUT_STATE, awaitItem())
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = CompleteRegistrationDialog.Loading),
                awaitItem(),
            )
            assertEquals(
                VALID_INPUT_STATE.copy(
                    dialog = CompleteRegistrationDialog.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = "mock_error".asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CallToActionClick register returns Success should attempt login`() = runTest {
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        coVerify {
            mockAuthRepository.login(
                email = EMAIL,
                password = PASSWORD,
                captchaToken = CAPTCHA_BYPASS_TOKEN,
            )
        }
    }

    @Test
    fun `when login attempt returns success should wait for state based navigation`() = runTest {
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        viewModel.eventFlow.test {
            assertTrue(awaitItem() is CompleteRegistrationEvent.ShowToast)
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when login attempt returns anything other than success should send navigate to login event`() =
        runTest {
            coEvery {
                mockAuthRepository.login(
                    email = EMAIL,
                    password = PASSWORD,
                    captchaToken = CAPTCHA_BYPASS_TOKEN,
                )
            } returns LoginResult.TwoFactorRequired

            val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            viewModel.eventFlow.test {
                assertTrue(awaitItem() is CompleteRegistrationEvent.ShowToast)
                assertEquals(
                    CompleteRegistrationEvent.NavigateToLogin(EMAIL, CAPTCHA_BYPASS_TOKEN),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ContinueWithBreachedPasswordClick should call repository with checkDataBreaches false`() {
        coEvery {
            mockAuthRepository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                emailVerificationToken = TOKEN,
                captchaToken = null,
                shouldCheckDataBreaches = false,
                isMasterPasswordStrong = true,
            )
        } returns RegisterResult.Error(null)
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.trySendAction(CompleteRegistrationAction.ContinueWithBreachedPasswordClick)
        coVerify {
            mockAuthRepository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                emailVerificationToken = TOKEN,
                captchaToken = null,
                shouldCheckDataBreaches = false,
                isMasterPasswordStrong = true,
            )
        }
    }

    @Test
    fun `CallToActionClick register returns ShowDataBreaches should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        emailVerificationToken = TOKEN,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                        isMasterPasswordStrong = true,
                    )
                } returns RegisterResult.DataBreachFound
            }
            val initialState = VALID_INPUT_STATE.copy(
                isCheckDataBreachesToggled = true,
            )
            val viewModel = createCompleteRegistrationViewModel(
                completeRegistrationState = initialState,
            )
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
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
    fun `CallToActionClick register returns DataBreachAndWeakPassword should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        emailVerificationToken = TOKEN,
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

            val viewModel =
                createCompleteRegistrationViewModel(completeRegistrationState = initialState)
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            viewModel.stateFlow.test {
                assertEquals(
                    initialState.copy(dialog = createHaveIBeenPwned()),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `CallToActionClick register returns WeakPassword should show HaveIBeenPwned dialog`() =
        runTest {
            mockAuthRepository.apply {
                coEvery {
                    register(
                        email = EMAIL,
                        masterPassword = PASSWORD,
                        masterPasswordHint = null,
                        emailVerificationToken = TOKEN,
                        captchaToken = null,
                        shouldCheckDataBreaches = true,
                        isMasterPasswordStrong = false,
                    )
                } returns RegisterResult.WeakPassword
            }
            val initialState = VALID_INPUT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.WEAK_1,
                isCheckDataBreachesToggled = true,
            )
            val viewModel =
                createCompleteRegistrationViewModel(completeRegistrationState = initialState)
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
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
    fun `CloseClick should emit NavigateBack and clear special circumstances`() = runTest {
        assertEquals(
            mockCompleteRegistrationCircumstance,
            specialCircumstanceManager.specialCircumstance,
        )
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(BackClick)
            assertEquals(CompleteRegistrationEvent.NavigateBack, awaitItem())
        }

        assertNull(specialCircumstanceManager.specialCircumstance)
    }

    @Test
    fun `On init should show toast if from email is true`() = runTest {
        val viewModel = createCompleteRegistrationViewModel(
            DEFAULT_STATE.copy(fromEmail = true),
        )
        viewModel.eventFlow.test {
            assertEquals(
                CompleteRegistrationEvent.ShowToast(R.string.email_verified.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmPasswordInputChange update passwordInput`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(ConfirmPasswordInputChange(PASSWORD))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(confirmPasswordInput = PASSWORD), awaitItem())
        }
    }

    @Test
    fun `PasswordHintChange update passwordInput`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(PasswordHintChange(PASSWORD))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordHintInput = PASSWORD), awaitItem())
        }
    }

    @Test
    fun `PasswordInputChange update passwordInput and call getPasswordStrength`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
        } returns PasswordStrengthResult.Error
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(PasswordInputChange(PASSWORD))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordInput = PASSWORD), awaitItem())
        }
        coVerify { mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD) }
    }

    @Test
    fun `Empty PasswordInputChange update should result in password strength being NONE`() =
        runTest {
            val viewModel = createCompleteRegistrationViewModel(
                completeRegistrationState = DEFAULT_STATE.copy(
                    passwordInput = PASSWORD,
                    passwordStrengthState = PasswordStrengthState.STRONG,
                ),
            )
            viewModel.trySendAction(PasswordInputChange(""))
            val expectedStrengthUpdateState = DEFAULT_STATE.copy(
                passwordInput = "",
                passwordStrengthState = PasswordStrengthState.NONE,
            )
            viewModel.stateFlow.test {
                assertEquals(expectedStrengthUpdateState, awaitItem())
            }
            coVerify(exactly = 0) { mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `Internal GeneratedPasswordResult update passwordInput and confirmPasswordInput and call getPasswordStrength`() =
        runTest {
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
            } returns PasswordStrengthResult.Error
            val viewModel = createCompleteRegistrationViewModel()
            mutableGeneratorResultFlow.emit(GeneratorResult.Password(PASSWORD))
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(
                        passwordInput = PASSWORD,
                        confirmPasswordInput = PASSWORD,
                    ),
                    awaitItem(),
                )
            }
            coVerify { mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD) }
        }

    @Test
    fun `CheckDataBreachesToggle should change isCheckDataBreachesToggled`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(CompleteRegistrationAction.CheckDataBreachesToggle(true))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(isCheckDataBreachesToggled = true), awaitItem())
        }
    }

    @Test
    fun `ReceivePasswordStrengthResult should update password strength state`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
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

    @Test
    fun `handling LearnToPreventLockoutClick should emit NavigateToPreventAccountLockout`() =
        runTest {
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(CompleteRegistrationAction.LearnToPreventLockoutClick)
                assertEquals(CompleteRegistrationEvent.NavigateToPreventAccountLockout, awaitItem())
            }
        }

    @Test
    fun `handling MakePasswordStrongClick should emit NavigateToMakePasswordStrong`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CompleteRegistrationAction.MakePasswordStrongClick)
            assertEquals(CompleteRegistrationEvent.NavigateToMakePasswordStrong, awaitItem())
        }
    }

    @Test
    fun `feature flag state update is captured in ViewModel state`() {
        mutableFeatureFlagFlow.value = true
        val viewModel = createCompleteRegistrationViewModel()
        assertTrue(viewModel.stateFlow.value.onboardingEnabled)
    }

    @Test
    fun `CreateAccountClick with password below 12 chars should show password length dialog`() =
        runTest {
            val input = "abcdefghikl"
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, input)
            } returns PasswordStrengthResult.Error
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(input))
            val expectedState = DEFAULT_STATE.copy(
                passwordInput = input,
                dialog = CompleteRegistrationDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_length_val_message_x.asText(12),
                ),
            )
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            viewModel.stateFlow.test {
                assertEquals(expectedState, awaitItem())
            }
        }

    @Test
    fun `CreateAccountClick with passwords not matching should show password match dialog`() =
        runTest {
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
            } returns PasswordStrengthResult.Error
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(PASSWORD))
            val expectedState = DEFAULT_STATE.copy(
                userEmail = EMAIL,
                passwordInput = PASSWORD,
                dialog = CompleteRegistrationDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_confirmation_val_message.asText(),
                ),
            )
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            viewModel.stateFlow.test {
                assertEquals(expectedState, awaitItem())
            }
        }

    @Test
    fun `CreateAccountClick with no email not should show dialog`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength("", PASSWORD)
        } returns PasswordStrengthResult.Error
        val viewModel = createCompleteRegistrationViewModel(
            DEFAULT_STATE.copy(userEmail = ""),
        )
        viewModel.trySendAction(PasswordInputChange(PASSWORD))
        val expectedState = DEFAULT_STATE.copy(
            userEmail = "",
            passwordInput = PASSWORD,
            dialog = CompleteRegistrationDialog.Error(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required.asText(
                    R.string.email_address.asText(),
                ),
            ),
        )
        viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    private fun createCompleteRegistrationViewModel(
        completeRegistrationState: CompleteRegistrationState? = DEFAULT_STATE,
    ): CompleteRegistrationViewModel = CompleteRegistrationViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "state" to completeRegistrationState,
            ),
        ),
        authRepository = mockAuthRepository,
        environmentRepository = fakeEnvironmentRepository,
        specialCircumstanceManager = specialCircumstanceManager,
        featureFlagManager = featureFlagManager,
        generatorRepository = generatorRepository,
    )

    companion object {
        private const val PASSWORD = "longenoughtpassword"
        private const val EMAIL = "test@test.com"
        private const val TOKEN = "token"
        private const val CAPTCHA_BYPASS_TOKEN = "captcha_bypass"
        private val DEFAULT_STATE = CompleteRegistrationState(
            userEmail = EMAIL,
            emailVerificationToken = TOKEN,
            fromEmail = false,
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isCheckDataBreachesToggled = true,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.NONE,
            onboardingEnabled = false,
            minimumPasswordLength = 12,
        )
        private val VALID_INPUT_STATE = CompleteRegistrationState(
            userEmail = EMAIL,
            emailVerificationToken = TOKEN,
            fromEmail = false,
            passwordInput = PASSWORD,
            confirmPasswordInput = PASSWORD,
            passwordHintInput = "",
            isCheckDataBreachesToggled = false,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.GOOD,
            onboardingEnabled = false,
            minimumPasswordLength = 12,
        )
    }
}
