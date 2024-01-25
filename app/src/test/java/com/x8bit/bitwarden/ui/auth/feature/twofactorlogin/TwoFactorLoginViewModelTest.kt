package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TwoFactorLoginViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { twoFactorData } returns TWO_FACTOR_DATA
    }
    private val savedStateHandle = SavedStateHandle().also {
        it["email_address"] = "test@gmail.com"
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
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
        private val TWO_FACTOR_DATA =
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
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
        )
    }
}
