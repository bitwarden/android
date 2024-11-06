package com.x8bit.bitwarden.ui.platform.manager.snackbar

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SnackbarRelayManagerTest {

    @Test
    fun `Relay is completed successfully when consumer registers first and event is sent`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay = SnackbarRelay.MY_VAULT_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            val consumer = relayManager.getSnackbarDataFlow(relay)

            consumer.test {
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(
                    expectedData,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `Relay is completed successfully when consumer registers second and event is sent`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay = SnackbarRelay.MY_VAULT_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            // producer code
            relayManager.sendSnackbarData(data = expectedData, relay = relay)
            relayManager.getSnackbarDataFlow(relay).test {
                assertEquals(
                    expectedData,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `When relay is specified by producer only send data to that relay`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay1 = SnackbarRelay.MY_VAULT_RELAY
            val relay2 = SnackbarRelay.VAULT_SETTINGS_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay1).testIn(backgroundScope)
                val consumer2 = relayManager.getSnackbarDataFlow(relay2).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay1)
                consumer2.expectNoEvents()
                assertEquals(
                    expectedData,
                    consumer1.awaitItem(),
                )
            }
        }

    @Test
    fun `When multiple consumers are registered to the same relay, send data to all consumers`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay = SnackbarRelay.MY_VAULT_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(
                    expectedData,
                    consumer1.awaitItem(),
                )
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                assertEquals(
                    expectedData,
                    consumer2.awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `When multiple consumers are registered to the same relay, and one is completed before the other the second consumer registers should not receive any emissions`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay = SnackbarRelay.MY_VAULT_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(
                    expectedData,
                    consumer1.awaitItem(),
                )
                consumer1.cancel()
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                consumer2.expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `When multiple consumers register to the same relay, and clearRelayBuffer is called, the second consumer should not receive any emissions`() =
        runTest {
            val relayManager = SnackbarRelayManagerImpl()
            val relay = SnackbarRelay.MY_VAULT_RELAY
            val expectedData = BitwardenSnackbarData(message = "Test message".asText())
            turbineScope {
                val consumer1 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                relayManager.sendSnackbarData(data = expectedData, relay = relay)
                assertEquals(
                    expectedData,
                    consumer1.awaitItem(),
                )
                relayManager.clearRelayBuffer(relay)
                val consumer2 = relayManager.getSnackbarDataFlow(relay).testIn(backgroundScope)
                consumer2.expectNoEvents()
            }
        }
}
