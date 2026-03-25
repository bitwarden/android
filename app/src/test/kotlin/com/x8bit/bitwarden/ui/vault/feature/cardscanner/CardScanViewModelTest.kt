package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanData
import com.bitwarden.ui.platform.feature.cardscanner.manager.CardScanManager
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardScanViewModelTest : BaseViewModelTest() {

    private val cardScanManager: CardScanManager = mockk {
        every { emitCardScanResult(any()) } just runs
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CardScanAction.CloseClick)
            assertEquals(CardScanEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CameraSetupErrorReceive should emit ScanError and NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CardScanAction.CameraSetupErrorReceive)
            assertEquals(CardScanEvent.NavigateBack, awaitItem())
        }

        verify(exactly = 1) {
            cardScanManager.emitCardScanResult(
                match { it is CardScanResult.ScanError },
            )
        }
    }

    @Test
    fun `CardScanReceive should emit result and NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                CardScanAction.CardScanReceive(
                    cardScanData = CARD_SCAN_DATA,
                ),
            )
            assertEquals(CardScanEvent.NavigateBack, awaitItem())
        }

        verify(exactly = 1) {
            cardScanManager.emitCardScanResult(
                CardScanResult.Success(cardScanData = CARD_SCAN_DATA),
            )
        }
    }

    @Test
    fun `CardScanReceive should only handle first scan`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                CardScanAction.CardScanReceive(
                    cardScanData = CARD_SCAN_DATA,
                ),
            )
            assertEquals(CardScanEvent.NavigateBack, awaitItem())

            viewModel.trySendAction(
                CardScanAction.CardScanReceive(
                    cardScanData = CARD_SCAN_DATA.copy(
                        number = "5500000000000004",
                    ),
                ),
            )
            expectNoEvents()
        }

        verify(exactly = 1) { cardScanManager.emitCardScanResult(any()) }
        assertEquals(
            DEFAULT_STATE.copy(hasHandledScan = true),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: CardScanState? = null,
    ): CardScanViewModel =
        CardScanViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", initialState)
            },
            cardScanManager = cardScanManager,
        )
}

private val DEFAULT_STATE = CardScanState(
    hasHandledScan = false,
)

private val CARD_SCAN_DATA = CardScanData(
    number = "4111111111111111",
    expirationMonth = "12",
    expirationYear = "2025",
    cardholderName = "JOHN DOE",
    securityCode = "123",
)
