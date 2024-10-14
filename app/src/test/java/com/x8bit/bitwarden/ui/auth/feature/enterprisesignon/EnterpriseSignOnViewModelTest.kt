package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.OrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForSso
import com.x8bit.bitwarden.data.platform.manager.util.FakeNetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class EnterpriseSignOnViewModelTest : BaseViewModelTest() {

    private val mutableSsoCallbackResultFlow = bufferedMutableSharedFlow<SsoCallbackResult>()
    private val mutableCaptchaTokenResultFlow =
        bufferedMutableSharedFlow<CaptchaCallbackTokenResult>()
    private val authRepository: AuthRepository = mockk {
        every { ssoCallbackResultFlow } returns mutableSsoCallbackResultFlow
        every { captchaTokenResultFlow } returns mutableCaptchaTokenResultFlow
        every { rememberedOrgIdentifier } returns null
        coEvery {
            getOrganizationDomainSsoDetails(any())
        } just awaits
    }

    private val environmentRepository: EnvironmentRepository = FakeEnvironmentRepository()

    private val generatorRepository: GeneratorRepository = FakeGeneratorRepository()

    @BeforeEach
    fun setUp() {
        mockkStatic(::generateUriForSso)
        mockkStatic(Uri::parse)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::generateUriForSso)
        unmockkStatic(Uri::parse)
    }

    @Test
    fun `initial state should be correct when not pulling from handle`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            orgIdentifierInput = "test",
        )
        val viewModel = createViewModel(expectedState)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(EnterpriseSignOnAction.CloseButtonClick)
            assertEquals(
                EnterpriseSignOnEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with valid organization and failed prevalidation should show a loading dialog, and then show an error`() =
        runTest {
            val organizationId = "Test"
            val state = DEFAULT_STATE.copy(orgIdentifierInput = organizationId)

            coEvery {
                authRepository.prevalidateSso(organizationId)
            } returns PrevalidateSsoResult.Failure

            val viewModel = createViewModel(state)
            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.trySendAction(EnterpriseSignOnAction.LogInClick)

                assertEquals(
                    state.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    state.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
                            message = R.string.login_sso_error.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with valid organization and successful prevalidation should show a loading dialog, hide a loading dialog, and then emit NavigateToSsoLogin`() =
        runTest {
            val organizationId = "Test"
            val state = DEFAULT_STATE.copy(orgIdentifierInput = organizationId)

            coEvery {
                authRepository.prevalidateSso(organizationId)
            } returns PrevalidateSsoResult.Success(token = "token")

            val ssoUri: Uri = mockk()
            every {
                generateUriForSso(any(), any(), any(), any(), any())
            } returns "https://identity.bitwarden.com/sso-test"
            every {
                Uri.parse("https://identity.bitwarden.com/sso-test")
            } returns ssoUri

            val viewModel = createViewModel(state)
            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.trySendAction(EnterpriseSignOnAction.LogInClick)

                assertEquals(
                    state.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    state.copy(dialogState = null),
                    awaitItem(),
                )
            }
            viewModel.eventFlow.test {
                assertEquals(
                    EnterpriseSignOnEvent.NavigateToSsoLogin(ssoUri),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `LogInClick with invalid organization should show error dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(EnterpriseSignOnAction.LogInClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        message = R.string.validation_field_required.asText(
                            R.string.org_identifier.asText(),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `LogInClick with no Internet should show error dialog`() = runTest {
        val viewModel = createViewModel(isNetworkConnected = false)
        viewModel.trySendAction(EnterpriseSignOnAction.LogInClick)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    title = R.string.internet_connection_required_title.asText(),
                    message = R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `OrgIdentifierInputChange should update organization identifier`() = runTest {
        val input = "input"
        val viewModel = createViewModel()
        viewModel.trySendAction(EnterpriseSignOnAction.OrgIdentifierInputChange(input))
        assertEquals(
            DEFAULT_STATE.copy(orgIdentifierInput = input),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Error`() = runTest {
        val viewModel = createViewModel(isNetworkConnected = false)
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(EnterpriseSignOnAction.LogInClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Loading`() {
        val viewModel = createViewModel(
            dismissInitialDialog = false,
        )
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(R.string.loading.asText()),
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)

        assertEquals(
            DEFAULT_STATE.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ssoCallbackResultFlow MissingCode should show an error dialog`() {
        val viewModel = createViewModel(
            ssoData = DEFAULT_SSO_DATA,
        )
        mutableSsoCallbackResultFlow.tryEmit(SsoCallbackResult.MissingCode)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = R.string.login_sso_error.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ssoCallbackResultFlow Success with different state should show an error dialog`() {
        val viewModel = createViewModel(
            ssoData = DEFAULT_SSO_DATA,
        )
        val ssoCallbackResult = SsoCallbackResult.Success(state = "xyz", code = "lmn")
        mutableSsoCallbackResultFlow.tryEmit(ssoCallbackResult)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = R.string.login_sso_error.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ssoCallbackResultFlow Success with same state with login Error should show loading dialog then show an error`() =
        runTest {
            val orgIdentifier = "Bitwarden"
            coEvery {
                authRepository.login(any(), any(), any(), any(), any(), any())
            } returns LoginResult.Error(null)

            val viewModel = createViewModel(
                ssoData = DEFAULT_SSO_DATA,
            )
            val ssoCallbackResult = SsoCallbackResult.Success(state = "abc", code = "lmn")

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )

                viewModel.trySendAction(
                    EnterpriseSignOnAction.OrgIdentifierInputChange(orgIdentifier),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        orgIdentifierInput = orgIdentifier,
                    ),
                    awaitItem(),
                )

                mutableSsoCallbackResultFlow.tryEmit(ssoCallbackResult)

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                        orgIdentifierInput = orgIdentifier,
                    ),
                    awaitItem(),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
                            message = R.string.login_sso_error.asText(),
                        ),
                        orgIdentifierInput = orgIdentifier,
                    ),
                    awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = "test@gmail.com",
                    ssoCode = "lmn",
                    ssoCodeVerifier = "def",
                    ssoRedirectUri = "bitwarden://sso-callback",
                    captchaToken = null,
                    organizationIdentifier = orgIdentifier,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ssoCallbackResultFlow Success with same state with login Success should show loading dialog, hide it, and save org identifier`() =
        runTest {
            coEvery {
                authRepository.login(any(), any(), any(), any(), any(), any())
            } returns LoginResult.Success

            coEvery {
                authRepository.rememberedOrgIdentifier = "Bitwarden"
            } just runs

            val initialState = DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden")
            val viewModel = createViewModel(
                initialState = initialState,
                ssoData = DEFAULT_SSO_DATA,
            )
            val ssoCallbackResult = SsoCallbackResult.Success(state = "abc", code = "lmn")

            viewModel.stateFlow.test {
                assertEquals(
                    initialState,
                    awaitItem(),
                )

                mutableSsoCallbackResultFlow.tryEmit(ssoCallbackResult)

                assertEquals(
                    initialState.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                    ),
                    awaitItem(),
                )

                assertEquals(
                    initialState,
                    awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = "test@gmail.com",
                    ssoCode = "lmn",
                    ssoCodeVerifier = "def",
                    ssoRedirectUri = "bitwarden://sso-callback",
                    captchaToken = null,
                    organizationIdentifier = "Bitwarden",
                )
            }
            coVerify(exactly = 1) {
                authRepository.rememberedOrgIdentifier = "Bitwarden"
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ssoCallbackResultFlow Success with same state with login CaptchaRequired should show loading dialog, hide it, and send NavigateToCaptcha event`() =
        runTest {
            coEvery {
                authRepository.login(any(), any(), any(), any(), any(), any())
            } returns LoginResult.CaptchaRequired("captcha")

            val uri: Uri = mockk()
            every {
                generateUriForCaptcha(captchaId = "captcha")
            } returns uri

            val initialState = DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden")
            val viewModel = createViewModel(
                initialState = initialState,
                ssoData = DEFAULT_SSO_DATA,
            )
            val ssoCallbackResult = SsoCallbackResult.Success(state = "abc", code = "lmn")

            turbineScope {
                val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
                val eventFlow = viewModel.eventFlow.testIn(backgroundScope)

                assertEquals(initialState, stateFlow.awaitItem())

                mutableSsoCallbackResultFlow.tryEmit(ssoCallbackResult)

                assertEquals(
                    initialState.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )

                assertEquals(
                    initialState,
                    stateFlow.awaitItem(),
                )

                assertEquals(
                    EnterpriseSignOnEvent.NavigateToCaptcha(uri),
                    eventFlow.awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = "test@gmail.com",
                    ssoCode = "lmn",
                    ssoCodeVerifier = "def",
                    ssoRedirectUri = "bitwarden://sso-callback",
                    captchaToken = null,
                    organizationIdentifier = "Bitwarden",
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ssoCallbackResultFlow Success with same state with login TwoFactorRequired should show loading dialog, hide it, and send NavigateToTwoFactorLogin event`() =
        runTest {
            coEvery {
                authRepository.login(any(), any(), any(), any(), any(), any())
            } returns LoginResult.TwoFactorRequired

            val initialState = DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden")
            val viewModel = createViewModel(
                initialState = initialState,
                ssoData = DEFAULT_SSO_DATA,
            )
            val ssoCallbackResult = SsoCallbackResult.Success(state = "abc", code = "lmn")

            turbineScope {
                val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
                val eventFlow = viewModel.eventFlow.testIn(backgroundScope)

                assertEquals(initialState, stateFlow.awaitItem())

                mutableSsoCallbackResultFlow.tryEmit(ssoCallbackResult)

                assertEquals(
                    initialState.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Loading(
                            R.string.logging_in.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )

                assertEquals(
                    initialState,
                    stateFlow.awaitItem(),
                )

                assertEquals(
                    EnterpriseSignOnEvent.NavigateToTwoFactorLogin("test@gmail.com", "Bitwarden"),
                    eventFlow.awaitItem(),
                )
            }

            coVerify(exactly = 1) {
                authRepository.login(
                    email = "test@gmail.com",
                    ssoCode = "lmn",
                    ssoCodeVerifier = "def",
                    ssoRedirectUri = "bitwarden://sso-callback",
                    captchaToken = null,
                    organizationIdentifier = "Bitwarden",
                )
            }
        }

    @Test
    fun `captchaTokenResultFlow MissingToken should show error dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.MissingToken)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = R.string.log_in_denied.asText(),
                        message = R.string.captcha_failed.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `captchaTokenResultFlow Success should update the state and attempt to login`() = runTest {
        coEvery {
            authRepository.login(any(), any(), any(), any(), any(), any())
        } returns LoginResult.Success

        coEvery {
            authRepository.rememberedOrgIdentifier = "Bitwarden"
        } just runs

        val initialState = DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden")
        val viewModel = createViewModel(
            initialState = initialState,
            ssoData = DEFAULT_SSO_DATA,
            ssoCallbackResult = SsoCallbackResult.Success(
                state = "abc",
                code = "lmn",
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(
                initialState,
                awaitItem(),
            )

            mutableCaptchaTokenResultFlow.tryEmit(CaptchaCallbackTokenResult.Success("token"))

            assertEquals(
                initialState.copy(
                    captchaToken = "token",
                    dialogState = EnterpriseSignOnState.DialogState.Loading(
                        R.string.logging_in.asText(),
                    ),
                ),
                awaitItem(),
            )

            assertEquals(
                initialState.copy(captchaToken = "token"),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `OrganizationDomainSsoDetails failure should make a request, hide the dialog, and update the org input based on the remembered org`() =
        runTest {
            coEvery {
                authRepository.getOrganizationDomainSsoDetails(any())
            } returns OrganizationDomainSsoDetailsResult.Failure

            coEvery {
                authRepository.rememberedOrgIdentifier
            } returns "Bitwarden"

            val viewModel = createViewModel(dismissInitialDialog = false)
            assertEquals(
                DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden"),
                viewModel.stateFlow.value,
            )

            coVerify(exactly = 1) {
                authRepository.getOrganizationDomainSsoDetails(DEFAULT_EMAIL)
                authRepository.rememberedOrgIdentifier
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OrganizationDomainSsoDetails success with no SSO available should make a request, hide the dialog, and update the org input based on the remembered org`() =
        runTest {
            val orgDetails = OrganizationDomainSsoDetailsResult.Success(
                isSsoAvailable = false,
                organizationIdentifier = "Bitwarden without SSO",
            )

            coEvery {
                authRepository.getOrganizationDomainSsoDetails(any())
            } returns orgDetails

            coEvery {
                authRepository.rememberedOrgIdentifier
            } returns "Bitwarden"

            val viewModel = createViewModel(dismissInitialDialog = false)
            assertEquals(
                DEFAULT_STATE.copy(orgIdentifierInput = "Bitwarden"),
                viewModel.stateFlow.value,
            )

            coVerify(exactly = 1) {
                authRepository.getOrganizationDomainSsoDetails(DEFAULT_EMAIL)
                authRepository.rememberedOrgIdentifier
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OrganizationDomainSsoDetails success with blank identifier should make a request, show the error dialog, and update the org input based on the remembered org`() =
        runTest {
            val orgDetails = OrganizationDomainSsoDetailsResult.Success(
                isSsoAvailable = true,
                organizationIdentifier = "",
            )

            coEvery {
                authRepository.getOrganizationDomainSsoDetails(any())
            } returns orgDetails

            coEvery {
                authRepository.rememberedOrgIdentifier
            } returns "Bitwarden"

            val viewModel = createViewModel(dismissInitialDialog = false)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        message = R.string.organization_sso_identifier_required.asText(),
                    ),
                    orgIdentifierInput = "Bitwarden",
                ),
                viewModel.stateFlow.value,
            )

            coVerify(exactly = 1) {
                authRepository.getOrganizationDomainSsoDetails(DEFAULT_EMAIL)
                authRepository.rememberedOrgIdentifier
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `OrganizationDomainSsoDetails success with valid organization should make a request then attempt to login`() =
        runTest {
            val orgDetails = OrganizationDomainSsoDetailsResult.Success(
                isSsoAvailable = true,
                organizationIdentifier = "Bitwarden with SSO",
            )

            coEvery {
                authRepository.getOrganizationDomainSsoDetails(any())
            } returns orgDetails

            // Just hang on this request; login is tested elsewhere
            coEvery {
                authRepository.prevalidateSso(any())
            } just awaits

            val viewModel = createViewModel(dismissInitialDialog = false)
            assertEquals(
                DEFAULT_STATE.copy(
                    orgIdentifierInput = "Bitwarden with SSO",
                    dialogState = EnterpriseSignOnState.DialogState.Loading(
                        message = R.string.logging_in.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            coVerify(exactly = 1) {
                authRepository.getOrganizationDomainSsoDetails(DEFAULT_EMAIL)
            }
        }

    @Suppress("LongParameterList")
    private fun createViewModel(
        initialState: EnterpriseSignOnState? = null,
        ssoData: SsoResponseData? = null,
        ssoCallbackResult: SsoCallbackResult? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            initialState = mapOf(
                "state" to initialState,
                "email_address" to DEFAULT_EMAIL,
                "ssoData" to ssoData,
                "ssoCallbackResult" to ssoCallbackResult,
            ),
        ),
        isNetworkConnected: Boolean = true,
        dismissInitialDialog: Boolean = true,
    ): EnterpriseSignOnViewModel = EnterpriseSignOnViewModel(
        authRepository = authRepository,
        environmentRepository = environmentRepository,
        generatorRepository = generatorRepository,
        networkConnectionManager = FakeNetworkConnectionManager(isNetworkConnected),
        savedStateHandle = savedStateHandle,
    )
        .also {
            if (dismissInitialDialog) {
                // A loading dialog is shown on initialization, so allow tests to automatically
                // dismiss it.
                it.trySendAction(EnterpriseSignOnAction.DialogDismiss)
            }
        }

    companion object {
        private val DEFAULT_STATE = EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
            captchaToken = null,
        )
        private val DEFAULT_SSO_DATA = SsoResponseData(
            state = "abc",
            codeVerifier = "def",
        )
        private const val DEFAULT_EMAIL = "test@gmail.com"
        private const val DEFAULT_ORG_ID = "orgId"
    }
}
