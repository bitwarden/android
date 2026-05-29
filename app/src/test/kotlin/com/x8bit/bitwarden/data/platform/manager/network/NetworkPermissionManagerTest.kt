package com.x8bit.bitwarden.data.platform.manager.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkPermissionManagerTest {
    private val context: Context = mockk()
    private val resourceManager: ResourceManager = mockk()

    private val networkPermissionManager: NetworkPermissionManager = NetworkPermissionManagerImpl(
        context = context,
        resourceManager = resourceManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(
            ContextCompat::checkSelfPermission,
            ::isBuildVersionAtLeast,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ContextCompat::checkSelfPermission,
            ::isBuildVersionAtLeast,
        )
    }

    @Test
    fun `errorMessageString should return the string from context`() {
        every {
            resourceManager.getString(
                resId = BitwardenString
                    .your_request_was_interrupted_because_the_app_needs_local_network_access,
            )
        } returns ERROR_MESSAGE
        assertEquals(ERROR_MESSAGE, networkPermissionManager.errorMessageString)
        verify(exactly = 1) {
            resourceManager.getString(
                resId = BitwardenString
                    .your_request_was_interrupted_because_the_app_needs_local_network_access,
            )
        }
    }

    @Test
    fun `hasLocalNetworkAccessPermission returns true when below CINNAMON_BUN`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.CINNAMON_BUN) } returns false

        assertTrue(networkPermissionManager.hasLocalNetworkAccessPermission)
    }

    @Test
    fun `hasLocalNetworkAccessPermission returns true when permission granted on CINNAMON_BUN+`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.CINNAMON_BUN) } returns true
        every {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            )
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(networkPermissionManager.hasLocalNetworkAccessPermission)
    }

    @Test
    fun `hasLocalNetworkAccessPermission returns false when permission denied on CINNAMON_BUN+`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.CINNAMON_BUN) } returns true
        every {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            )
        } returns PackageManager.PERMISSION_DENIED

        assertFalse(networkPermissionManager.hasLocalNetworkAccessPermission)
    }

    @Test
    fun `isLocalNetworkAccessRequiredStateFlow initial value is false`() = runTest {
        networkPermissionManager.isLocalNetworkAccessRequiredStateFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `acquireLocalNetworkAccessPermission sets isLocalNetworkAccessRequired to true`() =
        runTest {
            networkPermissionManager.isLocalNetworkAccessRequiredStateFlow.test {
                assertFalse(awaitItem())

                networkPermissionManager.acquireLocalNetworkAccessPermission()

                assertTrue(awaitItem())
            }
        }

    @Test
    fun `clearIsLocalNetworkAccessRequired sets isLocalNetworkAccessRequired to false`() =
        runTest {
            networkPermissionManager.isLocalNetworkAccessRequiredStateFlow.test {
                assertFalse(awaitItem())

                networkPermissionManager.acquireLocalNetworkAccessPermission()
                assertTrue(awaitItem())

                networkPermissionManager.clearIsLocalNetworkAccessRequired()
                assertFalse(awaitItem())
            }
        }
}

private const val ERROR_MESSAGE = "error message"
