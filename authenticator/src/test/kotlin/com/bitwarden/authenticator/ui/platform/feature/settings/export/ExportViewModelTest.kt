package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExportViewModelTest : BaseViewModelTest() {

    private val authenticatorRepository: AuthenticatorRepository = mockk()

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
