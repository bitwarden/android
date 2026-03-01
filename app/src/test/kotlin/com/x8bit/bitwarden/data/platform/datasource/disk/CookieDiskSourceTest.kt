package com.x8bit.bitwarden.data.platform.datasource.disk

import com.bitwarden.core.di.CoreModule
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CookieDiskSourceTest {
    private val fakeEncryptedSharedPreferences = FakeSharedPreferences()
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val json = CoreModule.providesJson()

    private val cookieDiskSource: CookieDiskSource = CookieDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        encryptedSharedPreferences = fakeEncryptedSharedPreferences,
        json = json,
    )

    @Test
    fun `getCookieConfig should return null when no config exists`() {
        assertNull(cookieDiskSource.getCookieConfig("example.com"))
    }

    @Test
    fun `storeCookieConfig should persist config and getCookieConfig should retrieve it`() {
        val hostname = "vault.bitwarden.com"
        val config = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "BW_SESSION",
                    value = "encrypted_cookie_value",
                ),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname, config)

        val retrieved = cookieDiskSource.getCookieConfig(hostname)
        assertEquals(config, retrieved)
    }

    @Test
    fun `storeCookieConfig should update existing config`() {
        val hostname = "vault.bitwarden.com"
        val initialConfig = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "SESSION",
                    value = "initial_value",
                ),
            ),
        )

        val updatedConfig = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "SESSION",
                    value = "updated_value",
                ),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname, initialConfig)
        cookieDiskSource.storeCookieConfig(hostname, updatedConfig)

        val retrieved = cookieDiskSource.getCookieConfig(hostname)
        assertEquals(updatedConfig, retrieved)
    }

    @Test
    fun `storage should handle cookies with multiple values`() {
        val hostname = "vault.bitwarden.com"
        val config = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "BW_SESSION",
                    value = "session_value",
                ),
                CookieConfigurationData.Cookie(
                    name = "BW_REFRESH",
                    value = "refresh_value",
                ),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname, config)

        assertEquals(config, cookieDiskSource.getCookieConfig(hostname))
    }

    @Test
    fun `storeCookieConfig with null should remove stored config`() {
        val hostname = "vault.bitwarden.com"
        val config = CookieConfigurationData(
            hostname = hostname,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "SESSION",
                    value = "value",
                ),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname, config)
        cookieDiskSource.storeCookieConfig(hostname, null)

        assertNull(cookieDiskSource.getCookieConfig(hostname))
    }

    @Test
    fun `storeCookieConfig with null should not affect other hostnames`() {
        val hostname1 = "vault.bitwarden.com"
        val hostname2 = "other.bitwarden.com"
        val config1 = CookieConfigurationData(
            hostname = hostname1,
            cookies = listOf(
                CookieConfigurationData.Cookie(name = "A", value = "1"),
            ),
        )
        val config2 = CookieConfigurationData(
            hostname = hostname2,
            cookies = listOf(
                CookieConfigurationData.Cookie(name = "B", value = "2"),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname1, config1)
        cookieDiskSource.storeCookieConfig(hostname2, config2)
        cookieDiskSource.storeCookieConfig(hostname1, null)

        assertNull(cookieDiskSource.getCookieConfig(hostname1))
        assertEquals(config2, cookieDiskSource.getCookieConfig(hostname2))
    }

    @Test
    fun `storage should isolate configs by hostname`() {
        val hostname1 = "vault.bitwarden.com"
        val hostname2 = "other.bitwarden.com"
        val config1 = CookieConfigurationData(
            hostname = hostname1,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "A",
                    value = "1",
                ),
            ),
        )
        val config2 = CookieConfigurationData(
            hostname = hostname2,
            cookies = listOf(
                CookieConfigurationData.Cookie(
                    name = "B",
                    value = "2",
                ),
            ),
        )

        cookieDiskSource.storeCookieConfig(hostname1, config1)
        cookieDiskSource.storeCookieConfig(hostname2, config2)

        assertEquals(config1, cookieDiskSource.getCookieConfig(hostname1))
        assertEquals(config2, cookieDiskSource.getCookieConfig(hostname2))
    }
}
