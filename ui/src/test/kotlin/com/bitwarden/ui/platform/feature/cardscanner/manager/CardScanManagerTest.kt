package com.bitwarden.ui.platform.feature.cardscanner.manager

import app.cash.turbine.test
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanData
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardScanManagerTest {

    private val manager = CardScanManagerImpl()

    @Test
    fun `emitCardScanResult should emit Success to cardScanResultFlow`() =
        runTest {
            manager.cardScanResultFlow.test {
                val expected = CardScanResult.Success(
                    cardScanData = CardScanData(
                        number = "4111111111111111",
                        expirationMonth = "12",
                        expirationYear = "2025",
                        cardholderName = "JOHN DOE",
                        securityCode = "123",
                    ),
                )
                manager.emitCardScanResult(expected)
                assertEquals(expected, awaitItem())
            }
        }

    @Test
    fun `emitCardScanResult should emit ScanError to cardScanResultFlow`() =
        runTest {
            manager.cardScanResultFlow.test {
                val expected = CardScanResult.ScanError()
                manager.emitCardScanResult(expected)
                assertEquals(expected, awaitItem())
            }
        }
}
