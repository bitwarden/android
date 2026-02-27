package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.datasource.disk.util.FakeConfigDiskSource
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.model.NetworkCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
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
    private val mockResourceCacheManager: ResourceCacheManager = mockk {
        every { domainExceptionSuffixes } returns emptyList()
        every { domainNormalSuffixes } returns listOf("com")
        every { domainWildCardSuffixes } returns emptyList()
    }

    private val manager = NetworkCookieManagerImpl(
        configDiskSource = fakeConfigDiskSource,
        cookieDiskSource = mockCookieDiskSource,
        cookieAcquisitionRequestManager = mockCookieAcquisitionRequestManager,
        resourceCacheManager = mockResourceCacheManager,
    )

    @Test
    fun `needsBootstrap should return false when serverConfig is null`() {
        fakeConfigDiskSource.serverConfig = null

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return false when communication is null`() {
        fakeConfigDiskSource.serverConfig = createServerConfig()

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return false when bootstrap type is not SSO`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = "other")

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return true when SSO type but cookie config is null`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = BOOTSTRAP_TYPE_SSO)
        every { mockCookieDiskSource.getCookieConfig(any()) } returns null

        val result = manager.needsBootstrap(HOSTNAME)

        assertTrue(result)
    }

    @Test
    fun `needsBootstrap should return true when SSO type and cookies are empty`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = BOOTSTRAP_TYPE_SSO)
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns createCookieConfig(hostname = HOSTNAME, cookies = emptyList())

        val result = manager.needsBootstrap(HOSTNAME)

        assertTrue(result)
    }

    @Test
    fun `needsBootstrap should return false when SSO type and cookies are present`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = BOOTSTRAP_TYPE_SSO)
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns createCookieConfig(hostname = HOSTNAME)

        val result = manager.needsBootstrap(HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `getCookies should return empty list when cookie config is null`() {
        every { mockCookieDiskSource.getCookieConfig(any()) } returns null

        val result = manager.getCookies(HOSTNAME)

        assertEquals(emptyList<NetworkCookie>(), result)
    }

    @Test
    fun `getCookies should return mapped cookies when config has cookies`() {
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns createCookieConfig(hostname = HOSTNAME)

        val result = manager.getCookies(HOSTNAME)

        assertEquals(EXPECTED_NETWORK_COOKIES, result)
    }

    @Test
    fun `getCookies should return empty list when config has empty cookies`() {
        every {
            mockCookieDiskSource.getCookieConfig(HOSTNAME)
        } returns createCookieConfig(hostname = HOSTNAME, cookies = emptyList())

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

    @Test
    fun `getCookies should resolve subdomain to cookie domain`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(
            bootstrapType = BOOTSTRAP_TYPE_SSO,
            cookieDomain = COOKIE_DOMAIN,
        )
        every { mockCookieDiskSource.getCookieConfig(SUBDOMAIN_HOSTNAME) } returns null
        every {
            mockCookieDiskSource.getCookieConfig(COOKIE_DOMAIN)
        } returns createCookieConfig(hostname = COOKIE_DOMAIN)

        val result = manager.getCookies(SUBDOMAIN_HOSTNAME)

        assertEquals(EXPECTED_NETWORK_COOKIES, result)
    }

    @Test
    fun `needsBootstrap should return false when subdomain resolves to domain with cookies`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(
            bootstrapType = BOOTSTRAP_TYPE_SSO,
            cookieDomain = COOKIE_DOMAIN,
        )
        every { mockCookieDiskSource.getCookieConfig(SUBDOMAIN_HOSTNAME) } returns null
        every {
            mockCookieDiskSource.getCookieConfig(COOKIE_DOMAIN)
        } returns createCookieConfig(hostname = COOKIE_DOMAIN)

        val result = manager.needsBootstrap(SUBDOMAIN_HOSTNAME)

        assertFalse(result)
    }

    @Test
    fun `needsBootstrap should return true when subdomain resolves to domain without cookies`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(
            bootstrapType = BOOTSTRAP_TYPE_SSO,
            cookieDomain = COOKIE_DOMAIN,
        )
        every { mockCookieDiskSource.getCookieConfig(any()) } returns null

        val result = manager.needsBootstrap(SUBDOMAIN_HOSTNAME)

        assertTrue(result)
    }

    @Test
    fun `storeCookies should store under cookieDomain when present`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(
            bootstrapType = BOOTSTRAP_TYPE_SSO,
            cookieDomain = COOKIE_DOMAIN,
        )
        every { mockCookieDiskSource.storeCookieConfig(any(), any()) } just runs

        manager.storeCookies(
            hostname = SUBDOMAIN_HOSTNAME,
            cookies = mapOf("awselb" to "session123"),
        )

        verify {
            mockCookieDiskSource.storeCookieConfig(
                hostname = COOKIE_DOMAIN,
                config = CookieConfigurationData(
                    hostname = COOKIE_DOMAIN,
                    cookies = listOf(
                        CookieConfigurationData.Cookie(
                            name = "awselb",
                            value = "session123",
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `storeCookies should store under parsed base domain when cookieDomain is null`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = BOOTSTRAP_TYPE_SSO)
        every { mockCookieDiskSource.storeCookieConfig(any(), any()) } just runs

        manager.storeCookies(
            hostname = SUBDOMAIN_HOSTNAME,
            cookies = mapOf("awselb" to "session123"),
        )

        verify {
            mockCookieDiskSource.storeCookieConfig(
                hostname = COOKIE_DOMAIN,
                config = CookieConfigurationData(
                    hostname = COOKIE_DOMAIN,
                    cookies = listOf(
                        CookieConfigurationData.Cookie(
                            name = "awselb",
                            value = "session123",
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `storeCookies should store under hostname when cookieDomain is null and parsing fails`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(bootstrapType = BOOTSTRAP_TYPE_SSO)
        every { mockResourceCacheManager.domainNormalSuffixes } returns emptyList()
        every { mockCookieDiskSource.storeCookieConfig(any(), any()) } just runs

        manager.storeCookies(
            hostname = HOSTNAME,
            cookies = mapOf("awselb" to "session123"),
        )

        verify {
            mockCookieDiskSource.storeCookieConfig(
                hostname = HOSTNAME,
                config = CookieConfigurationData(
                    hostname = HOSTNAME,
                    cookies = listOf(
                        CookieConfigurationData.Cookie(
                            name = "awselb",
                            value = "session123",
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `storeCookies should prefer cookieDomain over parsed base domain`() {
        fakeConfigDiskSource.serverConfig = createServerConfig(
            bootstrapType = BOOTSTRAP_TYPE_SSO,
            cookieDomain = COOKIE_DOMAIN,
        )
        every { mockCookieDiskSource.storeCookieConfig(any(), any()) } just runs

        manager.storeCookies(
            hostname = SUBDOMAIN_HOSTNAME,
            cookies = mapOf("awselb" to "session123"),
        )

        verify {
            mockCookieDiskSource.storeCookieConfig(
                hostname = COOKIE_DOMAIN,
                config = CookieConfigurationData(
                    hostname = COOKIE_DOMAIN,
                    cookies = listOf(
                        CookieConfigurationData.Cookie(
                            name = "awselb",
                            value = "session123",
                        ),
                    ),
                ),
            )
        }
    }
}

private const val HOSTNAME = "vault.bitwarden.com"
private const val COOKIE_DOMAIN = "bitwarden.com"
private const val SUBDOMAIN_HOSTNAME = "api.bitwarden.com"
private const val BOOTSTRAP_TYPE_SSO = "ssoCookieVendor"

private val DEFAULT_COOKIES = listOf(
    CookieConfigurationData.Cookie(name = "awselb", value = "session123"),
    CookieConfigurationData.Cookie(name = "awselbcors", value = "cors456"),
)

private val EXPECTED_NETWORK_COOKIES = listOf(
    NetworkCookie(name = "awselb", value = "session123"),
    NetworkCookie(name = "awselbcors", value = "cors456"),
)

private fun createServerConfig(
    bootstrapType: String? = null,
    cookieDomain: String? = null,
): ServerConfig = ServerConfig(
    lastSync = 1698408000000L,
    serverData = ConfigResponseJson(
        type = null,
        version = null,
        gitHash = null,
        server = null,
        environment = null,
        featureStates = null,
        communication = bootstrapType?.let {
            ConfigResponseJson.CommunicationJson(
                bootstrap = ConfigResponseJson.CommunicationJson.BootstrapJson(
                    type = it,
                    idpLoginUrl = null,
                    cookieName = null,
                    cookieDomain = cookieDomain,
                ),
            )
        },
    ),
)

private fun createCookieConfig(
    hostname: String,
    cookies: List<CookieConfigurationData.Cookie> = DEFAULT_COOKIES,
): CookieConfigurationData = CookieConfigurationData(
    hostname = hostname,
    cookies = cookies,
)
