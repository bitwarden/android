package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QrCodeScanViewModelTest : BaseViewModelTest() {

    private val totpTestCodeFlow: Flow<String> = bufferedMutableSharedFlow()
    private val vaultRepository: VaultRepository = mockk {
        every { totpCodeFlow } returns totpTestCodeFlow
        every { emitTotpCode(any()) } just runs
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(QrCodeScanAction.CloseClick)
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CameraErrorReceive should emit ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(QrCodeScanAction.CameraSetupErrorReceive)
            assertEquals(
                QrCodeScanEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ManualEntryTextClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(QrCodeScanAction.ManualEntryTextClick)
            assertEquals(
                QrCodeScanEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `QrCodeScan should emit new code and NavigateBack`() = runTest {
        val viewModel = createViewModel()
        val code = "NewCode"

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(QrCodeScanAction.QrCodeScanReceive(code))

            verify(exactly = 1) { vaultRepository.emitTotpCode(code) }
            assertEquals(QrCodeScanEvent.NavigateBack, awaitItem())
        }
    }

    fun createViewModel(): QrCodeScanViewModel =
        QrCodeScanViewModel(
            vaultRepository = vaultRepository,
        )
}
