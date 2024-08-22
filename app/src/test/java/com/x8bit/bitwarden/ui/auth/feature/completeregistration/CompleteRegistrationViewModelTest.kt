package com.x8bit.bitwarden.ui.auth.feature.completeregistration

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
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.BackClick
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.Internal.ReceivePasswordStrengthResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class CompleteRegistrationViewModelTest : BaseViewModelTest() {

    /**
     * Saved state handle that has valid inputs. Useful for tests that want to test things
     * after the user has entered all valid inputs.
     */
    private val mockAuthRepository = mockk<AuthRepository>()

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl()

    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.OnboardingFlow) } returns false
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
        val viewModel = createCompleteRegistrationViewModel(DEFAULT_STATE)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `onCleared should erase specialCircumstance`() = runTest {
        specialCircumstanceManager.specialCircumstance = SpecialCircumstance.CompleteRegistration(
            completeRegistrationData = CompleteRegistrationData(
                email = EMAIL,
                verificationToken = TOKEN,
                fromEmail = true,
            ),
            System.currentTimeMillis(),
        )

        val viewModel = CompleteRegistrationViewModel(
            savedStateHandle = SavedStateHandle(mapOf("state" to DEFAULT_STATE)),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository,
            specialCircumstanceManager = specialCircumstanceManager,
            featureFlagManager = featureFlagManager,
        )
        viewModel.onCleared()
        assertTrue(specialCircumstanceManager.specialCircumstance == null)
    }

    @Test
    fun `Password below 12 chars should have non-valid state`() =
        runTest {
            val input = "abcdefghikl"
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, input)
            } returns PasswordStrengthResult.Error
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(input))

            assertFalse(viewModel.stateFlow.value.hasValidMasterPassword)
        }

    @Test
    fun `Passwords not matching should have non-valid state`() =
        runTest {
            coEvery {
                mockAuthRepository.getPasswordStrength(EMAIL, PASSWORD)
            } returns PasswordStrengthResult.Error
            val viewModel = createCompleteRegistrationViewModel()
            viewModel.trySendAction(PasswordInputChange(PASSWORD))

            assertFalse(viewModel.stateFlow.value.hasValidMasterPassword)
        }

    @Test
    fun `CallToActionClick with all inputs valid should show and hide loading dialog`() = runTest {
        val repo = mockk<AuthRepository> {
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    emailVerificationToken = TOKEN,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success(captchaToken = CAPTCHA_BYPASS_TOKEN)
        }
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE, repo)
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = CompleteRegistrationDialog.Loading),
                stateFlow.awaitItem(),
            )
            assertEquals(
                CompleteRegistrationEvent.NavigateToLogin(
                    EMAIL,
                    CAPTCHA_BYPASS_TOKEN,
                ),
                eventFlow.awaitItem(),
            )
            // Make sure loading dialog is hidden:
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
        }
    }

    @Test
    fun `CallToActionClick register returns error should update errorDialogState`() = runTest {
        val repo = mockk<AuthRepository> {
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    emailVerificationToken = TOKEN,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Error(errorMessage = "mock_error")
        }
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE, repo)
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
    fun `CallToActionClick register returns Success should emit NavigateToLogin`() = runTest {
        val repo = mockk<AuthRepository> {
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    emailVerificationToken = TOKEN,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Success(captchaToken = CAPTCHA_BYPASS_TOKEN)
        }
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE, repo)
        viewModel.eventFlow.test {
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            assertEquals(
                CompleteRegistrationEvent.NavigateToLogin(
                    EMAIL,
                    CAPTCHA_BYPASS_TOKEN,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueWithBreachedPasswordClick should call repository with checkDataBreaches false`() {
        val repo = mockk<AuthRepository> {
            coEvery {
                register(
                    email = EMAIL,
                    masterPassword = PASSWORD,
                    masterPasswordHint = null,
                    emailVerificationToken = TOKEN,
                    captchaToken = null,
                    shouldCheckDataBreaches = false,
                    isMasterPasswordStrong = true,
                )
            } returns RegisterResult.Error(null)
        }
        val viewModel = createCompleteRegistrationViewModel(VALID_INPUT_STATE, repo)
        viewModel.trySendAction(CompleteRegistrationAction.ContinueWithBreachedPasswordClick)
        coVerify {
            repo.register(
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
            val initialState = VALID_INPUT_STATE
                .copy(
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
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createCompleteRegistrationViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(BackClick)
            assertEquals(CompleteRegistrationEvent.NavigateBack, awaitItem())
        }
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

    private fun createCompleteRegistrationViewModel(
        completeRegistrationState: CompleteRegistrationState? = DEFAULT_STATE,
        authRepository: AuthRepository = mockAuthRepository,
    ): CompleteRegistrationViewModel =
        CompleteRegistrationViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "state" to completeRegistrationState,
                ),
            ),
            authRepository = authRepository,
            environmentRepository = fakeEnvironmentRepository,
            specialCircumstanceManager = specialCircumstanceManager,
            featureFlagManager = featureFlagManager,
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
            onBoardingEnabled = false,
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
            onBoardingEnabled = false,
            minimumPasswordLength = 12,
        )
    }
}
