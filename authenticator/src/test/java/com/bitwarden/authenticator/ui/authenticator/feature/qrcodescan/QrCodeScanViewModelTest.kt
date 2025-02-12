package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import android.net.Uri
import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QrCodeScanViewModelTest : BaseViewModelTest() {

    private val authenticatorBridgeManager: AuthenticatorBridgeManager = mockk()
    private val authenticatorRepository: AuthenticatorRepository = mockk {
        every {
            sharedCodesStateFlow
        } returns MutableStateFlow(SharedVerificationCodesState.Success(emptyList()))
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { defaultSaveOption } returns DefaultSaveOption.NONE
    }

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::parse)
        every { Uri.parse(VALID_TOTP_CODE) } returns VALID_TOTP_URI
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Uri::parse)
    }

    @Test
    fun `on SaveToBitwardenClick receive without a pending QR scan should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(false))
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveToBitwardenClick receive with pending QR scan but no save to default should launch save to Bitwarden flow`() =
        runTest {
            val viewModel = createViewModel()
            every {
                authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE)
            } returns true
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(false))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveToBitwardenClick receive with pending QR scan but startAddTotpLoginItemFlow fails should show error dialog`() =
        runTest {
            val viewModel = createViewModel()
            every {
                authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE)
            } returns false
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(false))
            }
            val expectedState =
                DEFAULT_STATE.copy(dialog = QrCodeScanState.DialogState.SaveToBitwardenError)
            assertEquals(expectedState, viewModel.stateFlow.value)
            verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveToBitwardenClick receive with pending QR scan but and save to default should launch save to Bitwarden flow and update SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()
            every {
                settingsRepository.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP
            } just runs
            every {
                authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE)
            } returns true
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(true))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE) }
            verify { settingsRepository.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP }
        }

    @Test
    fun `on SaveLocallyClick receive without a pending QR scan should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(false))
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveLocallyClick receive with pending QR scan but no save to default should emit code to AuthenticatorRepository and navigate back`() =
        runTest {
            val viewModel = createViewModel()
            every {
                authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT)
            } just runs
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(false))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify {
                authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveLocallyClick receive with pending QR scan but and save to default should emit result to AuthenticatorRepository and update SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()
            every {
                settingsRepository.defaultSaveOption = DefaultSaveOption.LOCAL
            } just runs
            every {
                authenticatorRepository.emitTotpCodeResult(
                    TotpCodeResult.TotpCodeScan(VALID_TOTP_CODE),
                )
            } just runs
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(true))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify {
                authenticatorRepository.emitTotpCodeResult(
                    TotpCodeResult.TotpCodeScan(VALID_TOTP_CODE),
                )
            }
            verify { settingsRepository.defaultSaveOption = DefaultSaveOption.LOCAL }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SaveToBitwardenErrorDismiss recieve should clear dialog state`() {
        val viewModel = createViewModel()
        every {
            authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE)
        } returns false
        // Show error dialog:
        viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
        viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(false))
        val expectedState =
            DEFAULT_STATE.copy(dialog = QrCodeScanState.DialogState.SaveToBitwardenError)
        assertEquals(expectedState, viewModel.stateFlow.value)
        verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE) }

        // Clear dialog:
        viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenErrorDismiss)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on QrCodeScanReceive when authenticator sync is not enabled should just save locally`() {
        val viewModel = createViewModel()
        every {
            authenticatorRepository.sharedCodesStateFlow.value
        } returns SharedVerificationCodesState.SyncNotEnabled
        every {
            authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT)
        } just runs
        viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
        verify { authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT) }
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on QrCodeScanReceive when default save option is local should save locally and navigate back`() =
        runTest {
            val viewModel = createViewModel()
            every { settingsRepository.defaultSaveOption } returns DefaultSaveOption.LOCAL
            every {
                authenticatorRepository.sharedCodesStateFlow.value
            } returns SharedVerificationCodesState.Success(emptyList())
            every {
                authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT)
            } just runs
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify { authenticatorRepository.emitTotpCodeResult(VALID_TOTP_RESULT) }
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on QrCodeScanReceive when default save option is bitwarden should start navigate to Bitwarden flow`() =
        runTest {
            val viewModel = createViewModel()
            every { settingsRepository.defaultSaveOption } returns DefaultSaveOption.BITWARDEN_APP
            every {
                authenticatorRepository.sharedCodesStateFlow.value
            } returns SharedVerificationCodesState.Success(emptyList())
            every {
                authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE)
            } returns true
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(VALID_TOTP_CODE))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(VALID_TOTP_CODE) }
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on QrCodeScanReceive when code is invalid should emit result and navigate back`() =
        runTest {
            val viewModel = createViewModel()
            every {
                authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError)
            } just runs
            val invalidUri: Uri = mockk {
                every { getQueryParameter("secret") } returns "SECRET"
                every { queryParameterNames } returns setOf("digits")
                every { getQueryParameter("digits") } returns "100"
            }
            val invalidQrCode = "otpauth://totp/secret=SECRET"
            every { Uri.parse(invalidQrCode) } returns invalidUri
            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidQrCode))
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
            verify { authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError) }
        }

    private fun createViewModel() = QrCodeScanViewModel(
        authenticatorBridgeManager = authenticatorBridgeManager,
        authenticatorRepository = authenticatorRepository,
        settingsRepository = settingsRepository,
    )
}

private val DEFAULT_STATE = QrCodeScanState(
    dialog = null,
)
private const val VALID_TOTP_CODE = "otpauth://totp/Label?secret=SECRET&issuer=Issuer"
private val VALID_TOTP_URI: Uri = mockk {
    every { getQueryParameter("secret") } returns "SECRET"
    every { queryParameterNames } returns emptySet()
}
private val VALID_TOTP_RESULT = TotpCodeResult.TotpCodeScan(VALID_TOTP_CODE)
