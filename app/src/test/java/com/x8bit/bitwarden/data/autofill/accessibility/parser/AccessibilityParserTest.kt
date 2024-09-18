package com.x8bit.bitwarden.data.autofill.accessibility.parser

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.autofill.accessibility.model.Browser
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccessibilityParserTest {

    private val accessibilityParser: AccessibilityParser = AccessibilityParserImpl()

    @Test
    fun `parseForFillableFields should return empty data`() {
        val rootNode = mockk<AccessibilityNodeInfo>()
        val expectedResult = FillableFields(
            usernameFields = emptyList(),
            passwordFields = emptyList(),
        )

        val result = accessibilityParser.parseForFillableFields(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the package name as a URI when not a supported browser`() {
        val testPackageName = "testPackageName"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val expectedResult = "androidapp://$testPackageName".toUri()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return null when package is a supported browser and URL bar is not found`() {
        val testBrowser = Browser(packageName = "com.android.chrome", urlFieldId = "url_bar")
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testBrowser.packageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$packageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns emptyList()
        }

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the site url un-augmented with https protocol as a URI when package is a supported browser and URL is found`() {
        val testBrowser = Browser(packageName = "com.android.chrome", urlFieldId = "url_bar")
        val url = "https://www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testBrowser.packageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$packageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns listOf(urlNode)
        }
        val expectedResult = url.toUri()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the site url un-augmented with http protocol as a URI when package is a supported browser and URL is found`() {
        val testBrowser = Browser(packageName = "com.android.chrome", urlFieldId = "url_bar")
        val url = "http://www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testBrowser.packageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$packageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns listOf(urlNode)
        }
        val expectedResult = url.toUri()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the site url augmented with https protocol as a URI when package is a supported browser and URL is found`() {
        val testBrowser = Browser(packageName = "com.android.chrome", urlFieldId = "url_bar")
        val url = "www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testBrowser.packageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$packageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns listOf(urlNode)
        }
        val expectedResult = "https://$url".toUri()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }
}
