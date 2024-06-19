package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import android.net.Uri
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
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
    private val uriMock = mockk<Uri>()

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
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
    fun `ManualEntryTextClick should emit ShowToast`() = runTest {
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
    fun `QrCodeScan should emit new code and NavigateBack with a valid code with all values`() =
        runTest {
            setupMockUri()

            val validCode =
                "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP&algorithm=sha256&digits=8&period=60"
            val viewModel = createViewModel()
            val result = TotpCodeResult.Success(validCode)

            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(validCode))

                verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `QrCodeScan should emit new code and NavigateBack without optional values`() = runTest {
        setupMockUri(
            queryParameterNames = setOf(SECRET),
        )

        val validCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP"
        val viewModel = createViewModel()
        val result = TotpCodeResult.Success(validCode)

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(validCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack with invalid algorithm`() =
        runTest {
            setupMockUri(algorithm = "SHA-224")

            val viewModel = createViewModel()
            val result = TotpCodeResult.CodeScanningError
            val invalidCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP&algorithm=sha224"

            viewModel.eventFlow.test {
                viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

                verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
                assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack with invalid digits`() = runTest {
        setupMockUri(digits = "11")

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP&digits=11"

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack with invalid period`() = runTest {
        setupMockUri(period = "0")

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP&period=0"

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result without correct prefix`() = runTest {

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "nototpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP"

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result with non base32 secret`() = runTest {
        setupMockUri(secret = "JBSWY3dpeHPK3PXP1")

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "otpauth://totp/Test:me?secret=JBSWY3dpeHPK3PXP1"

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack without Secret`() = runTest {
        setupMockUri(secret = null)

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "otpauth://totp/Test:me"

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack if secret is empty`() = runTest {
        setupMockUri(secret = "")

        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = "otpauth://totp/Test:me?secret= "

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `QrCodeScan should emit failure result and NavigateBack if code is empty`() = runTest {
        val viewModel = createViewModel()
        val result = TotpCodeResult.CodeScanningError
        val invalidCode = ""

        viewModel.eventFlow.test {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(invalidCode))

            verify(exactly = 1) { vaultRepository.emitTotpCodeResult(result) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    private fun setupMockUri(
        secret: String? = "JBSWY3dpeHPK3PXP",
        algorithm: String = "SHA256",
        digits: String = "8",
        period: String = "60",
        queryParameterNames: Set<String> = setOf(
            ALGORITHM, PERIOD, DIGITS, SECRET,
        ),
    ) {
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.getQueryParameter(SECRET) } returns secret
        every { uriMock.getQueryParameter(ALGORITHM) } returns algorithm
        every { uriMock.getQueryParameter(DIGITS) } returns digits
        every { uriMock.getQueryParameter(PERIOD) } returns period
        every { uriMock.queryParameterNames } returns queryParameterNames
    }

    private fun createViewModel(): QrCodeScanViewModel =
        QrCodeScanViewModel(
            vaultRepository = vaultRepository,
        )

    companion object {
        private const val ALGORITHM = "algorithm"
        private const val DIGITS = "digits"
        private const val PERIOD = "period"
        private const val SECRET = "secret"
    }
}
