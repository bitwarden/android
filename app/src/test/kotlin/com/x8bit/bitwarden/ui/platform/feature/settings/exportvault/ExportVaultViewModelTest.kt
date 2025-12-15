package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class ExportVaultViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
        } returns emptyList()
        every {
            getActivePolicies(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns emptyList()
    }

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val vaultRepository: VaultRepository = mockk {
        coEvery {
            exportVaultDataToString(
                format = any(),
                restrictedTypes = emptyList(),
            )
        } returns ExportVaultDataResult.Success("data")
        coEvery {
            exportVaultDataToString(
                format = any(),
                restrictedTypes = listOf(CipherType.CARD),
            )
        } returns ExportVaultDataResult.Success("data")
    }
    private val fileManager: FileManager = mockk()
    private val organizationEventManager = mockk<OrganizationEventManager> {
        every { trackEvent(event = any()) } just runs
    }

    @Test
    fun `initial state should be correct`() = runTest {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
        } returns listOf(createMockPolicy())

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    policyPreventsExport = true,
                ),
                awaitItem(),
            )
        }
        verify { authRepository.userStateFlow }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.CloseButtonClick)
            assertEquals(
                ExportVaultEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked correct password should call exportVaultDataToString`() {
        val password = "password"
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = true)

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)

        coVerify {
            vaultRepository.exportVaultDataToString(any(), emptyList())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked correct password should call exportVaultDataToString with restricted item types when policy`() {
        val password = "password"
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = true)
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns listOf(createMockPolicy(isEnabled = true))

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)

        coVerify {
            vaultRepository.exportVaultDataToString(
                format = any(),
                restrictedTypes = listOf(CipherType.CARD),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked correct password should call exportVaultDataToString without restricted item types when policy is disabled`() {
        val password = "password"
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = true)

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)

        coVerify {
            vaultRepository.exportVaultDataToString(
                format = any(),
                restrictedTypes = listOf(),
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked with verified code should call exportVaultDataToString`() {
        val passcode = "1234"
        val initialState = DEFAULT_STATE.copy(
            passwordInput = passcode,
            showSendCodeButton = true,
        )
        coEvery {
            authRepository.verifyOneTimePasscode(
                oneTimePasscode = passcode,
            )
        } returns VerifyOtpResult.Verified

        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)

        assertEquals(
            initialState.copy(
                exportData = "data",
                passwordInput = "",
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.exportVaultDataToString(any(), emptyList())
        }
    }

    @Test
    fun `ConfirmExportVaultClicked with invalid code should call exportVaultDataToString`() {
        val passcode = "1234"
        val initialState = DEFAULT_STATE.copy(
            passwordInput = passcode,
            showSendCodeButton = true,
        )
        val error = Throwable("Fail!")
        coEvery {
            authRepository.verifyOneTimePasscode(
                oneTimePasscode = passcode,
            )
        } returns VerifyOtpResult.NotVerified(errorMessage = "Wrong", error = error)

        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)

        assertEquals(
            initialState.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    error = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 0) {
            vaultRepository.exportVaultDataToString(any(), emptyList())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked should show success with valid input when export type is JSON_ENCRYPTED`() {
        val filePassword = "filePassword"
        val password = "password"
        val initialState = DEFAULT_STATE.copy(
            confirmFilePasswordInput = filePassword,
            exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
            filePasswordInput = filePassword,
            passwordInput = password,
            passwordStrengthState = PasswordStrengthState.STRONG,
        )
        coEvery {
            authRepository.getPasswordStrength(
                password = password,
            )
        } returns PasswordStrengthResult.Success(
            passwordStrength = PasswordStrength.LEVEL_4,
        )
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = true)
        val viewModel = createViewModel(
            initialState = initialState,
        )
        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            initialState.copy(
                confirmFilePasswordInput = "",
                exportData = "data",
                filePasswordInput = "",
                passwordInput = "",
                passwordStrengthState = PasswordStrengthState.NONE,
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            authRepository.validatePassword(password)
            vaultRepository.exportVaultDataToString(
                format = ExportFormat.EncryptedJson(filePassword),
                restrictedTypes = emptyList(),
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked blank password should show an error`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required.asText(
                        BitwardenString.master_password.asText(),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(ExportVaultAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked blank file password should show an error when export type is JSON_ENCRYPTED`() {
        val password = "password"
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ExportVaultAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
        )
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))
        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required.asText(
                        BitwardenString.file_password.asText(),
                    ),
                ),
                exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(ExportVaultAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE.copy(
                exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked blank confirm file password should show an error when export type is JSON_ENCRYPTED`() {
        val password = "password"
        coEvery {
            authRepository.getPasswordStrength(
                password = password,
            )
        } returns PasswordStrengthResult.Success(
            passwordStrength = PasswordStrength.LEVEL_4,
        )
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ExportVaultAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
        )
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))
        viewModel.trySendAction(ExportVaultAction.FilePasswordInputChange(password))
        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required.asText(
                        BitwardenString.confirm_file_password.asText(),
                    ),
                ),
                exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
                filePasswordInput = password,
                passwordInput = password,
                passwordStrengthState = PasswordStrengthState.STRONG,
            ),
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(ExportVaultAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE.copy(
                exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
                filePasswordInput = password,
                passwordInput = password,
                passwordStrengthState = PasswordStrengthState.STRONG,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmExportVaultClicked non-matching file passwords should show an error when export type is JSON_ENCRYPTED`() {
        val password = "password"
        val initialState = DEFAULT_STATE.copy(
            confirmFilePasswordInput = "random",
            exportFormat = ExportVaultFormat.JSON_ENCRYPTED,
            filePasswordInput = password,
            passwordInput = password,
            passwordStrengthState = PasswordStrengthState.STRONG,
        )
        coEvery {
            authRepository.getPasswordStrength(
                password = password,
            )
        } returns PasswordStrengthResult.Success(
            passwordStrength = PasswordStrength.LEVEL_4,
        )
        val viewModel = createViewModel(
            initialState = initialState,
        )
        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            initialState.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.master_password_confirmation_val_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ConfirmExportVaultClicked invalid password should show an error`() {
        val password = "password"
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = false)

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.invalid_master_password.asText(),
                ),
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            authRepository.validatePassword(
                password = password,
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked error checking password should show an error`() {
        val password = "password"
        val error = Throwable("Fail!")
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Error(error = error)

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

        viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    error = error,
                ),
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ExportFormatOptionSelect should update the selected export format in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ExportVaultAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                exportFormat = ExportVaultFormat.CSV,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ConfirmFilePasswordInputChanged should update the confirm password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.ConfirmFilePasswordInputChange("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                confirmFilePasswordInput = "Test123",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `FilePasswordInputChanged should update the file password input in the state`() {
        val password = "Test123"
        coEvery {
            authRepository.getPasswordStrength(
                password = password,
            )
        } returns PasswordStrengthResult.Success(
            passwordStrength = PasswordStrength.LEVEL_4,
        )
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.FilePasswordInputChange(password))

        assertEquals(
            DEFAULT_STATE.copy(
                filePasswordInput = password,
                passwordStrengthState = PasswordStrengthState.STRONG,
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            authRepository.getPasswordStrength(
                password = password,
            )
        }
    }

    @Test
    fun `PasswordInputChanged should update the password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportVaultAction.PasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                passwordInput = "Test123",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SendCodeClick should call requestOneTimePasscode and update dialog state to sending then back to null when request completes and send correct event on success`() =
        runTest {
            val viewModel = createViewModel()
            coEvery { authRepository.requestOneTimePasscode() } returns RequestOtpResult.Success
            viewModel.stateEventFlow(backgroundScope) { stateTurbine, eventTurbine ->
                assertEquals(DEFAULT_STATE, stateTurbine.awaitItem())
                viewModel.trySendAction(ExportVaultAction.SendCodeClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = ExportVaultState.DialogState.Loading(
                            message = BitwardenString.sending.asText(),
                        ),
                    ),
                    stateTurbine.awaitItem(),
                )
                assertEquals(DEFAULT_STATE, stateTurbine.awaitItem())
                assertEquals(
                    ExportVaultEvent.ShowSnackbar(message = BitwardenString.code_sent.asText()),
                    eventTurbine.awaitItem(),
                )
            }
            coVerify { authRepository.requestOneTimePasscode() }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SendCodeClick should call requestOneTimePasscode and update dialog state to sending then back to null when request completes and send correct event on error`() =
        runTest {
            val error = Throwable("Fail!")
            val viewModel = createViewModel()
            coEvery {
                authRepository.requestOneTimePasscode()
            } returns RequestOtpResult.Error(message = null, error = error)
            viewModel.stateEventFlow(backgroundScope) { stateTurbine, eventTurbine ->
                assertEquals(DEFAULT_STATE, stateTurbine.awaitItem())
                viewModel.trySendAction(ExportVaultAction.SendCodeClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = ExportVaultState.DialogState.Loading(
                            message = BitwardenString.sending.asText(),
                        ),
                    ),
                    stateTurbine.awaitItem(),
                )
                assertEquals(DEFAULT_STATE, stateTurbine.awaitItem())
                assertEquals(
                    ExportVaultEvent.ShowSnackbar(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                    eventTurbine.awaitItem(),
                )
            }
            coVerify { authRepository.requestOneTimePasscode() }
        }

    @Test
    fun `ReceiveExportVaultDataToStringResult should update state to error if result is error`() {
        val viewModel = createViewModel()
        val error = Throwable("Fail")
        viewModel.trySendAction(
            ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult(
                result = ExportVaultDataResult.Error(error = error),
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.export_vault_failure.asText(),
                    error = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ReceiveExportVaultDataToStringResult should emit NavigateToSelectExportDataLocation on result success`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult(
                        result = ExportVaultDataResult.Success(vaultData = "TestVaultData"),
                    ),
                )

                assertEquals(
                    ExportVaultEvent.NavigateToSelectExportDataLocation(
                        fileName = "bitwarden_export_20231027120000.json",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ReceivePasswordStrengthResult should update password strength state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.NONE,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceivePasswordStrengthResult(
                    PasswordStrengthResult.Success(
                        PasswordStrength.LEVEL_0,
                    ),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceivePasswordStrengthResult(
                    PasswordStrengthResult.Success(
                        PasswordStrength.LEVEL_1,
                    ),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_2,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceivePasswordStrengthResult(
                    PasswordStrengthResult.Success(
                        PasswordStrength.LEVEL_2,
                    ),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.WEAK_3,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceivePasswordStrengthResult(
                    PasswordStrengthResult.Success(
                        PasswordStrength.LEVEL_3,
                    ),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    passwordStrengthState = PasswordStrengthState.GOOD,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceivePasswordStrengthResult(
                    PasswordStrengthResult.Success(
                        PasswordStrength.LEVEL_4,
                    ),
                ),
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
    fun `ExportLocationReceive should update state to error if exportData is null`() {
        val viewModel = createViewModel()
        val uri = mockk<Uri>()

        viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(fileUri = uri))

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.export_vault_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ExportLocationReceive should update state to error if saving the data fails`() {
        val exportData = "TestExportVaultData"
        val viewModel = createViewModel(
            DEFAULT_STATE.copy(
                exportData = exportData,
            ),
        )
        val uri = mockk<Uri>()
        coEvery {
            fileManager.stringToUri(fileUri = any(), dataString = exportData)
        } returns false

        viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(fileUri = uri))

        coVerify {
            fileManager.stringToUri(fileUri = any(), dataString = exportData)
        }

        assertEquals(
            DEFAULT_STATE.copy(
                exportData = exportData,
                dialogState = ExportVaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.export_vault_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ExportLocationReceive should emit ShowSnackbar and UserClientExportedVault on success`() =
        runTest {
            val exportData = "TestExportVaultData"
            val viewModel = createViewModel(
                DEFAULT_STATE.copy(
                    exportData = exportData,
                ),
            )
            val uri = mockk<Uri>()
            coEvery {
                fileManager.stringToUri(
                    fileUri = any(),
                    dataString = exportData,
                )
            } returns true

            viewModel.eventFlow.test {
                viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(uri))

                coVerify { fileManager.stringToUri(fileUri = any(), dataString = exportData) }

                verify(exactly = 1) {
                    organizationEventManager.trackEvent(
                        event = OrganizationEvent.UserClientExportedVault,
                    )
                }

                assertEquals(
                    ExportVaultEvent.ShowSnackbar(BitwardenString.export_vault_success.asText()),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(
        initialState: ExportVaultState? = null,
    ): ExportVaultViewModel = ExportVaultViewModel(
        authRepository = authRepository,
        policyManager = policyManager,
        savedStateHandle = SavedStateHandle(
            initialState = mapOf("state" to initialState),
        ),
        fileManager = fileManager,
        vaultRepository = vaultRepository,
        clock = clock,
        organizationEventManager = organizationEventManager,
    )
}

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "email",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
            isExportable = true,
        ),
    ),
)

private val DEFAULT_STATE = ExportVaultState(
    confirmFilePasswordInput = "",
    dialogState = null,
    exportFormat = ExportVaultFormat.JSON,
    filePasswordInput = "",
    passwordInput = "",
    exportData = null,
    passwordStrengthState = PasswordStrengthState.NONE,
    policyPreventsExport = false,
    showSendCodeButton = false,
)
