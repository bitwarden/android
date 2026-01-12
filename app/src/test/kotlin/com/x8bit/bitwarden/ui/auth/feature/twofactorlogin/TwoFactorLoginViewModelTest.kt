package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.network.model.TwoFactorDataModel
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForWebAuth
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
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
    private val mutableDuoTokenResultFlow = bufferedMutableSharedFlow<DuoCallbackTokenResult>()
    private val mutableYubiKeyResultFlow = bufferedMutableSharedFlow<YubiKeyResult>()
    private val mutableWebAuthResultFlow = bufferedMutableSharedFlow<WebAuthResult>()
    private val authRepository: AuthRepository = mockk {
        every { twoFactorResponse } returns TWO_FACTOR_RESPONSE
        every { duoTokenResultFlow } returns mutableDuoTokenResultFlow
        every { yubiKeyResultFlow } returns mutableYubiKeyResultFlow
        every { webAuthResultFlow } returns mutableWebAuthResultFlow
        coEvery { resendNewDeviceOtp() } returns ResendEmailResult.Success
        coEvery {
            login(
                email = any(),
                password = any(),
                twoFactorData = any(),
                orgIdentifier = any(),
            )
        } returns LoginResult.Success
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val resourceManager: ResourceManager = mockk()

    @BeforeEach
    fun setUp() {
        mockkStatic(
            ::generateUriForWebAuth,
            SavedStateHandle::toTwoFactorLoginArgs,
        )
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ::generateUriForWebAuth,
            SavedStateHandle::toTwoFactorLoginArgs,
        )
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
    fun `init with email auth method and not new device verification should call resendEmail`() {
        val initialState = DEFAULT_STATE.copy(
            authMethod = TwoFactorAuthMethod.EMAIL,
            isNewDeviceVerification = false,
        )
        coEvery { authRepository.resendVerificationCodeEmail() } returns ResendEmailResult.Success

        createViewModel(state = initialState)

        coVerify(exactly = 1) {
            authRepository.resendVerificationCodeEmail()
        }
    }

    @Test
    fun `init with email auth method and new device verification should not call resendEmail`() {
        val initialState = DEFAULT_STATE.copy(
            authMethod = TwoFactorAuthMethod.EMAIL,
            isNewDeviceVerification = true,
        )
        coEvery { authRepository.resendVerificationCodeEmail() } returns ResendEmailResult.Success

        createViewModel(state = initialState)

        coVerify(exactly = 0) {
            authRepository.resendVerificationCodeEmail()
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `init with non-email auth method and not new device verification should not call resendEmail`() {
        val initialState = DEFAULT_STATE.copy(
            authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP,
            isNewDeviceVerification = false,
        )
        coEvery { authRepository.resendVerificationCodeEmail() } returns ResendEmailResult.Success

        createViewModel(state = initialState)

        coVerify(exactly = 0) {
            authRepository.resendVerificationCodeEmail()
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `init with non-email auth method and new device verification should not call resendEmail`() {
        val initialState = DEFAULT_STATE.copy(
            authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP,
            isNewDeviceVerification = true,
        )
        createViewModel(state = initialState)

        coVerify(exactly = 0) { authRepository.resendVerificationCodeEmail() }
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
                    remember = DEFAULT_STATE.isRememberEnabled,
                ),
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
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        }
    }

    @Test
    fun `webAuthResultFlow failure without message should display generic error dialog`() {
        val initialState = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH)
        val viewModel = createViewModel(state = initialState)
        mutableWebAuthResultFlow.tryEmit(WebAuthResult.Failure(message = null))
        assertEquals(
            initialState.copy(
                dialogState = TwoFactorLoginState.DialogState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `webAuthResultFlow failure with message should display error dialog with message`() {
        val initialState = DEFAULT_STATE.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH)
        val viewModel = createViewModel(state = initialState)
        val errorMessage = "An error"
        mutableWebAuthResultFlow.tryEmit(WebAuthResult.Failure(message = errorMessage))
        assertEquals(
            initialState.copy(
                dialogState = TwoFactorLoginState.DialogState.Error(
                    message = errorMessage.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
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
    @Suppress("MaxLineLength")
    fun `Continue buttons should only be enabled when code is not empty`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(""))

        // 6 digit should be false when isNewDeviceVerification is true.
        assertEquals(
            DEFAULT_STATE.copy(
                codeInput = "",
                isContinueButtonEnabled = false,
            ),
            viewModel.stateFlow.value,
        )

        // Set it to true.
        viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged("12345678"))
        assertEquals(
            DEFAULT_STATE.copy(
                codeInput = "12345678",
                isContinueButtonEnabled = true,
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
                        message = BitwardenString.logging_in.asText(),
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
                    TwoFactorLoginEvent.NavigateToDuo(uri = mockkUri, scheme = "bitwarden"),
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
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.error_connecting_with_the_duo_service_use_a_different_two_step_login_method_or_contact_duo_for_assistance.asText(),
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
                ssoToken = null,
                twoFactorProviders = null,
            )
            val mockkUri = mockk<Uri>()
            val headerText = "header"
            val buttonText = "button"
            val returnButtonText = "return"
            every { resourceManager.getString(BitwardenString.fido2_title) } returns headerText
            every {
                resourceManager.getString(BitwardenString.fido2_authenticate_web_authn)
            } returns buttonText
            every {
                resourceManager.getString(BitwardenString.fido2_return_to_app)
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
                    TwoFactorLoginEvent.NavigateToWebAuth(uri = mockkUri, scheme = "bitwarden"),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick login should emit ShowSnackbar when auth method is WEB_AUTH and data is null`() =
        runTest {
            val response = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = emptyMap(),
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
                    TwoFactorLoginEvent.ShowSnackbar(
                        message = BitwardenString
                            .there_was_an_error_starting_web_authn_two_factor_authentication
                            .asText(),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ContinueButtonClick login returns Error should update dialogState`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            authRepository.login(
                email = DEFAULT_EMAIL_ADDRESS,
                password = DEFAULT_PASSWORD,
                twoFactorData = TwoFactorDataModel(
                    code = "",
                    method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                    remember = false,
                ),
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        } returns LoginResult.Error(errorMessage = null, error = error)

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = TwoFactorLoginState.DialogState.Loading(
                        message = BitwardenString.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = TwoFactorLoginState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.invalid_verification_code.asText(),
                        error = error,
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
                orgIdentifier = DEFAULT_ORG_IDENTIFIER,
            )
        }
    }

    @Test
    fun `ContinueButtonClick login returns Error with message should update dialogState`() =
        runTest {
            val error = Throwable("Fail!")
            coEvery {
                authRepository.login(
                    email = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    twoFactorData = TwoFactorDataModel(
                        code = "",
                        method = TwoFactorAuthMethod.AUTHENTICATOR_APP.value.toString(),
                        remember = false,
                    ),
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            } returns LoginResult.Error(errorMessage = "Mock error message", error = error)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())

                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Loading(
                            message = BitwardenString.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = "Mock error message".asText(),
                            error = error,
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
                            message = BitwardenString.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
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
                            message = BitwardenString.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.we_couldnt_verify_the_servers_certificate.asText(),
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
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueButtonClick login returns NewDeviceVerification with message should update dialogState`() =
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
                    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
                )
            } returns LoginResult.NewDeviceVerification(errorMessage = "new device verification required")

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())

                viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Loading(
                            message = BitwardenString.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = "new device verification required".asText(),
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
                isRememberEnabled = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `sendVerificationCodeEmail with isUserInitiated false should not show loading and snackbar on success`() =
        runTest {
            coEvery { authRepository.resendVerificationCodeEmail() } returns ResendEmailResult.Success
            val viewModel = createViewModel()
            // Simulate initial email send (not user initiated)
            viewModel.trySendAction(
                TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                    ResendEmailResult.Success,
                    isUserInitiated = false,
                ),
            )
            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                // No loading dialog
                assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
                // No snackbar
                eventFlow.expectNoEvents()
            }
        }

    @Test
    fun `ResendEmailClick returns success should emit ShowSnackbar`() = runTest {
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
                TwoFactorLoginEvent.ShowSnackbar(
                    message = BitwardenString.verification_email_sent.asText(),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ResendEmailClick returns error should update dialogState`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            authRepository.resendVerificationCodeEmail()
        } returns ResendEmailResult.Error(message = null, error = error)

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
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.verification_email_not_sent.asText(),
                        error = error,
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.EMAIL,
                    dialogState = TwoFactorLoginState.DialogState.Loading(
                        message = BitwardenString.submitting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    authMethod = TwoFactorAuthMethod.EMAIL,
                    dialogState = TwoFactorLoginState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.verification_email_not_sent.asText(),
                        error = error,
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
    fun `ReceiveResendEmailResult with ResendEmailResult Success and isUserInitiated true should ShowSnackbar`() =
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
                    TwoFactorLoginEvent.ShowSnackbar(
                        message = BitwardenString.verification_email_sent.asText(),
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
            val error = Throwable("Fail!")
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                        resendEmailResult = ResendEmailResult.Error(message = null, error = error),
                        isUserInitiated = true,
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.verification_email_not_sent.asText(),
                            error = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `when AuthRepository newDeviceOtp is true ContinueButtonClick should call the login method with newDeviceOtp`() {
        val code = " code"
        val initialState = DEFAULT_STATE.copy(
            authMethod = TwoFactorAuthMethod.EMAIL,
            codeInput = code,
            isNewDeviceVerification = true,
        )
        val localAuthRepository: AuthRepository = mockk {
            every { twoFactorResponse } returns TWO_FACTOR_RESPONSE
            every { duoTokenResultFlow } returns mutableDuoTokenResultFlow
            every { yubiKeyResultFlow } returns mutableYubiKeyResultFlow
            every { webAuthResultFlow } returns mutableWebAuthResultFlow
            coEvery {
                login(
                    email = any(),
                    password = any(),
                    newDeviceOtp = any(),
                    orgIdentifier = any(),
                )
            } returns LoginResult.Success
        }

        val localViewModel =
            TwoFactorLoginViewModel(
                authRepository = localAuthRepository,
                environmentRepository = environmentRepository,
                resourceManager = resourceManager,
                savedStateHandle = SavedStateHandle().also {
                    it["state"] = initialState
                },
            )

        localViewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
        assertEquals(
            initialState.copy(
                password = DEFAULT_PASSWORD,
                email = DEFAULT_EMAIL_ADDRESS,
                isContinueButtonEnabled = false,
            ),
            localViewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            localAuthRepository.login(
                email = DEFAULT_STATE.email,
                password = DEFAULT_STATE.password,
                newDeviceOtp = code.trim(),
                orgIdentifier = DEFAULT_STATE.orgIdentifier,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ReceiveResendEmailResult with newDeviceVerification true should call ResendNewDeviceOtp`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.EMAIL,
                isNewDeviceVerification = true,
            )
            val viewModel = createViewModel(state = initialState)
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    TwoFactorLoginAction.ResendEmailClick,
                )
                awaitItem()

                coVerify(exactly = 1) {
                    authRepository.resendNewDeviceOtp()
                }
                coVerify(exactly = 0) {
                    authRepository.resendVerificationCodeEmail()
                }
            }
        }

    @Test
    fun `ReceiveResendEmailResult with ResendEmailResult Success should ShowSnackbar`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                authMethod = TwoFactorAuthMethod.EMAIL,
                isNewDeviceVerification = true,
            )
            val viewModel = createViewModel(state = initialState)
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                        resendEmailResult = ResendEmailResult.Success,
                        isUserInitiated = true,
                    ),
                )
                assertEquals(
                    TwoFactorLoginEvent.ShowSnackbar(
                        message = BitwardenString.verification_email_sent.asText(),
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
            savedStateHandle = SavedStateHandle().apply {
                set(key = "state", value = state)
                every { toTwoFactorLoginArgs() } returns TwoFactorLoginArgs(
                    emailAddress = DEFAULT_EMAIL_ADDRESS,
                    password = DEFAULT_PASSWORD,
                    orgIdentifier = DEFAULT_ENCODED_ORG_IDENTIFIER,
                    isNewDeviceVerification = false,
                )
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
    ssoToken = null,
    twoFactorProviders = null,
)
private const val DEFAULT_EMAIL_ADDRESS = "example@email.com"
private const val DEFAULT_ORG_IDENTIFIER = "org_identifier"
private const val DEFAULT_ENCODED_ORG_IDENTIFIER = "org_identifier"
private const val DEFAULT_PASSWORD = "password123"
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
    isRememberEnabled = false,
    email = DEFAULT_EMAIL_ADDRESS,
    password = DEFAULT_PASSWORD,
    orgIdentifier = DEFAULT_ORG_IDENTIFIER,
    isNewDeviceVerification = false,
)
