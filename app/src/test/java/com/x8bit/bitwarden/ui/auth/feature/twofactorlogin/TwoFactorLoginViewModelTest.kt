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
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TwoFactorLoginViewModelTest : BaseViewModelTest() {
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val mutableDuoTokenResultFlow =
        bufferedMutableSharedFlow<DuoCallbackTokenResult>()
    private val mutableYubiKeyResultFlow = bufferedMutableSharedFlow<YubiKeyResult>()
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { twoFactorResponse } returns TWO_FACTOR_RESPONSE
        every { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
        every { duoTokenResultFlow } returns mutableDuoTokenResultFlow
        every { yubiKeyResultFlow } returns mutableYubiKeyResultFlow
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::generateUriForCaptcha)
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::generateUriForCaptcha)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `yubiKeyResultFlow update should populate the input field`() {
        val initialState = DEFAULT_STATE
        val token = "token"
        val viewModel = createViewModel(initialState)
        mutableYubiKeyResultFlow.tryEmit(YubiKeyResult(token))
        assertEquals(
            initialState.copy(
                codeInput = token,
                isContinueButtonEnabled = true,
            ),
            viewModel.stateFlow.value,
        )
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
    fun `duoTokenResultFlow success update should trigger a login`() = runTest {
        coEvery {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "token",
                    method = TwoFactorAuthMethod.DUO.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        } returns LoginResult.Success
        createViewModel(
            state = DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.DUO,
            ),
        )
        mutableDuoTokenResultFlow.tryEmit(DuoCallbackTokenResult.Success("token"))
        coVerify {
            authRepository.login(
                email = "example@email.com",
                password = "password123",
                twoFactorData = TwoFactorDataModel(
                    code = "token",
                    method = TwoFactorAuthMethod.DUO.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
            )
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(TwoFactorLoginAction.CloseButtonClick)
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
            viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(input))
            assertEquals(
                DEFAULT_STATE.copy(
                    codeInput = input,
                    isContinueButtonEnabled = true,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `CodeInputChanged should update input and disable button if code is blank`() {
        val input = "123456"
        val viewModel = createViewModel()
        // Set it to true.
        viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(input))
        assertEquals(
            DEFAULT_STATE.copy(
                codeInput = input,
                isContinueButtonEnabled = true,
            ),
            viewModel.stateFlow.value,
        )

        // Set it to false.
        viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(""))
        assertEquals(
            DEFAULT_STATE.copy(
                codeInput = "",
                isContinueButtonEnabled = false,
            ),
            viewModel.stateFlow.value,
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login should emit NavigateToDuo when auth method is Duo and authUrl is non-null`() =
        runTest {
            val authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO to JsonObject(
                    mapOf("AuthUrl" to JsonPrimitive("bitwarden.com")),
                ),
            )
            val response = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = authMethodsData,
                captchaToken = null,
                ssoToken = null,
            )
            every { authRepository.twoFactorResponse } returns response
            val mockkUri = mockk<Uri>()
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.DUO,
                ),
            )
            every { Uri.parse("bitwarden.com") } returns mockkUri
            viewModel.eventFlow.test {
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    TwoFactorLoginEvent.NavigateToDuo(mockkUri),
                    awaitItem(),
                )
            }
            verify {
                Uri.parse("bitwarden.com")
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login should emit ShowToast when auth method is Duo and authUrl is null`() =
        runTest {
            val authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO to JsonObject(
                    mapOf("Nothing" to JsonPrimitive("Nothing")),
                ),
            )
            val response = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = authMethodsData,
                captchaToken = null,
                ssoToken = null,
            )
            every { authRepository.twoFactorResponse } returns response
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.DUO,
                ),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    TwoFactorLoginEvent.ShowToast(R.string.generic_error_message.asText()),
                    awaitItem(),
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
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)

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
    fun `ContinueButtonClick login returns Error should update dialogState`() = runTest {
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
    fun `RememberMeToggle should update the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(TwoFactorLoginAction.RememberMeToggle(true))
        assertEquals(
            DEFAULT_STATE.copy(
                isRememberMeEnabled = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ResendEmailClick returns success should emit ShowToast`() = runTest {
        coEvery {
            authRepository.resendVerificationCodeEmail()
        } returns ResendEmailResult.Success

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                TwoFactorLoginAction.SelectAuthMethod(
                    TwoFactorAuthMethod.EMAIL,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.EMAIL),
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick)

            assertEquals(
                DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.EMAIL),
                viewModel.stateFlow.value,
            )

            assertEquals(
                TwoFactorLoginEvent.ShowToast(message = R.string.verification_email_sent.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ResendEmailClick returns error should update dialogState`() = runTest {
        coEvery {
            authRepository.resendVerificationCodeEmail()
        } returns ResendEmailResult.Error(message = null)

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(
                TwoFactorLoginAction.SelectAuthMethod(
                    TwoFactorAuthMethod.EMAIL,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.EMAIL),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.EMAIL,
                    dialogState = TwoFactorLoginState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.verification_email_not_sent.asText(),
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.EMAIL,
                    dialogState = TwoFactorLoginState.DialogState.Loading(
                        message = R.string.submitting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.EMAIL,
                    dialogState = TwoFactorLoginState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.verification_email_not_sent.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SelectAuthMethod with RECOVERY_CODE should launch the NavigateToRecoveryCode event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
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
    fun `SelectAuthMethod with other method should update the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
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

    @Test
    @Suppress("MaxLineLength")
    fun `ReceiveResendEmailResult with ResendEmailResult Success and isUserInitiated true should ShowToast`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                        resendEmailResult = ResendEmailResult.Success,
                        isUserInitiated = true,
                    ),
                )
                assertEquals(
                    TwoFactorLoginEvent.ShowToast(
                        message = R.string.verification_email_sent.asText(),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `ReceiveResendEmailResult with ResendEmailResult Success and isUserInitiated false should not emit any events`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                        resendEmailResult = ResendEmailResult.Success,
                        isUserInitiated = false,
                    ),
                )
                expectNoEvents()
            }
        }

    @Test
    fun `ReceiveResendEmailResult with ResendEmailResult Error should not emit any events`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                        resendEmailResult = ResendEmailResult.Error(message = null),
                        isUserInitiated = true,
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.verification_email_not_sent.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(
        state: TwoFactorLoginState? = null,
    ): TwoFactorLoginViewModel =
        TwoFactorLoginViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle().also {
                it["state"] = state
                it["email_address"] = "example@email.com"
                it["password"] = "password123"
            },
        )

    companion object {
        private val TWO_FACTOR_AUTH_METHODS_DATA = mapOf(
            TwoFactorAuthMethod.EMAIL to JsonObject(
                mapOf("Email" to JsonPrimitive("ex***@email.com")),
            ),
            TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
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
