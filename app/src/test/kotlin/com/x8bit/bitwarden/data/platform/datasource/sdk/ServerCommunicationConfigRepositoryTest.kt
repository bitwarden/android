package com.x8bit.bitwarden.data.platform.datasource.sdk

import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.datasource.disk.util.FakeConfigDiskSource
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.bitwarden.servercommunicationconfig.BootstrapConfig
import com.bitwarden.servercommunicationconfig.ServerCommunicationConfig
import com.bitwarden.servercommunicationconfig.SsoCookieVendorConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.ServerCommunicationConfigRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerCommunicationConfigRepositoryTest {

    private val cookieDiskSource: CookieDiskSource = mockk()
    private val configDiskSource: FakeConfigDiskSource = FakeConfigDiskSource()

    private val repository = ServerCommunicationConfigRepositoryImpl(
        cookieDiskSource = cookieDiskSource,
        configDiskSource = configDiskSource,
    )

    @Test
    fun `get returns null when communication config not present`() = runTest {
        val hostname = "vault.bitwarden.com"

        val result = repository.get(hostname)

        assertNull(result)
    }

    @Test
    fun `get returns ServerCommunicationConfig with cookies when config exists`() = runTest {
        val hostname = "vault.bitwarden.com"
        val idpLoginUrl = "https://idp.example.com/login"
        val cookieName = "session"
        val cookieDomain = ".example.com"
        configDiskSource.serverConfig = ServerConfig(
            lastSync = 0L,
            serverData = ConfigResponseJson(
                type = null,
                version = null,
                gitHash = null,
                server = null,
                environment = null,
                featureStates = null,
                communication = ConfigResponseJson.CommunicationJson(
                    bootstrap = ConfigResponseJson.CommunicationJson.BootstrapJson(
                        type = "ssoCookieVendor",
                        idpLoginUrl = idpLoginUrl,
                        cookieName = cookieName,
                        cookieDomain = cookieDomain,
                    ),
                ),
            ),
        )
        val cookieData = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie("session", "abc123"),
                CookieConfigurationData.Cookie("csrf", "def456"),
            ),
        )
        every { cookieDiskSource.getCookieConfig(hostname) } returns cookieData

        val result = repository.get(hostname)

        assertEquals(
            ServerCommunicationConfig(
                bootstrap = BootstrapConfig.SsoCookieVendor(
                    v1 = SsoCookieVendorConfig(
                        idpLoginUrl = idpLoginUrl,
                        cookieName = cookieName,
                        cookieDomain = cookieDomain,
                        cookieValue = listOf(
                            AcquiredCookie(name = "session", value = "abc123"),
                            AcquiredCookie(name = "csrf", value = "def456"),
                        ),
                    ),
                ),
            ),
            result,
        )
        coVerify { cookieDiskSource.getCookieConfig(hostname) }
    }

    @Test
    fun `get returns Direct when bootstrap type is not ssoCookieVendor`() = runTest {
        val hostname = "vault.bitwarden.com"
        configDiskSource.serverConfig = ServerConfig(
            lastSync = 0L,
            serverData = ConfigResponseJson(
                type = null,
                version = null,
                gitHash = null,
                server = null,
                environment = null,
                featureStates = null,
                communication = ConfigResponseJson.CommunicationJson(
                    bootstrap = ConfigResponseJson.CommunicationJson.BootstrapJson(
                        type = "direct",
                        idpLoginUrl = null,
                        cookieName = null,
                        cookieDomain = null,
                    ),
                ),
            ),
        )

        val result = repository.get(hostname)

        assertEquals(
            ServerCommunicationConfig(bootstrap = BootstrapConfig.Direct),
            result,
        )
    }

    @Test
    fun `save converts ServerCommunicationConfig to CookieConfigurationData and stores`() =
        runTest {
            val hostname = "vault.bitwarden.com"
            val config = ServerCommunicationConfig(
                bootstrap = BootstrapConfig.SsoCookieVendor(
                    v1 = SsoCookieVendorConfig(
                        idpLoginUrl = "https://$hostname/proxy-cookie-redirect-connector",
                        cookieName = "session",
                        cookieDomain = hostname,
                        cookieValue = listOf(
                            AcquiredCookie(name = "session", value = "xyz789"),
                            AcquiredCookie(name = "token", value = "uvw456"),
                        ),
                    ),
                ),
            )
            coEvery { cookieDiskSource.storeCookieConfig(any(), any()) } just runs

            repository.save(hostname, config)

            coVerify {
                cookieDiskSource.storeCookieConfig(
                    hostname,
                    CookieConfigurationData(
                        hostname = hostname,
                        cookies = listOf(
                            CookieConfigurationData.Cookie("session", "xyz789"),
                            CookieConfigurationData.Cookie("token", "uvw456"),
                        ),
                    ),
                )
            }
        }

    @Test
    fun `save deletes cookie config when bootstrap type is Direct`() =
        runTest {
            val hostname = "vault.bitwarden.com"
            val config = ServerCommunicationConfig(bootstrap = BootstrapConfig.Direct)
            every { cookieDiskSource.deleteCookieConfig(any()) } just runs

            repository.save(hostname, config)

            coVerify {
                cookieDiskSource.deleteCookieConfig(hostname)
            }
        }

    @Test
    fun `save handles ServerCommunicationConfig with null cookieValue`() = runTest {
        val hostname = "vault.bitwarden.com"
        val config = ServerCommunicationConfig(
            bootstrap = BootstrapConfig.SsoCookieVendor(
                v1 = SsoCookieVendorConfig(
                    idpLoginUrl = "https://$hostname/proxy-cookie-redirect-connector",
                    cookieName = "session",
                    cookieDomain = hostname,
                    cookieValue = null,
                ),
            ),
        )
        coEvery { cookieDiskSource.storeCookieConfig(any(), any()) } just runs

        repository.save(hostname, config)

        coVerify {
            cookieDiskSource.storeCookieConfig(
                hostname,
                CookieConfigurationData(
                    hostname = hostname,
                    cookies = emptyList(),
                ),
            )
        }
    }
}
