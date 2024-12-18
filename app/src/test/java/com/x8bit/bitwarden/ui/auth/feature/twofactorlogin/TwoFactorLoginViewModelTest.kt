package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import androidx.core.net.toUri
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
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForWebAuth
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlDecodeOrNull
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
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

@Suppress("LargeClass")
class TwoFactorLoginViewModelTest : BaseViewModelTest() {
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val mutableDuoTokenResultFlow = bufferedMutableSharedFlow<DuoCallbackTokenResult>()
    private val mutableYubiKeyResultFlow = bufferedMutableSharedFlow<YubiKeyResult>()
    private val mutableWebAuthResultFlow = bufferedMutableSharedFlow<WebAuthResult>()
    private val authRepository: AuthRepository = mockk {
        every { twoFactorResponse } returns TWO_FACTOR_RESPONSE
        every { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
        every { duoTokenResultFlow } returns mutableDuoTokenResultFlow
        every { yubiKeyResultFlow } returns mutableYubiKeyResultFlow
        every { webAuthResultFlow } returns mutableWebAuthResultFlow
        coEvery { login(any(), any(), any(), any(), any()) } returns LoginResult.Success
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val resourceManager: ResourceManager = mockk()

    @BeforeEach
    fun setUp() {
        mockkStatic(
            ::generateUriForCaptcha,
            ::generateUriForWebAuth,
            String::base64UrlDecodeOrNull,
        )
        mockkStatic(Uri::class)
        every {
            DEFAULT_ENCODED_PASSWORD.base64UrlDecodeOrNull()
        } returns DEFAULT_PASSWORD
        every {
            DEFAULT_ENCODED_ORG_IDENTIFIER.base64UrlDecodeOrNull()
        } returns DEFAULT_ORG_IDENTIFIER
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ::generateUriForCaptcha,
            ::generateUriForWebAuth,
            String::base64UrlDecodeOrNull,
        )
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            verify {
                DEFAULT_ENCODED_PASSWORD.base64UrlDecodeOrNull()
            }
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `yubiKeyResultFlow update should populate the input field and attempt login`() {
        val initialState = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.YUBI_KEY)
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
        coVerify(exactly = 1) {
            authRepository.login(
                email = DEFAULT_STATE.email,
                password = DEFAULT_STATE.password,
                twoFactorData = TwoFactorDataModel(
                    code = token,
                    method = TwoFactorAuthMethod.YUBI_KEY.value.toString(),
                    remember = DEFAULT_STATE.isRememberMeEnabled,
                ),
                captchaToken = DEFAULT_STATE.captchaToken,
                orgIdentifier = DEFAULT_STATE.orgIdentifier,
            )
        }
    }

    @Test
    fun `webAuthResultFlow update with success should populate the codeInput and initial login`() {
        val token = "token"
        val initialState = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH)
        coEvery {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = token,
                    method = TwoFactorAuthMethod.WEB_AUTH.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        } returns LoginResult.Success
        val viewModel = createViewModel(state = initialState)

        mutableWebAuthResultFlow.tryEmit(WebAuthResult.Success(token))

        assertEquals(
            initialState.copy(codeInput = token),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = token,
                    method = TwoFactorAuthMethod.WEB_AUTH.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        }
    }

    @Test
    fun `webAuthResultFlow update with failure should display error dialog`() {
        val initialState = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH)
        val viewModel = createViewModel(state = initialState)
        mutableWebAuthResultFlow.tryEmit(WebAuthResult.Failure)
        assertEquals(
            initialState.copy(
                dialogState = TwoFactorLoginState.DialogState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `captchaTokenFlow success update should trigger a login`() = runTest {
        coEvery {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = "token",
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        } returns LoginResult.Success
        createViewModel()
        mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success("token"))
        coVerify {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = "token",
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        }
    }

    @Test
    fun `duoTokenResultFlow success update should trigger a login`() = runTest {
        coEvery {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "token",
                    method = TwoFactorAuthMethod.DUO.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "token",
                    method = TwoFactorAuthMethod.DUO.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                twoFactorProviders = null,
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
    fun `ContinueButtonClick login should show a dialog when auth method is Duo and authUrl is null`() =
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
                twoFactorProviders = null,
            )
            every { authRepository.twoFactorResponse } returns response
            val state = DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.DUO,
            )
            val viewModel = createViewModel(
                state = state,
            )
            viewModel.stateFlow.test {
                assertEquals(
                    state,
                    awaitItem(),
                )
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    state.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.error_connecting_with_the_duo_service_use_a_different_two_step_login_method_or_contact_duo_for_assistance.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login should emit NavigateToWebAuth when auth method is WEB_AUTH and data is non-null`() =
        runTest {
            val data = JsonObject(mapOf("AuthUrl" to JsonPrimitive("bitwarden.com")))
            val response = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = mapOf(TwoFactorAuthMethod.WEB_AUTH to data),
                captchaToken = null,
                ssoToken = null,
                twoFactorProviders = null,
            )
            val mockkUri = mockk<Uri>()
            val headerText = "header"
            val buttonText = "button"
            val returnButtonText = "return"
            every { resourceManager.getString(R.string.fido2_title) } returns headerText
            every {
                resourceManager.getString(R.string.fido2_authenticate_web_authn)
            } returns buttonText
            every {
                resourceManager.getString(R.string.fido2_return_to_app)
            } returns returnButtonText
            every { authRepository.twoFactorResponse } returns response
            every {
                generateUriForWebAuth(
                    baseUrl = Environment.Us.environmentUrlData.baseWebVaultUrlOrDefault,
                    data = data,
                    headerText = headerText,
                    buttonText = buttonText,
                    returnButtonText = returnButtonText,
                )
            } returns mockkUri
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    TwoFactorLoginEvent.NavigateToWebAuth(mockkUri),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login should emit ShowToast when auth method is WEB_AUTH and data is null`() =
        runTest {
            val response = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = emptyMap(),
                captchaToken = null,
                ssoToken = null,
                twoFactorProviders = null,
            )
            every { authRepository.twoFactorResponse } returns response
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    TwoFactorLoginEvent.ShowToast(
                        message = R.string.there_was_an_error_starting_web_authn_two_factor_authentication.asText(),
                    ),
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
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            }
        }

