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
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

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
        val expectedResponse = mockk<Response>()
        val chain = FakeInterceptorChain(
            request = Request.Builder().url("http://8.8.8.8/api").build(),
            responseProvider = { expectedResponse },
        )

        val response = interceptor.intercept(chain)

        assertSame(expectedResponse, response)
        verify(exactly = 1) {
            permissionProvider.hasLocalNetworkAccessPermission
        }
        verify(exactly = 0) {
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Test
    fun `intercept should offer timeout recovery for IPv6 and public endpoints`() {
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { permissionProvider.acquireLocalNetworkAccessPermission() } just runs
        // Routing determines local reachability, so even public timeouts offer recovery.
        listOf(
            "ula.example" to "fd00::1",
            "global.example" to "2001:db8::1",
            "vault.bitwarden.com" to "8.8.8.8",
        ).forEachIndexed { index, (host, address) ->
            every { InetAddress.getByName(host) } returns mockk(
                name = address,
            ) {
                every { isSiteLocalAddress } returns false
            }
            val request = Request.Builder().url("https://$host/api").build()
            val timeout = SocketTimeoutException(address)
            val chain = mockk<Interceptor.Chain> {
                every { request() } returns request
                every { proceed(request) } throws timeout
            }

            val exception = assertThrows(SocketTimeoutException::class.java) {
                interceptor.intercept(chain)
            }

            assertSame(timeout, exception)
            verify(exactly = 1) { chain.proceed(request) }
            verify(exactly = index + 1) {
                permissionProvider.acquireLocalNetworkAccessPermission()
            }
        }
        verify(exactly = 6) { permissionProvider.hasLocalNetworkAccessPermission }
        verify(exactly = 0) { permissionProvider.errorMessageString }
    }

    @Test
    fun `intercept should recheck permission after a timeout`() {
        listOf(true, false).forEach { initiallyGranted ->
            every {
                permissionProvider.hasLocalNetworkAccessPermission
            } returnsMany listOf(initiallyGranted, true)
            every { InetAddress.getByName("self-hosted.example") } returns mockk {
                every { isSiteLocalAddress } returns false
            }
            val request = Request.Builder().url("https://self-hosted.example/api").build()
            val timeout = SocketTimeoutException("timeout")
            val chain = mockk<Interceptor.Chain> {
                every { request() } returns request
                every { proceed(request) } throws timeout
            }

            val exception = assertThrows(SocketTimeoutException::class.java) {
                interceptor.intercept(chain)
            }

            assertSame(timeout, exception)
            verify(exactly = 1) { chain.proceed(request) }
            verify(exactly = if (initiallyGranted) 0 else 1) {
                InetAddress.getByName("self-hosted.example")
            }
        }
        verify(exactly = 4) { permissionProvider.hasLocalNetworkAccessPermission }
        verify(exactly = 0) {
            permissionProvider.acquireLocalNetworkAccessPermission()
            permissionProvider.errorMessageString
        }
    }

    @Test
    fun `intercept should propagate other IO failures without offering permission recovery`() {
        every { permissionProvider.hasLocalNetworkAccessPermission } returns false
        every { InetAddress.getByName("self-hosted.example") } returns mockk {
            every { isSiteLocalAddress } returns false
        }
        listOf(
            UnknownHostException("unknown host"),
            SSLHandshakeException("TLS failure"),
            IOException("canceled"),
            InterruptedIOException("interrupted"),
        ).forEach { failure ->
            val request = Request.Builder().url("https://self-hosted.example/api").build()
            val chain = mockk<Interceptor.Chain> {
                every { request() } returns request
                every { proceed(request) } throws failure
            }

            val exception = assertThrows(IOException::class.java) {
                interceptor.intercept(chain)
            }

            assertSame(failure, exception)
            verify(exactly = 1) { chain.proceed(request) }
        }
        verify(exactly = 4) { permissionProvider.hasLocalNetworkAccessPermission }
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
