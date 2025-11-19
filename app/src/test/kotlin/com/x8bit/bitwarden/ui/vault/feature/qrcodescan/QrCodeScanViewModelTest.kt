package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.util.getTotpDataOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QrCodeScanViewModelTest : BaseViewModelTest() {

    private val totpTestCodeFlow: Flow<TotpCodeResult> = bufferedMutableSharedFlow()
    private val vaultRepository: VaultRepository = mockk {
        every { totpCodeFlow } returns totpTestCodeFlow
        every { emitTotpCodeResult(any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(String::getTotpDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(String::getTotpDataOrNull)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.CloseClick)
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CameraErrorReceive should emit NavigateToManualCodeEntry`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.CameraSetupErrorReceive)
            assertEquals(
                QrCodeScanEvent.NavigateToManualCodeEntry,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ManualEntryTextClick should emit NavigateToManualCodeEntry`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.ManualEntryTextClick)
            assertEquals(
                QrCodeScanEvent.NavigateToManualCodeEntry,
                awaitItem(),
            )
        }
    }

    @Test
    fun `QrCodeScanReceive with valid code should emit new code and NavigateBack`() = runTest {
        val viewModel = createViewModel()
        val validCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP"
        val result = TotpCodeResult.Success(validCode)
        every { validCode.getTotpDataOrNull() } returns mockk()

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(validCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScanReceive with invalid totp should emit failure result`() = runTest {
        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError()
        val invalidCode = "nototpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP"
        every { invalidCode.getTotpDataOrNull() } returns null

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(): QrCodeScanViewModel =
        QrCodeScanViewModel(
            vaultRepository = vaultRepository,
        )
}
