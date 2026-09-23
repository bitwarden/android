package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.indicator.PasswordStrengthState
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExportViewModelTest : BaseViewModelTest() {

    private val authenticatorRepository: AuthenticatorRepository = mockk()

    @BeforeEach
    fun setup() {
        // Strength scoring is asserted in its own tests; elsewhere it must not perturb state.
        coEvery {
            authenticatorRepository.getPasswordStrength(any())
        } returns IllegalStateException("not stubbed").asFailure()
    }

    @Test
    fun `initial state should have null dialogState and JSON format`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            expectNoEvents()
            viewModel.trySendAction(ExportAction.CloseButtonClick)
            assertEquals(ExportEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ExportFormatOptionSelect with JSON should update state to JSON format`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            // First switch to CSV, then back to JSON to test a meaningful state change
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
            )
            assertEquals(
                DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.CSV),
                awaitItem(),
            )

            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON),
            )
            assertEquals(
                DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.JSON),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ExportFormatOptionSelect with CSV should update state to CSV format`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
            )
            assertEquals(
                DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.CSV),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmExportClick should emit NavigateToSelectExportDestination with JSON filename`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                expectNoEvents()
                viewModel.trySendAction(ExportAction.ConfirmExportClick)
                assertEquals(
                    ExportEvent.NavigateToSelectExportDestination(
                        fileName = "authenticator_export_20241027123045.json",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ConfirmExportClick should emit NavigateToSelectExportDestination with CSV filename`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
            )
            viewModel.eventFlow.test {
                expectNoEvents()
                viewModel.trySendAction(ExportAction.ConfirmExportClick)
                assertEquals(
                    ExportEvent.NavigateToSelectExportDestination(
                        fileName = "authenticator_export_20241027123045.csv",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DialogDismiss when error dialog shown should clear dialogState`() = runTest {
        val mockUri: Uri = mockk()
        coEvery {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        } returns ExportDataResult.Error

        val viewModel = createViewModel()

        // First, trigger loading dialog via ExportLocationReceive
        viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))

        viewModel.stateFlow.test {
            // Should be in error state now
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.export_vault_failure.asText(),
                    ),
                ),
                awaitItem(),
            )

            // Dismiss the dialog
            viewModel.trySendAction(ExportAction.DialogDismiss)
            assertEquals(
                DEFAULT_STATE.copy(dialogState = null),
                awaitItem(),
            )
        }

        coVerify {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        }
    }

    @Test
    fun `ExportLocationReceive should show loading dialog`() = runTest {
        val mockUri: Uri = mockk()
        coEvery {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        } returns ExportDataResult.Success

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))

            // First state change should be loading dialog
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportState.DialogState.Loading(),
                ),
                awaitItem(),
            )

            // Then success clears dialog
            assertEquals(
                DEFAULT_STATE.copy(dialogState = null),
                awaitItem(),
            )
        }

        coVerify {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        }
    }

    @Test
    fun `ExportLocationReceive with successful export should clear dialog and show snackbar`() =
        runTest {
            val mockUri: Uri = mockk()
            coEvery {
                authenticatorRepository.exportVaultData(
                    format = ExportVaultFormat.JSON,
                    fileUri = mockUri,
                )
            } returns ExportDataResult.Success

            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                expectNoEvents()
                viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))
                assertEquals(
                    ExportEvent.ShowSnackBar(BitwardenString.export_success.asText()),
                    awaitItem(),
                )
            }

            coVerify {
                authenticatorRepository.exportVaultData(
                    format = ExportVaultFormat.JSON,
                    fileUri = mockUri,
                )
            }
        }

    @Test
    fun `ExportLocationReceive with error should show error dialog`() = runTest {
        val mockUri: Uri = mockk()
        coEvery {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        } returns ExportDataResult.Error

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))

            // Loading dialog
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportState.DialogState.Loading(),
                ),
                awaitItem(),
            )

            // Error dialog
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.export_vault_failure.asText(),
                    ),
                ),
                awaitItem(),
            )
        }

        coVerify {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.JSON,
                fileUri = mockUri,
            )
        }
    }

    @Test
    fun `ExportLocationReceive with CSV format should call repository with CSV format`() = runTest {
        val mockUri: Uri = mockk()
        coEvery {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.CSV,
                fileUri = mockUri,
            )
        } returns ExportDataResult.Success

        val viewModel = createViewModel()

        // Set CSV format first
        viewModel.trySendAction(
            ExportAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
        )

        viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))

        coVerify {
            authenticatorRepository.exportVaultData(
                format = ExportVaultFormat.CSV,
                fileUri = mockUri,
            )
        }
    }

    @Test
    fun `FilePasswordInputChange and ConfirmFilePasswordInputChange should update state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())

                viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
                assertEquals(DEFAULT_STATE.copy(filePasswordInput = "pass"), awaitItem())

                viewModel.trySendAction(ExportAction.ConfirmFilePasswordInputChange("pass"))
                assertEquals(
                    DEFAULT_STATE.copy(
                        filePasswordInput = "pass",
                        confirmFilePasswordInput = "pass",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ExportFormatOptionSelect should clear the file password inputs`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
        viewModel.trySendAction(ExportAction.ConfirmFilePasswordInputChange("pass"))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    filePasswordInput = "pass",
                    confirmFilePasswordInput = "pass",
                ),
                awaitItem(),
            )
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
            )
            assertEquals(
                DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.JSON_ENCRYPTED),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmExportClick with encrypted format and blank password should show error`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
            )

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.JSON_ENCRYPTED),
                    awaitItem(),
                )
                viewModel.trySendAction(ExportAction.ConfirmExportClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        exportVaultFormat = ExportVaultFormat.JSON_ENCRYPTED,
                        dialogState = ExportState.DialogState.Error(
                            message = BitwardenString.validation_field_required
                                .asText(BitwardenString.file_password.asText()),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ConfirmExportClick with encrypted format and blank confirm should show error`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
        )
        viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))

        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(ExportAction.ConfirmExportClick)
            assertEquals(
                BitwardenString.validation_field_required
                    .asText(BitwardenString.confirm_file_password.asText()),
                (awaitItem().dialogState as ExportState.DialogState.Error).message,
            )
        }
    }

    @Test
    fun `ConfirmExportClick with encrypted format and mismatched password should show error`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
            )
            viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
            viewModel.trySendAction(ExportAction.ConfirmFilePasswordInputChange("different"))

            viewModel.stateFlow.test {
                awaitItem()
                viewModel.trySendAction(ExportAction.ConfirmExportClick)
                assertEquals(
                    BitwardenString.master_password_confirmation_val_message.asText(),
                    (awaitItem().dialogState as ExportState.DialogState.Error).message,
                )
            }
        }

    @Test
    fun `ConfirmExportClick with encrypted format and matching password should navigate`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
            )
            viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
            viewModel.trySendAction(ExportAction.ConfirmFilePasswordInputChange("pass"))

            viewModel.eventFlow.test {
                expectNoEvents()
                viewModel.trySendAction(ExportAction.ConfirmExportClick)
                assertEquals(
                    ExportEvent.NavigateToSelectExportDestination(
                        fileName = "authenticator_export_20241027123045.json",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ExportLocationReceive with encrypted format should pass the password to the repository`() =
        runTest {
            val mockUri: Uri = mockk()
            coEvery {
                authenticatorRepository.exportVaultData(
                    format = ExportVaultFormat.JSON_ENCRYPTED,
                    fileUri = mockUri,
                    password = "pass",
                )
            } returns ExportDataResult.Success

            val viewModel = createViewModel()
            viewModel.trySendAction(
                ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
            )
            viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
            viewModel.trySendAction(ExportAction.ConfirmFilePasswordInputChange("pass"))

            viewModel.trySendAction(ExportAction.ExportLocationReceive(mockUri))

            coVerify {
                authenticatorRepository.exportVaultData(
                    format = ExportVaultFormat.JSON_ENCRYPTED,
                    fileUri = mockUri,
                    password = "pass",
                )
            }
            assertEquals("", viewModel.stateFlow.value.filePasswordInput)
            assertEquals("", viewModel.stateFlow.value.confirmFilePasswordInput)
        }

    @Test
    fun `FilePasswordInputChange should map the score to a password strength state`() = runTest {
        coEvery {
            authenticatorRepository.getPasswordStrength("pass")
        } returns 3.toUByte().asSuccess()

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
            assertEquals(DEFAULT_STATE.copy(filePasswordInput = "pass"), awaitItem())
            assertEquals(
                DEFAULT_STATE.copy(
                    filePasswordInput = "pass",
                    passwordStrengthState = PasswordStrengthState.GOOD,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `FilePasswordInputChange with a blank password should reset the strength state`() =
        runTest {
            coEvery {
                authenticatorRepository.getPasswordStrength("pass")
            } returns 4.toUByte().asSuccess()

            val viewModel = createViewModel()
            viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
            assertEquals(
                PasswordStrengthState.STRONG,
                viewModel.stateFlow.value.passwordStrengthState,
            )

            viewModel.trySendAction(ExportAction.FilePasswordInputChange(""))
            assertEquals(
                PasswordStrengthState.NONE,
                viewModel.stateFlow.value.passwordStrengthState,
            )
            coVerify(exactly = 0) { authenticatorRepository.getPasswordStrength("") }
        }

    @Test
    fun `FilePasswordInputChange should ignore a score for a stale password`() = runTest {
        coEvery {
            authenticatorRepository.getPasswordStrength("stale")
        } returns 4.toUByte().asSuccess()

        val viewModel = createViewModel()
        // The default stub fails, so "current" leaves the strength at NONE.
        viewModel.trySendAction(ExportAction.FilePasswordInputChange("current"))
        viewModel.trySendAction(
            ExportAction.Internal.ReceivePasswordStrengthResult(
                password = "stale",
                result = 4.toUByte().asSuccess(),
            ),
        )

        assertEquals(
            PasswordStrengthState.NONE,
            viewModel.stateFlow.value.passwordStrengthState,
        )
    }

    @Test
    fun `FilePasswordInputChange should leave the strength alone when scoring fails`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))

        assertEquals(
            DEFAULT_STATE.copy(filePasswordInput = "pass"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ExportFormatOptionSelect should reset the strength state`() = runTest {
        coEvery {
            authenticatorRepository.getPasswordStrength("pass")
        } returns 4.toUByte().asSuccess()

        val viewModel = createViewModel()
        viewModel.trySendAction(ExportAction.FilePasswordInputChange("pass"))
        viewModel.trySendAction(
            ExportAction.ExportFormatOptionSelect(ExportVaultFormat.JSON_ENCRYPTED),
        )

        assertEquals(
            DEFAULT_STATE.copy(exportVaultFormat = ExportVaultFormat.JSON_ENCRYPTED),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(): ExportViewModel = ExportViewModel(
        authenticatorRepository = authenticatorRepository,
        clock = FIXED_CLOCK,
    )
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2024-10-27T12:30:45Z"),
    ZoneOffset.UTC,
)

private val DEFAULT_STATE = ExportState(
    exportData = null,
    dialogState = null,
    exportVaultFormat = ExportVaultFormat.JSON,
)
