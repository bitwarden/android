package com.x8bit.bitwarden.data.util

import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import com.x8bit.bitwarden.data.platform.manager.model.DomainName
import com.x8bit.bitwarden.data.platform.util.addSchemeToUriIfNecessary
import com.x8bit.bitwarden.data.platform.util.parseDomainNameOrNull
import com.x8bit.bitwarden.data.platform.util.parseDomainOrNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.URI

class UriExtensionsTest {
    private val resourceCacheManager: ResourceCacheManager = mockk {
        every { domainExceptionSuffixes } returns emptyList()
        every { domainNormalSuffixes } returns emptyList()
        every { domainWildCardSuffixes } returns emptyList()
    }

    @Test
    fun `parseDomainOrNull should return null when host is null`() {
        // Setup
        val uri: URI = mockk {
            every { host } returns null
        }

        // Test
        val actual = uri.parseDomainOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `parseDomainOrNull should return host when it is localhost`() {
        // Setup
        val expected = "localhost"
        val uri: URI = mockk {
            every { host } returns expected
        }

        // Test
        val actual = uri.parseDomainOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDomainOrNull should return host when it is ip address`() {
        // Setup
        val expected = "192.168.1.1"
        val uri: URI = mockk {
            every { host } returns expected
        }

        // Test
        val actual = uri.parseDomainOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDomainOrNull should return domain name when it matches exception suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            "example.co.uk",
            "example.co.uk",
            "example.uk",
            null,
        )
        every { resourceCacheManager.domainExceptionSuffixes } returns listOf("co.uk", "uk")

        // Test & Verify
        listOf(
            "example.co.uk",
            "sub.example.co.uk",
            "sub.example.uk",
            "sub.example.uk.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `parseDomainOrNull should return domain name when it matches normal suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            "example.co.uk",
            "example.co.uk",
            "example.uk",
            null,
        )
        every { resourceCacheManager.domainExceptionSuffixes } returns listOf("co.uk", "uk")

        // Test & Verify
        listOf(
            "example.co.uk",
            "sub.example.co.uk",
            "sub.example.uk",
            "sub.example.uk.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `parseDomainOrNull should return domain name when it matches wild card suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            "example.foo.compute.amazonaws.com",
            "example.foo.compute.amazonaws.com",
            "example.foo.amazonaws.com",
            null,
        )
        every {
            resourceCacheManager.domainWildCardSuffixes
        } returns listOf("compute.amazonaws.com", "amazonaws.com")

        // Test & Verify
        listOf(
            "sub.example.foo.compute.amazonaws.com",
            "bar.sub.example.foo.compute.amazonaws.com",
            "bar.sub.example.foo.amazonaws.com",
            "foo.sub.example.foo.amazonaws.com.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `parseDomainNameOrNull should return null when host is null`() {
        // Setup
        val uri: URI = mockk {
            every { host } returns null
        }

        // Test
        val actual = uri.parseDomainNameOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `parseDomainNameOrNull should return domain name when it matches exception suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            DomainName(
                secondLevelDomain = "example",
                subDomain = null,
                topLevelDomain = "co.uk",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "sub",
                topLevelDomain = "co.uk",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "sub",
                topLevelDomain = "uk",
            ),
            null,
        )
        every { resourceCacheManager.domainExceptionSuffixes } returns listOf("co.uk", "uk")

        // Test & Verify
        listOf(
            "example.co.uk",
            "sub.example.co.uk",
            "sub.example.uk",
            "sub.example.uk.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainNameOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `parseDomainNameOrNull should return domain name when it matches normal suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            DomainName(
                secondLevelDomain = "example",
                subDomain = null,
                topLevelDomain = "co.uk",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "sub",
                topLevelDomain = "co.uk",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "sub",
                topLevelDomain = "uk",
            ),
            null,
        )
        every { resourceCacheManager.domainExceptionSuffixes } returns listOf("co.uk", "uk")

        // Test & Verify
        listOf(
            "example.co.uk",
            "sub.example.co.uk",
            "sub.example.uk",
            "sub.example.uk.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainNameOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `parseDomainNameOrNull should return domain name when it matches wild card suffix`() {
        // Setup
        val uri: URI = mockk()
        val expected = listOf(
            DomainName(
                secondLevelDomain = "example",
                subDomain = "sub",
                topLevelDomain = "foo.compute.amazonaws.com",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "bar.sub",
                topLevelDomain = "foo.compute.amazonaws.com",
            ),
            DomainName(
                secondLevelDomain = "example",
                subDomain = "bar.sub",
                topLevelDomain = "foo.amazonaws.com",
            ),
            null,
        )
        every {
            resourceCacheManager.domainWildCardSuffixes
        } returns listOf("compute.amazonaws.com", "amazonaws.com")

        // Test & Verify
        listOf(
            "sub.example.foo.compute.amazonaws.com",
            "bar.sub.example.foo.compute.amazonaws.com",
            "bar.sub.example.foo.amazonaws.com",
            "bar.sub.example.foo.amazonaws.com.usa",
        )
            .forEachIndexed { index, host ->
                // Setup item test
                every { uri.host } returns host

                // Test
                val actual = uri.parseDomainNameOrNull(
                    resourceCacheManager = resourceCacheManager,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }

    @Test
    fun `addSchemeToUriIfNecessary should add https when missing`() {
        val uriWithNoScheme = URI("example.com")
        assertEquals(URI("https://example.com"), uriWithNoScheme.addSchemeToUriIfNecessary())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `addSchemeToUriIfNecessary should add https when https scheme is missing and a port is present`() {
        val uriWithPort = URI("example.com:8080")
        assertEquals(URI("https://example.com:8080"), uriWithPort.addSchemeToUriIfNecessary())
    }

    @Test
    fun `addSchemeToUriIfNecessary should not add https when http scheme is present`() {
        val uriWithHttpScheme = URI("http://example.com")
        assertEquals(URI("http://example.com"), uriWithHttpScheme.addSchemeToUriIfNecessary())
    }

    @Test
    fun `addSchemeToUriIfNecessary should not add https when https scheme is present`() {
        val uriWithHttpsScheme = URI("https://example.com")
        assertEquals(URI("https://example.com"), uriWithHttpsScheme.addSchemeToUriIfNecessary())
    }

    @Test
    fun `addSchemeToUriIfNecessary should not add https when custom scheme is already present`() {
        val uriWithCustomScheme = URI("bitwarden://example.com")
        assertEquals(
            URI("bitwarden://example.com"),
            uriWithCustomScheme.addSchemeToUriIfNecessary(),
        )
    }
}
