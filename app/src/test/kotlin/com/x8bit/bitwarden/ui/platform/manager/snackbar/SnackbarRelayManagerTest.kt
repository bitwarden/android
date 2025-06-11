package com.x8bit.bitwarden.ui.platform.manager.snackbar

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.data.datasource.disk.base.FakeDispatcherManager
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SnackbarRelayManagerTest {

    private val relayManager: SnackbarRelayManager = SnackbarRelayManagerImpl(
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `when relay is completed successfully when consumer registers first and event is sent`() =
        runTest {
            val relay = SnackbarRelay.LOGINS_IMPORTED
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())

            relayManager.getSnackbarDataFlow(relay).test {
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(expectedData, awaitItem())
            }
        }

    @Test
    fun `when relay is completed successfully when consumer registers second and event is sent`() =
        runTest {
            val relay = SnackbarRelay.LOGINS_IMPORTED
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            // producer code
            relayManager.sendSnackbarData(data = expectedData, relay = relay)
            relayManager.getSnackbarDataFlow(relay).test {
                assertEquals(expectedData, awaitItem())
            }
        }

    @Test
    fun `when relay is specified by producer only send data to that relay`() = runTest {
        val relay1 = SnackbarRelay.LOGINS_IMPORTED
        val relay2 = SnackbarRelay.SEND_DELETED
        val expectedData = BitwardenSnackbarData(message = "Test message".asText())
        turbineScope {
            val consumer1 = relayManager.getSnackbarDataFlow(relay1).testIn(backgroundScope)
            val consumer2 = relayManager.getSnackbarDataFlow(relay2).testIn(backgroundScope)
            relayManager.sendSnackbarData(data = expectedData, relay = relay1)
            consumer2.expectNoEvents()
            assertEquals(expectedData, consumer1.awaitItem())
        }
    }

    @Test
    fun `when multiple consumers are registered to the same relay, send data to last consumers`() =
        runTest {
            val relay = SnackbarRelay.LOGINS_IMPORTED
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(expectedData, consumer2.awaitItem())
                consumer1.expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when multiple consumers are registered to the same relay, and one is completed before the other the second consumer registers should not receive any emissions`() =
        runTest {
            val relay = SnackbarRelay.LOGINS_IMPORTED
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(expectedData, consumer1.awaitItem())
                consumer1.cancel()
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                consumer2.expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when multiple consumers are registered to the same relay, and the last one is cancelled, the other most recent consumer should receive the emissions`() =
        runTest {
            val relay = SnackbarRelay.LOGINS_IMPORTED
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                consumer2.cancel()
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(expectedData, consumer1.awaitItem())
            }
        }
}
