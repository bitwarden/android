package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import androidx.camera.core.ImageProxy
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.util.FakeQrCodeAnalyzer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class QrCodeScanScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val imageProxy: ImageProxy = mockk()
    private val qrCodeAnalyzer = FakeQrCodeAnalyzer()

    private val mutableEventFlow = bufferedMutableSharedFlow<QrCodeScanEvent>()

    private val viewModel = mockk<QrCodeScanViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            QrCodeScanScreen(
                qrCodeAnalyzer = qrCodeAnalyzer,
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(QrCodeScanEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking on manual text should send ManualEntryTextClick`() = runTest {
        composeTestRule
            .onNodeWithText("Enter key manually")
            .performClick()

        verify {
            viewModel.trySendAction(QrCodeScanAction.ManualEntryTextClick)
        }
    }

    @Test
    fun `when unable to setup camera CameraErrorReceive will be sent`() = runTest {
        // Because the camera is not set up in the tests, this will always be triggered
        verify {
            viewModel.trySendAction(QrCodeScanAction.CameraSetupErrorReceive)
        }
    }

    @Test
    fun `when a scan is successful a result will be sent`() = runTest {
        val result = "testCode"

        qrCodeAnalyzer.scanResult = result
        qrCodeAnalyzer.analyze(imageProxy)

        verify {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(result))
        }
    }

    @Test
    fun `when a scan is unsuccessful a result will not be sent`() = runTest {
        val result = "testCode"

        qrCodeAnalyzer.scanResult = null
        qrCodeAnalyzer.analyze(imageProxy)

        verify(exactly = 0) {
            viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(result))
        }
    }
}
