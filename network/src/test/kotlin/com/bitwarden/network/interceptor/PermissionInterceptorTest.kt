package com.bitwarden.network.interceptor

import com.bitwarden.network.exception.LocalNetworkAccessException
import com.bitwarden.network.provider.PermissionProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.UnknownHostException

class PermissionInterceptorTest {
    private val permissionProvider = mockk<PermissionProvider>()
    private val interceptor = PermissionInterceptor(
        permissionProvider = permissionProvider,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(InetAddress::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(InetAddress::class)
    }

    @Test
    fun `intercept should proceed when local network access permission is granted`() {
        every { permissionProvider.hasLocalNetworkAccessPermission } returns true
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://192.168.1.1/api").build(),
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
        }
        verify(exactly = 0) {
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Test
    fun `intercept should proceed when request is not targeting the local network`() {
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { InetAddress.getByName("8.8.8.8") } returns mockk {
            every { isSiteLocalAddress } returns false
        }
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://8.8.8.8/api").build(),
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
        }
        verify(exactly = 0) {
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `intercept should acquire permission and throw when request targets local network without permission`() {
        val errorMessage = "Local network access required"
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { permissionProvider.errorMessageString } returns errorMessage
        every { permissionProvider.acquireLocalNetworkAccessPermission() } just runs
        every { InetAddress.getByName("192.168.1.1") } returns mockk {
            every { isSiteLocalAddress } returns true
        }
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://192.168.1.1/api").build(),
        )

        val exception = assertThrows(LocalNetworkAccessException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals(errorMessage, exception.message)
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Test
    fun `intercept should rethrow UnknownHostException when DNS resolution fails`() {
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { InetAddress.getByName(any()) } throws UnknownHostException("unknownhost")
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://unknownhost/api").build(),
        )

        assertThrows(UnknownHostException::class.java) {
            interceptor.intercept(chain)
        }
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
        }
        verify(exactly = 0) {
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `intercept should treat request as local and throw when SecurityException occurs during DNS resolution`() {
        val errorMessage = "Local network access required"
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { permissionProvider.errorMessageString } returns errorMessage
        every { permissionProvider.acquireLocalNetworkAccessPermission() } just runs
        every { InetAddress.getByName(any()) } throws SecurityException("permission denied")
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://somehost/api").build(),
        )

        val exception = assertThrows(LocalNetworkAccessException::class.java) {
            interceptor.intercept(chain)
        }

        assertEquals(errorMessage, exception.message)
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }
}
