package com.x8bit.bitwarden.data.platform.manager.provider

import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.platform.datasource.disk.FakeEnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.provider.BaseUrlsProviderImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BaseUrlsProviderTest {

    private val fakeEnvironmentDiskSource = FakeEnvironmentDiskSource()
    private val baseUrlsManager = BaseUrlsProviderImpl(
        environmentDiskSource = fakeEnvironmentDiskSource,
    )

    @Test
    fun `getBaseApiUrl should return correct api URL when preAuthEnvironmentUrlData is set`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = Environment.Eu.environmentUrlData
        Assertions.assertEquals(
            "https://vault.bitwarden.eu/api",
            baseUrlsManager.getBaseApiUrl(),
        )
    }

    @Test
    fun `getBaseApiUrl should return default value when preAuthEnvironmentUrlData is null`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null
        Assertions.assertEquals(
            "https://vault.bitwarden.com/api",
            baseUrlsManager.getBaseApiUrl(),
        )
    }

    @Test
    fun `getBaseIdentityUrl should return correct api URL when preAuthEnvironmentUrlData is set`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = Environment.Eu.environmentUrlData
        Assertions.assertEquals(
            "https://vault.bitwarden.eu/identity",
            baseUrlsManager.getBaseIdentityUrl(),
        )
    }

    @Test
    fun `getBaseIdentityUrl should return default value when preAuthEnvironmentUrlData is null`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null
        Assertions.assertEquals(
            "https://vault.bitwarden.com/identity",
            baseUrlsManager.getBaseIdentityUrl(),
        )
    }

    @Test
    fun `getBaseEventsUrl should return correct api URL when preAuthEnvironmentUrlData is set`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = Environment.Eu.environmentUrlData
        Assertions.assertEquals(
            "https://vault.bitwarden.eu/events",
            baseUrlsManager.getBaseEventsUrl(),
        )
    }

    @Test
    fun `getBaseEventsUrl should return default value when preAuthEnvironmentUrlData is null`() {
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null
        Assertions.assertEquals(
            "https://vault.bitwarden.com/events",
            baseUrlsManager.getBaseEventsUrl(),
        )
    }
}
