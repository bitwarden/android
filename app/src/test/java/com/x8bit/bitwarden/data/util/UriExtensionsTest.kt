package com.x8bit.bitwarden.data.util

import android.content.Context
import android.content.res.Resources
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.DomainName
import com.x8bit.bitwarden.data.platform.util.parseDomainNameOrNull
import com.x8bit.bitwarden.data.platform.util.parseDomainOrNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.URI

class UriExtensionsTest {
    private val resources: Resources = mockk {
        every { getStringArray(R.array.exception_suffixes) } returns emptyArray()
        every { getStringArray(R.array.normal_suffixes) } returns emptyArray()
        every { getStringArray(R.array.wild_card_suffixes) } returns emptyArray()
    }
    private val context: Context = mockk {
        every { this@mockk.resources } returns this@UriExtensionsTest.resources
    }

    @Test
    fun `parseDomainOrNull should return null when host is null`() {
        // Setup
        val uri: URI = mockk {
            every { host } returns null
        }

        // Test
        val actual = uri.parseDomainOrNull(
            context = context,
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
            context = context,
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
            context = context,
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
        every {
            resources.getStringArray(R.array.exception_suffixes)
        } returns arrayOf("co.uk", "uk")

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
                    context = context,
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
        every { resources.getStringArray(R.array.exception_suffixes) } returns arrayOf(
            "co.uk",
            "uk",
        )

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
                    context = context,
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
        every { resources.getStringArray(R.array.wild_card_suffixes) } returns arrayOf(
            "compute.amazonaws.com",
            "amazonaws.com",
        )

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
                    context = context,
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
            context = context,
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
        every { resources.getStringArray(R.array.exception_suffixes) } returns arrayOf(
            "co.uk",
            "uk",
        )

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
                    context = context,
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
        every { resources.getStringArray(R.array.exception_suffixes) } returns arrayOf(
            "co.uk",
            "uk",
        )

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
                    context = context,
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
        every { resources.getStringArray(R.array.wild_card_suffixes) } returns arrayOf(
            "compute.amazonaws.com",
            "amazonaws.com",
        )

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
                    context = context,
                )

                // Verify
                assertEquals(expected[index], actual)
            }
    }
}
