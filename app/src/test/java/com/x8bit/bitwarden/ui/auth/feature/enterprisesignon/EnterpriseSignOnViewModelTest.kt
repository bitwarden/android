package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForSso
import com.x8bit.bitwarden.data.platform.manager.util.FakeNetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnterpriseSignOnViewModelTest : BaseViewModelTest() {

    private val mutableSsoCallbackResultFlow = bufferedMutableSharedFlow<SsoCallbackResult>()
    private val authRepository: AuthRepository = mockk {
        every { ssoCallbackResultFlow } returns mutableSsoCallbackResultFlow
    }

    private val environmentRepository: EnvironmentRepository = FakeEnvironmentRepository()

    private val generatorRepository: GeneratorRepository = FakeGeneratorRepository()

    private val savedStateHandle = SavedStateHandle()

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
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.CloseButtonClick)
            assertEquals(
                EnterpriseSignOnEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with valid organization and failed prevalidation should emit ShowToast, show a loading dialog, and then show an error`() =
        runTest {
            val organizationId = "Test"
            val state = DEFAULT_STATE.copy(orgIdentifierInput = organizationId)

            coEvery {
                authRepository.prevalidateSso(organizationId)
            } returns PrevalidateSsoResult.Failure

            val viewModel = createViewModel(state)
            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)

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
            viewModel.eventFlow.test {
                assertEquals(
                    EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with valid organization and successful prevalidation should emit ShowToast, show a loading dialog, hide a loading dialog, and then emit NavigateToSsoLogin`() =
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
                viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)

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
                    EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                    awaitItem(),
                )
                assertEquals(
                    EnterpriseSignOnEvent.NavigateToSsoLogin(ssoUri),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with invalid organization should emit ShowToast and show error dialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)
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
                assertEquals(
                    EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with no Internet should emit ShowToast and show error dialog`() = runTest {
        val viewModel = createViewModel(isNetworkConnected = false)
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            assertEquals(
                EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OrgIdentifierInputChange should update organization identifier`() = runTest {
        val input = "input"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.OrgIdentifierInputChange(input))
            assertEquals(
                DEFAULT_STATE.copy(orgIdentifierInput = input),
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Error`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = EnterpriseSignOnState.DialogState.Error(
                message = "Error".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Loading`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = EnterpriseSignOnState.DialogState.Loading(
                message = "Loading".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: EnterpriseSignOnState? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            initialState = mapOf("state" to initialState),
        ),
        isNetworkConnected: Boolean = true,
    ): EnterpriseSignOnViewModel = EnterpriseSignOnViewModel(
        authRepository = authRepository,
        environmentRepository = environmentRepository,
        generatorRepository = generatorRepository,
        networkConnectionManager = FakeNetworkConnectionManager(isNetworkConnected),
        savedStateHandle = savedStateHandle,
    )

    companion object {
        private val DEFAULT_STATE = EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
        )
    }
}
