package com.x8bit.bitwarden.data.autofill.accessibility.parser

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.bitwarden.ui.platform.base.BaseRobolectricTest
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityNodeInfoManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.Browser
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessibilityParserTest : BaseRobolectricTest() {

    private val accessibilityNodeInfoManager: AccessibilityNodeInfoManager = mockk()

    private val accessibilityParser: AccessibilityParser = AccessibilityParserImpl(
        accessibilityNodeInfoManager = accessibilityNodeInfoManager,
    )

    @Test
    fun `parseForFillableFields with no accessibility nodes should return empty data`() {
        val uri = mockk<Uri>()
        val rootNode = mockk<AccessibilityNodeInfo>()
        every {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = rootNode,
                predicate = any(),
            )
        } returns emptyList()
        every {
            accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
                uri = uri,
                allNodes = emptyList(),
                passwordNodes = emptyList(),
            )
        } returns null
        val expectedResult = FillableFields(
            usernameField = null,
            passwordFields = emptyList(),
        )

        val result = accessibilityParser.parseForFillableFields(
            rootNode = rootNode,
            uri = uri,
        )

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForFillableFields with password fields should return no username field and all password fields`() {
        val uri = mockk<Uri>()
        val rootNode = mockk<AccessibilityNodeInfo>()
        val passwordField = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns true
        }
        val allFields = listOf(passwordField, passwordField)
        every {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = rootNode,
                predicate = any(),
            )
        } returns allFields
        every {
            accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
                uri = uri,
                allNodes = allFields,
                passwordNodes = allFields,
            )
        } returns null
        val expectedResult = FillableFields(
            usernameField = null,
            passwordFields = allFields,
        )

        val result = accessibilityParser.parseForFillableFields(
            rootNode = rootNode,
            uri = uri,
        )

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForFillableFields with password fields and username field should return username field and all password fields`() {
        val uri = mockk<Uri>()
        val rootNode = mockk<AccessibilityNodeInfo>()
        val usernameField = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns false
        }
        val passwordField = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns true
        }
        val allFields = listOf(usernameField, passwordField, passwordField)
        val passwordFields = listOf(passwordField, passwordField)
        every {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = rootNode,
                predicate = any(),
            )
        } returns allFields
        every {
            accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
                uri = uri,
                allNodes = allFields,
                passwordNodes = passwordFields,
            )
        } returns usernameField
        val expectedResult = FillableFields(
            usernameField = usernameField,
            passwordFields = passwordFields,
        )

        val result = accessibilityParser.parseForFillableFields(
            rootNode = rootNode,
            uri = uri,
        )

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
        val testPackageName = "com.android.chrome"
        val testBrowser = Browser(packageName = testPackageName, urlFieldId = "url_bar")
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$testPackageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns emptyList()
        }

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return null when URL bar node has no text or content description`() {
        val testPackageName = "com.android.chrome"
        val testBrowser = Browser(packageName = testPackageName, urlFieldId = "url_bar")
        val emptyNode = mockk<AccessibilityNodeInfo> {
            every { text } returns null
            every { contentDescription } returns null
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$testPackageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns listOf(emptyNode)
        }

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertNull(result)
    }

    @Test
    fun `parseForUriOrPackageName should not use semantic lookup when standard lookup succeeds`() {
        val firefoxPackage = "org.mozilla.firefox"
        val url = "https://www.reddit.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns firefoxPackage
            every { findAccessibilityNodeInfosByViewId(any()) } returns emptyList()
            every {
                findAccessibilityNodeInfosByViewId(
                    "$firefoxPackage:id/mozac_browser_toolbar_url_view",
                )
            } returns listOf(urlNode)
        }

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(url.toUri(), result)
        verify(exactly = 0) {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = any(),
                predicate = any(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return null when package is a supported browser with semantic ids and no URL bar is found`() {
        val firefoxPackage = "org.mozilla.firefox"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns firefoxPackage
            every { findAccessibilityNodeInfosByViewId(any()) } returns emptyList()
        }
        every {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = rootNode,
                predicate = any(),
            )
        } returns emptyList()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the site url from content description when URL bar is found via semantic id`() {
        val firefoxPackage = "org.mozilla.firefox"
        val contentDesc = " www.reddit.com. Search or enter address"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns null
            every { contentDescription } returns contentDesc
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns firefoxPackage
            every { findAccessibilityNodeInfosByViewId(any()) } returns emptyList()
        }
        every {
            accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
                rootNode = rootNode,
                predicate = any(),
            )
        } returns listOf(urlNode)
        val expectedResult = Uri.parse("https://www.reddit.com")

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parseForUriOrPackageName should return the site url un-augmented with https protocol as a URI when package is a supported browser and URL is found`() {
        val testPackageName = "com.android.chrome"
        val testBrowser = Browser(packageName = testPackageName, urlFieldId = "url_bar")
        val url = "https://www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$testPackageName:id/${testBrowser.possibleUrlFieldIds.first()}",
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
        val testPackageName = "com.android.chrome"
        val testBrowser = Browser(packageName = testPackageName, urlFieldId = "url_bar")
        val url = "http://www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$testPackageName:id/${testBrowser.possibleUrlFieldIds.first()}",
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
        val testPackageName = "com.android.chrome"
        val testBrowser = Browser(packageName = testPackageName, urlFieldId = "url_bar")
        val url = "www.google.com"
        val urlNode = mockk<AccessibilityNodeInfo> {
            every { text } returns url
        }
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
            every {
                findAccessibilityNodeInfosByViewId(
                    "$testPackageName:id/${testBrowser.possibleUrlFieldIds.first()}",
                )
            } returns listOf(urlNode)
        }
        val expectedResult = "https://$url".toUri()

        val result = accessibilityParser.parseForUriOrPackageName(rootNode = rootNode)

        assertEquals(expectedResult, result)
    }
}
