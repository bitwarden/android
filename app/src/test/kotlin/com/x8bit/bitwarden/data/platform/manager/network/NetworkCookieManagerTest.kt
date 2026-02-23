package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.datasource.disk.util.FakeConfigDiskSource
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.model.NetworkCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkCookieManagerTest {

    private val fakeConfigDiskSource = FakeConfigDiskSource()
    private val mockCookieDiskSource: CookieDiskSource = mockk()
    private val mockCookieAcquisitionRequestManager: CookieAcquisitionRequestManager =
        mockk {
            every { setPendingCookieAcquisition(any()) } just runs
        }

    private val manager = NetworkCookieManagerImpl(
        configDiskSource = fakeConfigDiskSource,
        cookieDiskSource = mockCookieDiskSource,
        cookieAcquisitionRequestManager = mockCookieAcquisitionRequestManager,
    )

    @Test
    fun `needsBootstrap should return false when serverConfig is null`() {
        fakeConfigDiskSource.serverConfig = null

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return false when communication is null`() {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG_NO_COMMUNICATION

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return false when bootstrap type is not SSO`() {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG_NON_SSO

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return true when SSO type but cookie config is null`() {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG_SSO
        every { mockCookieDiskSource.getCookieConfig(HOSTNAME) } returns null

        val result = manager.needsBootstrap(HOSTNAME)

        assertTrue(result)
    }

    @Test
    fun `needsBootstrap should return true when SSO type and cookies are empty`() {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG_SSO
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns CookieConfigurationData(
            hostname = HOSTNAME,
            cookies = emptyList(),
        )

        val result = manager.needsBootstrap(HOSTNAME)

        assertTrue(result)
    }

    @Test
    fun `needsBootstrap should return false when SSO type and cookies are present`() {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG_SSO
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns COOKIE_CONFIG_WITH_COOKIES

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `getCookies should return empty list when cookie config is null`() {
        every { mockCookieDiskSource.getCookieConfig(HOSTNAME) } returns null

        val result = manager.getCookies(HOSTNAME)

        assertEquals(emptyList<NetworkCookie>(), result)
    }

    @Test
    fun `getCookies should return mapped cookies when config has cookies`() {
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns COOKIE_CONFIG_WITH_COOKIES

        val result = manager.getCookies(HOSTNAME)

        assertEquals(
            listOf(
                NetworkCookie(name = "awselb", value = "session123"),
                NetworkCookie(name = "awselbcors", value = "cors456"),
            ),
            result,
        )
    }

    @Test
    fun `getCookies should return empty list when config has empty cookies`() {
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns CookieConfigurationData(
            hostname = HOSTNAME,
            cookies = emptyList(),
        )

        val result = manager.getCookies(HOSTNAME)

        assertEquals(emptyList<NetworkCookie>(), result)
    }

    @Test
    fun `acquireCookies should set pending cookie acquisition with hostname`() {
        manager.acquireCookies(HOSTNAME)

        verify {
            mockCookieAcquisitionRequestManager.setPendingCookieAcquisition(
                CookieAcquisitionRequest(hostname = HOSTNAME),
            )
        }
    }
}

private const val HOSTNAME = "vault.bitwarden.com"
private const val BOOTSTRAP_TYPE_SSO = "ssoCookieVendor"

private val SERVER_CONFIG_NO_COMMUNICATION = ServerConfig(
    lastSync = 1698408000000L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = null,
        featureStates = null,
        communication = null,
    ),
)

private val SERVER_CONFIG_NON_SSO = ServerConfig(
    lastSync = 1698408000000L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = null,
        featureStates = null,
        communication = ConfigResponseJson.CommunicationJson(
            bootstrap = ConfigResponseJson.CommunicationJson.BootstrapJson(
                type = "other",
                idpLoginUrl = null,
                cookieName = null,
                cookieDomain = null,
            ),
        ),
    ),
)

private val SERVER_CONFIG_SSO = ServerConfig(
    lastSync = 1698408000000L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = null,
        featureStates = null,
        communication = ConfigResponseJson.CommunicationJson(
            bootstrap = ConfigResponseJson.CommunicationJson.BootstrapJson(
                type = BOOTSTRAP_TYPE_SSO,
                idpLoginUrl = null,
                cookieName = null,
                cookieDomain = null,
            ),
        ),
    ),
)

private val COOKIE_CONFIG_WITH_COOKIES = CookieConfigurationData(
    hostname = HOSTNAME,
    cookies = listOf(
        CookieConfigurationData.Cookie(name = "awselb", value = "session123"),
        CookieConfigurationData.Cookie(name = "awselbcors", value = "cors456"),
    ),
)
