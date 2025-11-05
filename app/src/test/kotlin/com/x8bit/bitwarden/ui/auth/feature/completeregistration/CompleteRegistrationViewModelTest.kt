package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
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
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance.RegistrationEvent
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.BackClick
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.Internal.ReceivePasswordStrengthResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordInputChange
import io.mockk.Runs
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
        coEvery { login(email = any(), password = any()) } returns LoginResult.Success
        coEvery {
            register(
                email = any(),
                masterPassword = any(),
                masterPasswordHint = any(),
                emailVerificationToken = any(),
                shouldCheckDataBreaches = any(),
                isMasterPasswordStrong = any(),
            )
        } returns RegisterResult.Success
        coEvery { setOnboardingStatus(OnboardingStatus.NOT_STARTED) } just Runs
    }
    private val toastManager: ToastManager = mockk {
        every { show(messageId = any(), duration = any()) } just runs
    }

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )
    private val mutableGeneratorResultFlow = bufferedMutableSharedFlow<GeneratorResult>()
    private val mockCompleteRegistrationCircumstance =
        mockk<RegistrationEvent.CompleteRegistration>()
    private val generatorRepository = mockk<GeneratorRepository>(relaxed = true) {
        every { generatorResultFlow } returns mutableGeneratorResultFlow
    }

    @BeforeEach
    fun setUp() {
        specialCircumstanceManager.specialCircumstance = mockCompleteRegistrationCircumstance
        mockkStatic(
            SavedStateHandle::toCompleteRegistrationArgs,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toCompleteRegistrationArgs,
        )
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
        } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.trySendAction(PasswordInputChange(input))

        assertFalse(viewModel.stateFlow.value.validSubmissionReady)
    }

    @Test
    fun `Passwords not matching should have non-valid state`() = runTest {
        coEvery {
            mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
        } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
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
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success
            val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
            viewModel.stateFlow.test {
                assertEquals(VALID_INPUT_STATE, awaitItem())
                viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
                assertEquals(
                    VALID_INPUT_STATE.copy(dialog = CompleteRegistrationDialog.Loading),
                    awaitItem(),
                )
                // Make sure loading dialog is hidden:
                assertEquals(VALID_INPUT_STATE, awaitItem())
            }
            verify(exactly = 1) {
                toastManager.show(messageId = BitwardenString.account_created_success)
            }
        }

    @Test
    fun `CallToActionClick register returns error should update errorDialogState`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            mockAuthRepository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                emailVerificationToken = TOKEN,
                shouldCheckDataBreaches = false,
                isMasterPasswordStrong = true,
            )
        } returns RegisterResult.Error(errorMessage = "mock_error", error = error)
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
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = "mock_error".asText(),
                        error = error,
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
            )
        }
    }

    @Test
    fun `when login attempt returns success should wait for state based navigation`() = runTest {
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        viewModel.eventFlow.test {
            expectNoEvents()
        }
        verify(exactly = 1) {
            mockAuthRepository.setOnboardingStatus(OnboardingStatus.NOT_STARTED)
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
                )
            } returns LoginResult.TwoFactorRequired

            val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            viewModel.eventFlow.test {
                assertEquals(
                    CompleteRegistrationEvent.NavigateToLogin(EMAIL),
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                mockAuthRepository.setOnboardingStatus(OnboardingStatus.NOT_STARTED)
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
                shouldCheckDataBreaches = false,
                isMasterPasswordStrong = true,
            )
        } returns RegisterResult.Error(errorMessage = null, error = null)
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE)
        viewModel.trySendAction(CompleteRegistrationAction.ContinueWithBreachedPasswordClick)
        coVerify {
            mockAuthRepository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                emailVerificationToken = TOKEN,
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
                            title = BitwardenString.exposed_master_password.asText(),
                            message = BitwardenString
                                .password_found_in_a_data_breach_alert_description
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
                            title = BitwardenString.weak_master_password.asText(),
                            message = BitwardenString.weak_password_identified_use_a_strong_password_to_protect_your_account.asText(),
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
    fun `On init should show snackbar if from email is true`() = runTest {
        val viewModel = createCompleteRegistrationViewModel(
            DEFAULT_STATE.copy(fromEmail = true),
        )
        viewModel.eventFlow.test {
            assertEquals(
                CompleteRegistrationEvent.ShowSnackbar(BitwardenString.email_verified.asText()),
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
        } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
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
            } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
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
    fun `CreateAccountClick with password below 12 chars should show password length dialog`() =
        runTest {
            val input = "abcdefghikl"
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, input)
            } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(input))
            val expectedState = DEFAULT_STATE.copy(
                passwordInput = input,
                dialog = CompleteRegistrationDialog.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenPlurals.master_password_length_val_message_x.asPluralsText(
                        quantity = 12,
                        args = arrayOf(12),
                    ),
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
            } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(PASSWORD))
            val expectedState = DEFAULT_STATE.copy(
                userEmail = EMAIL,
                passwordInput = PASSWORD,
                dialog = CompleteRegistrationDialog.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.master_password_confirmation_val_message.asText(),
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
        } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
        val viewModel = createCompleteRegistrationViewModel(
            DEFAULT_STATE.copy(userEmail = ""),
        )
        viewModel.trySendAction(PasswordInputChange(PASSWORD))
        val expectedState = DEFAULT_STATE.copy(
            userEmail = "",
            passwordInput = PASSWORD,
            dialog = CompleteRegistrationDialog.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.validation_field_required.asText(
                    BitwardenString.email_address.asText(),
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
        savedStateHandle = SavedStateHandle().apply {
            set(key = "state", value = completeRegistrationState)
            every { toCompleteRegistrationArgs() } returns CompleteRegistrationArgs(
                emailAddress = completeRegistrationState?.userEmail ?: EMAIL,
                verificationToken = completeRegistrationState?.emailVerificationToken ?: TOKEN,
                fromEmail = completeRegistrationState?.fromEmail == true,
            )
        },
        authRepository = mockAuthRepository,
        environmentRepository = fakeEnvironmentRepository,
        specialCircumstanceManager = specialCircumstanceManager,
        generatorRepository = generatorRepository,
        toastManager = toastManager,
    )

    companion object {
        private const val PASSWORD = "longenoughtpassword"
        private const val EMAIL = "test@test.com"
        private const val TOKEN = "token"
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
            minimumPasswordLength = 12,
        )
    }
}
