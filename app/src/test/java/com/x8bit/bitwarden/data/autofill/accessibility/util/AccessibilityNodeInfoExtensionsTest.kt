package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityNodeInfoExtensionsTest {

    @Test
    fun `shouldSkipPackage when packageName is null should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns null
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Test
    fun `shouldSkipPackage when packageName is blank should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns ""
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Test
    fun `shouldSkipPackage when packageName is system UI package should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.android.systemui"
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldSkipPackage when packageName is prefixed with bitwarden package should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.x8bit.bitwarden.beta"
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Test
    fun `shouldSkipPackage when packageName contains with 'launcher' should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.android.launcher.foo"
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Test
    fun `shouldSkipPackage when packageName is blocked package should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.google.android.googlequicksearchbox"
        }

        assertTrue(accessibilityNodeInfo.shouldSkipPackage)
    }

    @Test
    fun `shouldSkipPackage when packageName is valid should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.another.app"
        }

        assertFalse(accessibilityNodeInfo.shouldSkipPackage)
    }
}
