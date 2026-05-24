package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.feature.cardscanner.manager.CardScanManager
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanData
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
            cardScanManager.emitCardScanResult(CardScanResult.ScanError())
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

    @Test
    fun `showHint should be true after the timeout elapses with no scan`() = runTest {
        val viewModel = createViewModel()

        assertEquals(false, viewModel.stateFlow.value.showHint)

        advanceTimeBy(SCAN_HINT_TIMEOUT_MS + 100)

        assertEquals(
            DEFAULT_STATE.copy(showHint = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `showHint should remain false when scan succeeds before the timeout`() = runTest {
        val viewModel = createViewModel()

        // Advance well below the timeout, then emit a successful scan.
        advanceTimeBy(SCAN_HINT_TIMEOUT_MS - 1_000)
        viewModel.trySendAction(
            CardScanAction.CardScanReceive(cardScanData = CARD_SCAN_DATA),
        )

        // Advance past where the timeout would have fired.
        advanceTimeBy(2_000)

        assertEquals(
            DEFAULT_STATE.copy(hasHandledScan = true, showHint = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `showHint should be reset to false when a scan succeeds after the hint shows`() = runTest {
        val viewModel = createViewModel()

        // Let the timeout fire so the hint is showing.
        advanceTimeBy(SCAN_HINT_TIMEOUT_MS + 100)
        assertEquals(true, viewModel.stateFlow.value.showHint)

        // Receive a successful scan.
        viewModel.trySendAction(
            CardScanAction.CardScanReceive(cardScanData = CARD_SCAN_DATA),
        )

        assertEquals(
            DEFAULT_STATE.copy(hasHandledScan = true, showHint = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `hint timeout should be cancelled when the screen exits via CloseClick`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CardScanAction.CloseClick)

        // Advance well past the timeout window. Without cancellation, showHint would flip true.
        advanceTimeBy(SCAN_HINT_TIMEOUT_MS + 1_000)

        assertEquals(false, viewModel.stateFlow.value.showHint)
    }

    @Test
    fun `hint timeout should be cancelled when the camera fails to set up`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CardScanAction.CameraSetupErrorReceive)

        advanceTimeBy(SCAN_HINT_TIMEOUT_MS + 1_000)

        assertEquals(false, viewModel.stateFlow.value.showHint)
    }

    @Test
    fun `HintTimeoutElapsed is a no-op when state already shows hasHandledScan`() = runTest {
        val viewModel = createViewModel(
            initialState = DEFAULT_STATE.copy(hasHandledScan = true),
        )

        viewModel.trySendAction(CardScanAction.Internal.HintTimeoutElapsed)

        assertEquals(false, viewModel.stateFlow.value.showHint)
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

private const val SCAN_HINT_TIMEOUT_MS = 5_000L

private val DEFAULT_STATE = CardScanState(
    hasHandledScan = false,
    showHint = false,
)

private val CARD_SCAN_DATA = CardScanData(
    number = "4111111111111111",
    expirationMonth = "12",
    expirationYear = "2025",
    securityCode = "123",
)
