package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.view.accessibility.AccessibilityNodeInfo
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessOptions
import com.x8bit.bitwarden.data.autofill.accessibility.model.KnownUsernameField
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityNodeInfoExtensionsTest {

    @Test
    fun `isUsername without uri match should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo>()

        val result = accessibilityNodeInfo.isUsername(
            knownUsernameField = MOCK_KNOWN_USERNAME_FIELD,
            uriPath = "",
        )

        assertFalse(result)
    }

    @Test
    fun `isUsername with uri match and no matching viewId should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { viewIdResourceName } returns ""
        }

        val result = accessibilityNodeInfo.isUsername(
            knownUsernameField = MOCK_KNOWN_USERNAME_FIELD,
            uriPath = MOCK_MATCH_VALUE,
        )

        assertFalse(result)
    }

    @Test
    fun `isUsername with uri match and matching viewId should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { viewIdResourceName } returns MOCK_USERNAME_VIEW_ID
        }

        val result = accessibilityNodeInfo.isUsername(
            knownUsernameField = MOCK_KNOWN_USERNAME_FIELD,
            uriPath = MOCK_MATCH_VALUE,
        )

        assertTrue(result)
    }

    @Test
    fun `isEditText when className is null should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns null
        }

        assertFalse(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isEditText when className does not contain 'EditText' should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns "TextView"
        }

        assertFalse(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isEditText when className is an EditText should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns "android.widget.EditText"
        }

        assertTrue(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isEditText when className is assignable to 'EditText' should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns "android.widget.AutoCompleteTextView"
        }

        assertTrue(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isEditText when className does contains 'EditText' should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns "com.EditText"
        }

        assertTrue(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isEditText when className is exactly 'EditText' should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { className } returns "EditText"
        }

        assertTrue(accessibilityNodeInfo.isEditText)
    }

    @Test
    fun `isSystemPackage when packageName is null should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns null
        }

        assertFalse(accessibilityNodeInfo.isSystemPackage)
    }

    @Test
    fun `isSystemPackage when packageName is blank should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns ""
        }

        assertFalse(accessibilityNodeInfo.isSystemPackage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isSystemPackage when packageName is populated with non system UI package should return false`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.x8bit.bitwarden.beta"
        }

        assertFalse(accessibilityNodeInfo.isSystemPackage)
    }

    @Test
    fun `isSystemPackage when packageName is system UI package should return true`() {
        val accessibilityNodeInfo = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "com.android.systemui"
        }

        assertTrue(accessibilityNodeInfo.isSystemPackage)
    }

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

private const val MOCK_MATCH_VALUE: String = "/ap/signin"
private const val MOCK_USERNAME_VIEW_ID: String = "ap_email"
private val MOCK_KNOWN_USERNAME_FIELD: KnownUsernameField = KnownUsernameField(
    uriAuthority = "amazon.com",
    accessOption = AccessOptions(
        matchValue = MOCK_MATCH_VALUE,
        matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
        usernameViewIds = listOf("ap_email_login", MOCK_USERNAME_VIEW_ID),
    ),
)
