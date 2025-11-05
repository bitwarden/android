package com.bitwarden.ui.platform.manager.snackbar

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SnackbarRelayManagerTest {

    private val relayManager: SnackbarRelayManager<TestRelay> = SnackbarRelayManagerImpl(
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `when relay is completed successfully when consumer registers first and event is sent`() =
        runTest {
            val relay = TestRelay.TEST1
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())

            relayManager.getSnackbarDataFlow(relay).test {
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(expectedData, awaitItem())
            }
        }

    @Test
    fun `when relay is completed successfully when consumer registers second and event is sent`() =
        runTest {
            val relay = TestRelay.TEST1
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            // producer code
            relayManager.sendSnackbarData(data = expectedData, relay = relay)
            relayManager.getSnackbarDataFlow(relay).test {
                assertEquals(expectedData, awaitItem())
            }
        }

    @Test
    fun `when relay is specified by producer only send data to that relay`() = runTest {
        val relay1 = TestRelay.TEST1
        val relay2 = TestRelay.TEST2
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
            val relay = TestRelay.TEST1
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
            val relay = TestRelay.TEST1
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
            val relay = TestRelay.TEST1
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

private enum class TestRelay {
    TEST1,
    TEST2,
}
