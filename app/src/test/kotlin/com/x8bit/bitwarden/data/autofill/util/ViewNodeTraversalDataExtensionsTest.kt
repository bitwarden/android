package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViewNodeTraversalDataExtensionsTest {
    private val windowNode: AssistStructure.WindowNode = mockk()
    private val assistStructure: AssistStructure = mockk {
        every { this@mockk.windowNodeCount } returns 1
        every { this@mockk.getWindowNodeAt(0) } returns windowNode
    }

    @Test
    fun `buildPackageNameOrNull should return idPackage when available`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = ID_PACKAGE,
            ignoreAutofillIds = emptyList(),
            urlBarWebsites = emptyList(),
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
            urlBarWebsites = emptyList(),
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
