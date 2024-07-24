package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
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

@Suppress("LargeClass")
class StartRegistrationViewModelTest : BaseViewModelTest() {

    /**
     * Saved state handle that has valid inputs. Useful for tests that want to test things
     * after the user has entered all valid inputs.
     */
    private val validInputHandle = SavedStateHandle(mapOf("state" to VALID_INPUT_STATE))

    private val mockAuthRepository = mockk<AuthRepository> {
        every { captchaTokenResultFlow } returns flowOf()
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
    fun `initial state should be correct`() {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should pull from saved state handle when present`() {
        val savedState = StartRegistrationState(
            emailInput = "",
            nameInput = "",
            isReceiveMarketingEmailsToggled = false,
            isContinueButtonEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            dialog = null
        )
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = handle,
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `ContinueClick with blank email should show email required`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        val input = "a"
        viewModel.trySendAction(EmailInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = input,
            isContinueButtonEnabled = true,
            dialog = StartRegistrationDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.invalid_email.asText(),
                ),
            ),
        )
        viewModel.trySendAction(StartRegistrationAction.ContinueClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `ContinueClick with invalid email should show invalid email`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        val input = " "
        viewModel.trySendAction(EmailInputChange(input))
        val expectedState = DEFAULT_STATE.copy(
            emailInput = input,
            dialog = StartRegistrationDialog.Error(
                BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required
                        .asText(R.string.email_address.asText()),
                ),
            ),
        )
        viewModel.trySendAction(StartRegistrationAction.ContinueClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `ContinueClick with all inputs valid should show and hide loading dialog`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                sendVerificationEmail(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                )
            } returns SendVerificationEmailResult.Success(
                emailVerificationToken = "verification_token"
            )
        }
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
            environmentRepository = fakeEnvironmentRepository
        )
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
            viewModel.trySendAction(StartRegistrationAction.ContinueClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = StartRegistrationDialog.Loading),
                stateFlow.awaitItem(),
            )
            assertEquals(
                StartRegistrationEvent.NavigateToCompleteRegistration(EMAIL, "verification_token"),
                eventFlow.awaitItem(),
            )
            // Make sure loading dialog is hidden:
            assertEquals(VALID_INPUT_STATE, stateFlow.awaitItem())
        }
    }

    @Test
    fun `ContinueClick register returns error should update errorDialogState`() = runTest {
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                sendVerificationEmail(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                )
            } returns SendVerificationEmailResult.Error(errorMessage = "mock_error")
        }
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.stateFlow.test {
            assertEquals(VALID_INPUT_STATE, awaitItem())
            viewModel.trySendAction(StartRegistrationAction.ContinueClick)
            assertEquals(
                VALID_INPUT_STATE.copy(dialog = StartRegistrationDialog.Loading),
                awaitItem(),
            )
            assertEquals(
                VALID_INPUT_STATE.copy(
                    dialog = StartRegistrationDialog.Error(
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
    fun `ContinueClick register returns Success should emit NavigateToCheckEmail`() = runTest {
        val mockkUri = mockk<Uri>()
        every {
            generateUriForCaptcha(captchaId = "mock_captcha_id")
        } returns mockkUri
        val repo = mockk<AuthRepository> {
            every { captchaTokenResultFlow } returns flowOf()
            coEvery {
                sendVerificationEmail(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                )
            } returns SendVerificationEmailResult.Success(emailVerificationToken = "verification_token")
        }
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = validInputHandle,
            authRepository = repo,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(StartRegistrationAction.ContinueClick)
            assertEquals(
                StartRegistrationEvent.NavigateToCompleteRegistration(EMAIL, "verification_token"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(CloseClick)
            assertEquals(StartRegistrationEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `PrivacyPolicyClick should emit NavigatePrivacyPolicy`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(StartRegistrationAction.PrivacyPolicyClick)
            assertEquals(StartRegistrationEvent.NavigateToPrivacyPolicy, awaitItem())
        }
    }

    @Test
    fun `TermsClick should emit NavigateToTerms`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(StartRegistrationAction.TermsClick)
            assertEquals(StartRegistrationEvent.NavigateToTerms, awaitItem())
        }
    }

    @Test
    fun `UnsubscribeMarketingEmailsClick should emit NavigateToUnsubscribe`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(StartRegistrationAction.UnsubscribeMarketingEmailsClick)
            assertEquals(StartRegistrationEvent.NavigateToUnsubscribe, awaitItem())
        }
    }

    @Test
    fun `EnvironmentTypeSelect should update value of selected region for US or EU`() = runTest {
        val inputEnvironmentType = Environment.Type.EU
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(
                StartRegistrationAction.EnvironmentTypeSelect(
                    inputEnvironmentType
                )
            )
            assertEquals(
                DEFAULT_STATE.copy(selectedEnvironmentType = Environment.Type.EU),
                awaitItem(),
            )
        }
    }

    @Test
    fun `EnvironmentTypeSelect should emit NavigateToEnvironment for self-hosted`() = runTest {
        val inputEnvironmentType = Environment.Type.SELF_HOSTED
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                StartRegistrationAction.EnvironmentTypeSelect(
                    inputEnvironmentType
                )
            )
            assertEquals(
                StartRegistrationEvent.NavigateToEnvironment,
                awaitItem(),
            )
        }
    }

    @Test
    fun `EmailInputChange update email`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.trySendAction(EmailInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    emailInput = "input",
                    isContinueButtonEnabled = true,
                ), awaitItem()
            )
        }
    }

    @Test
    fun `NameInputChange update name`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.trySendAction(StartRegistrationAction.NameInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(nameInput = "input"), awaitItem())
        }
    }

    @Test
    fun `ReceiveMarketingEmailsToggle update isReceiveMarketingEmailsToggled`() = runTest {
        val viewModel = StartRegistrationViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockAuthRepository,
            environmentRepository = fakeEnvironmentRepository
        )
        viewModel.trySendAction(StartRegistrationAction.ReceiveMarketingEmailsToggle(false))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(isReceiveMarketingEmailsToggled = false), awaitItem())
        }
    }

    companion object {
        private const val EMAIL = "test@test.com"
        private const val NAME = "name"
        private val DEFAULT_STATE = StartRegistrationState(
            emailInput = "",
            nameInput = "",
            isReceiveMarketingEmailsToggled = true,
            isContinueButtonEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            dialog = null,
        )
        private val VALID_INPUT_STATE = StartRegistrationState(
            emailInput = EMAIL,
            nameInput = NAME,
            isReceiveMarketingEmailsToggled = true,
            isContinueButtonEnabled = true,
            selectedEnvironmentType = Environment.Type.US,
            dialog = null,
        )
    }
}
