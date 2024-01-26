package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
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
    private val autofillViewData = AutofillView.Data(
        autofillId = mockk(),
        isFocused = false,
    )

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
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(WEBSITE, actual)
    }

    @Test
    fun `buildUriOrNull should return idPackage URI when WEBSITE is null`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = ID_PACKAGE,
            ignoreAutofillIds = emptyList(),
            website = null,
        )
        val expected = "androidapp://$ID_PACKAGE"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `buildUriOrNull should return title URI when website and idPackage are null`() {
        // Setup
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = emptyList(),
            idPackage = null,
            ignoreAutofillIds = emptyList(),
            website = null,
        )
        val expected = "androidapp://com.x8bit.bitwarden"
        every { windowNode.title } returns "com.x8bit.bitwarden/path.deeper.into.app"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }

    companion object {
        private const val ID_PACKAGE: String = "com.x8bit.bitwarden"
        private const val WEBSITE: String = "https://www.google.com"
    }
}
