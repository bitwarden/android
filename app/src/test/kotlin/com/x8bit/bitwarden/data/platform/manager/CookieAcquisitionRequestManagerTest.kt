package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CookieAcquisitionRequestManagerTest {

    private val manager = CookieAcquisitionRequestManagerImpl()

    @Test
    fun `pendingCookieAcquisitionFlow initial value is null`() = runTest {
        manager.cookieAcquisitionRequestFlow.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `setPendingCookieAcquisition emits new value to flow`() = runTest {
        val pending = CookieAcquisitionRequest(hostname = "vault.bitwarden.com")

        manager.cookieAcquisitionRequestFlow.test {
            assertNull(awaitItem())

            manager.setPendingCookieAcquisition(pending)
            assertEquals(pending, awaitItem())
        }
    }

    @Test
    fun `setPendingCookieAcquisition can clear pending state`() = runTest {
        val pending = CookieAcquisitionRequest(hostname = "vault.bitwarden.com")

        manager.cookieAcquisitionRequestFlow.test {
            assertNull(awaitItem())

            manager.setPendingCookieAcquisition(pending)
            assertEquals(pending, awaitItem())

            manager.setPendingCookieAcquisition(null)
            assertNull(awaitItem())
        }
    }

    @Test
    fun `setPendingCookieAcquisition replaces existing value`() = runTest {
        val pending1 = CookieAcquisitionRequest(hostname = "vault1.bitwarden.com")
        val pending2 = CookieAcquisitionRequest(hostname = "vault2.bitwarden.com")

        manager.cookieAcquisitionRequestFlow.test {
            assertNull(awaitItem())

            manager.setPendingCookieAcquisition(pending1)
            assertEquals(pending1, awaitItem())

            manager.setPendingCookieAcquisition(pending2)
            assertEquals(pending2, awaitItem())
        }
    }
}
