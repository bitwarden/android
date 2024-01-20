package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class NetworkConnectionManagerTest {
    @Test
    fun `isNetworkConnected should return false if no active network`() {
        val connectivityManager: ConnectivityManager = mockk {
            every { activeNetwork } returns null
            every { getNetworkCapabilities(any()) } returns null
        }
        val networkConnectionManager = createNetworkConnectionManager(connectivityManager)
        assertFalse(networkConnectionManager.isNetworkConnected)
    }

    @Test
    fun `isNetworkConnected should return false if active network has no Internet capabilities`() {
        val network: Network = mockk()
        val networkCapabilities: NetworkCapabilities = mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        }
        val connectivityManager: ConnectivityManager = mockk {
            every { activeNetwork } returns network
            every { getNetworkCapabilities(network) } returns networkCapabilities
        }
        val networkConnectionManager = createNetworkConnectionManager(connectivityManager)
        assertFalse(networkConnectionManager.isNetworkConnected)
    }

    @Test
    fun `isNetworkConnected should return true if active network has Internet capabilities`() {
        val network: Network = mockk()
        val networkCapabilities: NetworkCapabilities = mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        }
        val connectivityManager: ConnectivityManager = mockk {
            every { activeNetwork } returns network
            every { getNetworkCapabilities(network) } returns networkCapabilities
        }
        val networkConnectionManager = createNetworkConnectionManager(connectivityManager)
        assertTrue(networkConnectionManager.isNetworkConnected)
    }

    private fun createNetworkConnectionManager(
        connectivityManager: ConnectivityManager,
    ): NetworkConnectionManager {
        val appContext: Context = mockk {
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        }
        val context: Context = mockk {
            every { applicationContext } returns appContext
        }

        return NetworkConnectionManagerImpl(context)
    }
}
