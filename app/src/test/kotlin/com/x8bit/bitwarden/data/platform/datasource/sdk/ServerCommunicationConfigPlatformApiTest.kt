package com.x8bit.bitwarden.data.platform.datasource.sdk

import com.x8bit.bitwarden.data.platform.error.CookiesRequiredException
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.manager.sdk.platformapi.ServerCommunicationConfigPlatformApiImpl
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ServerCommunicationConfigPlatformApiTest {

    private val serverCommConfigManager: CookieAcquisitionRequestManager = mockk {
        every { setPendingCookieAcquisition(any()) } just runs
    }

    private val platformApi = ServerCommunicationConfigPlatformApiImpl(
        serverCommConfigManager = serverCommConfigManager,
    )

    @Test
    fun `acquireCookies sets pending state and throws exception`() = runTest {
        val hostname = "vault.bitwarden.com"
        val pendingSlot = slot<CookieAcquisitionRequest>()
        every {
            serverCommConfigManager.setPendingCookieAcquisition(capture(pendingSlot))
        } just runs

        val exception = assertThrows<CookiesRequiredException> {
            platformApi.acquireCookies(hostname)
        }

        assertEquals(hostname, exception.hostname)
        assertEquals("Cookie acquisition required for $hostname", exception.message)

        verify { serverCommConfigManager.setPendingCookieAcquisition(any()) }

        val capturedPending = pendingSlot.captured
        assertEquals(
            CookieAcquisitionRequest(hostname = hostname),
            capturedPending,
        )
    }

    @Test
    fun `acquireCookies exception contains correct hostname for custom domain`() = runTest {
        val hostname = "custom.example.com"
        every { serverCommConfigManager.setPendingCookieAcquisition(any()) } just runs

        val exception = assertThrows<CookiesRequiredException> {
            platformApi.acquireCookies(hostname)
        }

        assertEquals(hostname, exception.hostname)
        assertEquals("Cookie acquisition required for $hostname", exception.message)
    }
}
