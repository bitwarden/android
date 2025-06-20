package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ViewNodeTraversalDataExtensionsTest {
    private val windowNode: AssistStructure.WindowNode = mockk()
    private val assistStructure: AssistStructure = mockk {
        every { this@mockk.windowNodeCount } returns 1
        every { this@mockk.getWindowNodeAt(0) } returns windowNode
    }

    @Test
    fun `buildUriOrNull should return website URI when present`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = null,
            ignoreAutofillIds = emptyList(),
            website = WEBSITE,
        )

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertEquals(WEBSITE, actual)
    }

    @Test
    fun `buildUriOrNull should return package name URI when website is null`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = null,
            ignoreAutofillIds = emptyList(),
            website = null,
        )
        val expected = "androidapp://$PACKAGE_NAME"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `buildUriOrNull should return null when website and packageName are null`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = null,
            ignoreAutofillIds = emptyList(),
            website = null,
        )

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            packageName = null,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `buildPackageNameOrNull should return idPackage when available`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = ID_PACKAGE,
            ignoreAutofillIds = emptyList(),
            website = null,
        )

        // Test
        val actual = listOf(viewNodeTraversalData).buildPackageNameOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(ID_PACKAGE, actual)
    }

    @Test
    fun `buildPackageNameOrNull should return title URI when idPackage is null`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = null,
            ignoreAutofillIds = emptyList(),
            website = null,
        )
        val expected = "com.x8bit.bitwarden"
        every { windowNode.title } returns "com.x8bit.bitwarden/path.deeper.into.app"

        // Test
        val actual = listOf(viewNodeTraversalData).buildPackageNameOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }
}

private const val ID_PACKAGE: String = "com.x8bit.bitwarden"
private const val PACKAGE_NAME: String = "com.google"
private const val WEBSITE: String = "https://www.google.com"
