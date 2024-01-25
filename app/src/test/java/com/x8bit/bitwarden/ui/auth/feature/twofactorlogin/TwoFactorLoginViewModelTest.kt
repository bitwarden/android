package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TwoFactorLoginViewModelTest : BaseViewModelTest() {
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { twoFactorResponse } returns TWO_FACTOR_RESPONSE
        every { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
    }

    private val savedStateHandle = SavedStateHandle().also {
        it["email_address"] = "example@email.com"
        it["password"] = "password123"
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
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `captchaTokenFlow success update should trigger a login`() = runTest {
        coEvery {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = "token",
            )
        } returns LoginResult.Success
        createViewModel()
        mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success("token"))
        coVerify {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = "token",
            )
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(TwoFactorLoginAction.CloseButtonClick)
            assertEquals(
                TwoFactorLoginEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `CodeInputChanged should update input and enable button if code is long enough`() =
        runTest {
            val input = "123456"
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(TwoFactorLoginAction.CodeInputChanged(input))
                assertEquals(
                    DEFAULT_STATE.copy(
                        codeInput = input,
                        isContinueButtonEnabled = true,
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `CodeInputChanged should update input and disable button if code is blank`() =
        runTest {
            val input = "123456"
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                // Set it to true.
                viewModel.actionChannel.trySend(TwoFactorLoginAction.CodeInputChanged(input))
                assertEquals(
                    DEFAULT_STATE.copy(
                        codeInput = input,
                        isContinueButtonEnabled = true,
                    ),
                    viewModel.stateFlow.value,
                )

                // Set it to false.
                viewModel.actionChannel.trySend(TwoFactorLoginAction.CodeInputChanged(""))
                assertEquals(
                    DEFAULT_STATE.copy(
                        codeInput = "",
                        isContinueButtonEnabled = false,
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `ContinueButtonClick login returns success should update loadingDialogState`() = runTest {
        coEvery {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        } returns LoginResult.Success

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = TwoFactorLoginState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
        coVerify {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        }
    }

    @Test
    fun `ContinueButtonClick login returns CaptchaRequired should emit NavigateToCaptcha`() =
        runTest {
            val mockkUri = mockk<Uri>()
            every {
                generateUriForCaptcha(captchaId = "mock_captcha_id")
            } returns mockkUri
            coEvery {
                authRepository.login(
                    email = "example@email.com",
                    password = "password123",
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                )
            } returns LoginResult.CaptchaRequired(captchaId = "mock_captcha_id")
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(TwoFactorLoginAction.ContinueButtonClick)

                assertEquals(
                    DEFAULT_STATE,
                    viewModel.stateFlow.value,
                )

                assertEquals(
                    TwoFactorLoginEvent.NavigateToCaptcha(uri = mockkUri),
                    awaitItem(),
                )
            }
            coVerify {
                authRepository.login(
                    email = "example@email.com",
                    password = "password123",
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                )
            }
        }

    @Test
    fun `ContinueButtonClick login returns Error should update errorStateDialog`() = runTest {
        coEvery {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        } returns LoginResult.Error(errorMessage = null)

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = TwoFactorLoginState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = TwoFactorLoginState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.invalid_verification_code.asText(),
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss)
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        coVerify {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        }
    }

    @Test
    fun `RememberMeToggle should update the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(TwoFactorLoginAction.RememberMeToggle(true))
            assertEquals(
                DEFAULT_STATE.copy(
                    isRememberMeEnabled = true,
                ),
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `ResendEmailClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(TwoFactorLoginAction.ResendEmailClick)
            assertEquals(
                TwoFactorLoginEvent.ShowToast("Not yet implemented"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SelectAuthMethod with RECOVERY_CODE should launch the NavigateToRecoveryCode event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(
                    TwoFactorLoginAction.SelectAuthMethod(
                        TwoFactorAuthMethod.RECOVERY_CODE,
                    ),
                )
                assertEquals(
                    TwoFactorLoginEvent.NavigateToRecoveryCode,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SelectAuthMethod with other method should update the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(
                TwoFactorLoginAction.SelectAuthMethod(
                    TwoFactorAuthMethod.AUTHENTICATOR_APP,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP,
                ),
                viewModel.stateFlow.value,
            )
        }
    }

    private fun createViewModel(): TwoFactorLoginViewModel =
        TwoFactorLoginViewModel(
            authRepository = authRepository,
            savedStateHandle = savedStateHandle,
        )

    companion object {
        private val TWO_FACTOR_AUTH_METHODS_DATA = mapOf(
            TwoFactorAuthMethod.EMAIL to mapOf("Email" to "ex***@email.com"),
            TwoFactorAuthMethod.AUTHENTICATOR_APP to mapOf("Email" to null),
        )
        private val TWO_FACTOR_RESPONSE =
            GetTokenResponseJson.TwoFactorRequired(
                TWO_FACTOR_AUTH_METHODS_DATA,
                null,
                null,
            )

        private val DEFAULT_STATE = TwoFactorLoginState(
            authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP,
            availableAuthMethods = listOf(
                TwoFactorAuthMethod.EMAIL,
                TwoFactorAuthMethod.AUTHENTICATOR_APP,
                TwoFactorAuthMethod.RECOVERY_CODE,
            ),
            codeInput = "",
            displayEmail = "ex***@email.com",
            dialogState = null,
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
            captchaToken = null,
            email = "example@email.com",
            password = "password123",
        )
    }
}
