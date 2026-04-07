package com.x8bit.bitwarden.data.platform.manager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.NetworkConnection
import com.x8bit.bitwarden.data.platform.manager.model.NetworkSignalStrength
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkConnectionManagerTest {
    private val networkCallback = slot<ConnectivityManager.NetworkCallback>()
    private val connectivityManager = mockk<ConnectivityManager> {
        every {
            registerNetworkCallback(any<NetworkRequest>(), capture(networkCallback))
        } just runs
        every { activeNetwork } returns null
        every { getNetworkCapabilities(any()) } returns null
    }
    private val context = mockk<Context> {
        every { applicationContext } returns this
        every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    }
    private val fakeDispatcherManager = FakeDispatcherManager()

    private lateinit var networkConnectionManager: NetworkConnectionManagerImpl

    @BeforeEach
    fun setup() {
        mockkConstructor(NetworkRequest.Builder::class)
        val builder = mockk<NetworkRequest.Builder> {
            every { addTransportType(any()) } returns this
            every { build() } returns mockk()
        }
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns builder
        networkConnectionManager = NetworkConnectionManagerImpl(
            context = context,
            dispatcherManager = fakeDispatcherManager,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NetworkRequest.Builder::class)
    }

    @Test
    fun `isNetworkConnected should return false if no active network`() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns null
        assertFalse(networkConnectionManager.isNetworkConnected)
    }

    @Test
    fun `isNetworkConnected should return false if active network has no Internet capabilities`() {
        val network: Network = mockk()
        val networkCapabilities: NetworkCapabilities = mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        assertFalse(networkConnectionManager.isNetworkConnected)
    }

    @Test
    fun `isNetworkConnected should return true if active network has Internet capabilities`() {
        val network: Network = mockk()
        val networkCapabilities: NetworkCapabilities = mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        assertTrue(networkConnectionManager.isNetworkConnected)
    }

    @Test
    fun `isNetworkConnectedFlow should emit changes to the network state`() = runTest {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { signalStrength } returns -75
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        }
        every { connectivityManager.activeNetwork } returns network

        networkConnectionManager
            .isNetworkConnectedFlow
            .test {
                assertFalse(awaitItem())

                every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
                networkCallback.captured.onLost(mockk())
                assertTrue(awaitItem())

                every {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } returns false
                networkCallback.captured.onLinkPropertiesChanged(mockk(), mockk())
                assertFalse(awaitItem())
            }
    }

    @Test
    fun `networkConnection should return None if no active network`() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns null
        assertEquals(NetworkConnection.None, networkConnectionManager.networkConnection)
    }

    @Test
    fun `networkConnection should return none Wifi if active network has wifi transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { signalStrength } returns -120
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(
            NetworkConnection.Wifi(strength = NetworkSignalStrength.NONE),
            networkConnectionManager.networkConnection,
        )
    }

    @Test
    fun `networkConnection should return weak Wifi if active network has wifi transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { signalStrength } returns -100
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(
            NetworkConnection.Wifi(strength = NetworkSignalStrength.WEAK),
            networkConnectionManager.networkConnection,
        )
    }

    @Test
    fun `networkConnection should return fair Wifi if active network has wifi transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { signalStrength } returns -90
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(
            NetworkConnection.Wifi(strength = NetworkSignalStrength.FAIR),
            networkConnectionManager.networkConnection,
        )
    }

    @Test
    fun `networkConnection should return good Wifi if active network has wifi transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { signalStrength } returns -75
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(
            NetworkConnection.Wifi(strength = NetworkSignalStrength.GOOD),
            networkConnectionManager.networkConnection,
        )
    }

    @Test
    fun `networkConnection should return excellent Wifi if active network has wifi transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { signalStrength } returns -50
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(
            NetworkConnection.Wifi(strength = NetworkSignalStrength.EXCELLENT),
            networkConnectionManager.networkConnection,
        )
    }

    @Test
    fun `networkConnection should return Cellular if active network has cellular transport`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        }
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        assertEquals(NetworkConnection.Cellular, networkConnectionManager.networkConnection)
    }

    @Test
    fun `networkConnectionFlow should emit changes to the network state`() = runTest {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { signalStrength } returns -75
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        }
        every { connectivityManager.activeNetwork } returns network

        networkConnectionManager
            .networkConnectionFlow
            .test {
                assertEquals(NetworkConnection.None, awaitItem())

                every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
                networkCallback.captured.onCapabilitiesChanged(mockk(), mockk())
                assertEquals(NetworkConnection.Cellular, awaitItem())

                every {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } returns true
                every {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                } returns false
                networkCallback.captured.onAvailable(mockk())
                assertEquals(
                    NetworkConnection.Wifi(strength = NetworkSignalStrength.GOOD),
                    awaitItem(),
                )
            }
    }
}
