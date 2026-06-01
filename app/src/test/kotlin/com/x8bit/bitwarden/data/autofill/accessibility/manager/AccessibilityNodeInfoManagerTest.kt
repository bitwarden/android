package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import com.x8bit.bitwarden.data.autofill.accessibility.util.isUsername
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessibilityNodeInfoManagerTest {

    private val accessibilityNodeInfoManager: AccessibilityNodeInfoManager =
        AccessibilityNodeInfoManagerImpl()

    @BeforeEach
    fun setup() {
        mockkStatic(AccessibilityNodeInfo::isUsername)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(AccessibilityNodeInfo::isUsername)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findAccessibilityNodeInfoList with the node matching the predicate should return that node`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo>()

        val result = accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
            rootNode = accessibilityNodeInfo,
            maxRecursionDepth = 100,
            predicate = { true },
        )

        assertEquals(listOf(accessibilityNodeInfo), result)
    }

    @Test
    fun `findAccessibilityNodeInfoList with a high recursion depth should return an empty list`() {
        // This node will always returns itself, so it should recur until it hits the max depth
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { childCount } returns 1
            every { getChild(any()) } returns this
        }

        val result = accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
            rootNode = accessibilityNodeInfo,
            maxRecursionDepth = 100,
            predicate = { false },
        )

        assertEquals(emptyList<AccessibilityNodeInfo>(), result)
    }

    @Test
    fun `findAccessibilityNodeInfoList where child node is null should return an empty list`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { childCount } returns 1
            every { getChild(any()) } returns null
        }

        val result = accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
            rootNode = accessibilityNodeInfo,
            maxRecursionDepth = 100,
            predicate = { false },
        )

        assertEquals(emptyList<AccessibilityNodeInfo>(), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findAccessibilityNodeInfoList with child nodes should return list of matching child nodes`() {
        val childChildAccessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns true
        }
        val childAccessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns false
            every { childCount } returns 3
            every { getChild(any()) } returns childChildAccessibilityNodeInfo
        }
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { isPassword } returns false
            every { childCount } returns 3
            every { getChild(any()) } returns childAccessibilityNodeInfo
        }

        val result = accessibilityNodeInfoManager.findAccessibilityNodeInfoList(
            rootNode = accessibilityNodeInfo,
            maxRecursionDepth = 100,
            predicate = { it.isPassword },
        )

        assertEquals(
            listOf(
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
                childChildAccessibilityNodeInfo,
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findUsernameAccessibilityNodeInfo with null uri path and no password fields should return null`() {
        val uri: Uri = mockk {
            every { path } returns null
        }

        val result = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
            uri = uri,
            allNodes = emptyList(),
            passwordNodes = emptyList(),
        )

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findUsernameAccessibilityNodeInfo with null uri path and a password field should return field above it`() {
        val uri: Uri = mockk {
            every { path } returns null
        }
        val usernameField = mockk<AccessibilityNodeInfo>()
        val passwordField = mockk<AccessibilityNodeInfo>()

        val result = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
            uri = uri,
            allNodes = listOf(usernameField, passwordField),
            passwordNodes = listOf(passwordField),
        )

        assertEquals(usernameField, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findUsernameAccessibilityNodeInfo with null uri authority and no possible username field should return null`() {
        val uri: Uri = mockk {
            every { path } returns "amazon/qa"
            every { authority } returns null
        }
        val passwordField = mockk<AccessibilityNodeInfo>()

        val result = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
            uri = uri,
            allNodes = listOf(passwordField),
            passwordNodes = listOf(passwordField),
        )

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findUsernameAccessibilityNodeInfo with known username field but no matches should return null`() {
        val uriPath = "/ap/signin"
        val uri: Uri = mockk {
            every { path } returns uriPath
            every { authority } returns "www.amazon.com"
        }
        val usernameField = mockk<AccessibilityNodeInfo> {
            every { isUsername(knownUsernameField = any(), uriPath = uriPath) } returns false
        }
        val passwordField = mockk<AccessibilityNodeInfo> {
            every { isUsername(knownUsernameField = any(), uriPath = uriPath) } returns false
        }

        val result = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
            uri = uri,
            allNodes = listOf(passwordField, usernameField),
            passwordNodes = listOf(passwordField),
        )

        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `findUsernameAccessibilityNodeInfo with known username field should return correct field`() {
        val uriPath = "/ap/signin"
        val uri: Uri = mockk {
            every { path } returns uriPath
            every { authority } returns "amazon.com"
        }
        val usernameField = mockk<AccessibilityNodeInfo> {
            every { isUsername(knownUsernameField = any(), uriPath = uriPath) } returns true
        }
        val passwordField = mockk<AccessibilityNodeInfo> {
            every { isUsername(knownUsernameField = any(), uriPath = uriPath) } returns false
        }

        val result = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
            uri = uri,
            allNodes = listOf(passwordField, usernameField),
            passwordNodes = listOf(passwordField),
        )

        assertEquals(usernameField, result)
    }
}