    @Test
    fun `ContinueButtonClick login returns Error should update dialogState`() = runTest {
        coEvery {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                captchaToken = null,
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        }
    }

    @Test
    fun `ContinueButtonClick login returns Error with message should update dialogState`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            } returns LoginResult.Error(errorMessage = "Mock error message")

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
                            message = "Mock error message".asText(),
                        ),
                    ),
                    awaitItem(),
                )

                viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss)
                assertEquals(DEFAULT_STATE, awaitItem())
            }
            coVerify {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login returns UnofficialServerError should update dialogState`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            } returns LoginResult.UnofficialServerError

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
                            message = R.string.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss)
                assertEquals(DEFAULT_STATE, awaitItem())
            }
            coVerify {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login returns CertificateError should update dialogState`() =
        runTest {
            coEvery {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            } returns LoginResult.CertificateError

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
                            message = R.string.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss)
                assertEquals(DEFAULT_STATE, awaitItem())
            }
            coVerify {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    captchaToken = null,
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
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
            val mockkUri = mockk<Uri>()
            every {
                Uri.parse("https://vault.bitwarden.com/#/recover-2fa")
            } returns mockkUri

            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    TwoFactorLoginAction.SelectAuthMethod(
                        TwoFactorAuthMethod.RECOVERY_CODE,
                    ),
                )
                assertEquals(
                    TwoFactorLoginEvent.NavigateToRecoveryCode(
                        uri = "https://vault.bitwarden.com/#/recover-2fa".toUri(),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SelectAuthMethod with other method should update the state`() {
        val viewModel = createViewModel()

        // To method with continue button enabled by default
        viewModel.trySendAction(
            TwoFactorLoginAction.SelectAuthMethod(
                TwoFactorAuthMethod.DUO,
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.DUO,
                isContinueButtonEnabled = true,
            ),
            viewModel.stateFlow.value,
        )

        // To method with continue button disabled by default
        viewModel.trySendAction(
            TwoFactorLoginAction.SelectAuthMethod(
                TwoFactorAuthMethod.YUBI_KEY,
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.YUBI_KEY,
                isContinueButtonEnabled = false,
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
            environmentRepository = environmentRepository,
            resourceManager = resourceManager,
            savedStateHandle = SavedStateHandle().also {
                it["state"] = state
                it["email_address"] = DEFAULT_EMAIL_ADDRESS
                it["password"] = DEFAULT_ENCODED_PASSWORD
                it["org_identifier"] = DEFAULT_ENCODED_ORG_IDENTIFIER
            },
        )
}

private val TWO_FACTOR_AUTH_METHODS_DATA = mapOf(
    TwoFactorAuthMethod.EMAIL to JsonObject(
        mapOf("Email" to JsonPrimitive("ex***@email.com")),
    ),
    TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
)

private val TWO_FACTOR_RESPONSE = GetTokenResponseJson.TwoFactorRequired(
    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
    captchaToken = null,
    ssoToken = null,
    twoFactorProviders = null,
)
private const val DEFAULT_EMAIL_ADDRESS = "example@email.com"
private const val DEFAULT_ORG_IDENTIFIER = "org_identifier"
private const val DEFAULT_ENCODED_ORG_IDENTIFIER = "org_identifier"
private const val DEFAULT_PASSWORD = "password123"
private const val DEFAULT_ENCODED_PASSWORD = "base64EncodedPassword"
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
    email = DEFAULT_EMAIL_ADDRESS,
    password = DEFAULT_PASSWORD,
    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
)
