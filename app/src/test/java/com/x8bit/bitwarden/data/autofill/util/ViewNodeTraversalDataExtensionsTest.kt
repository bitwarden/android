package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import com.x8bit.bitwarden.data.autofill.model.AutofillView
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
    private val autofillViewData = AutofillView.Data(
        autofillId = mockk(),
        idPackage = null,
        isFocused = false,
        webDomain = null,
        webScheme = null,
    )

    @Test
    fun `buildUriOrNull should return URI when contains valid domain and scheme`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                webDomain = WEB_DOMAIN,
                webScheme = WEB_SCHEME,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
        )
        val expected = "$WEB_SCHEME://$WEB_DOMAIN"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `buildUriOrNull should return URI with default scheme when domain valid and scheme null`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                webDomain = WEB_DOMAIN,
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
        )
        val expected = "https://$WEB_DOMAIN"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildUriOrNull should return URI with default scheme when domain valid and scheme empty`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                webDomain = WEB_DOMAIN,
                webScheme = "",
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
        )
        val expected = "https://$WEB_DOMAIN"

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `buildUriOrNull should return idPackage URI when domain is null`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = ID_PACKAGE,
                webDomain = null,
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
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
    fun `buildUriOrNull should return idPackage URI when domain is empty`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = ID_PACKAGE,
                webDomain = "",
                webScheme = "",
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
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
    fun `buildUriOrNull should return title URI when domain and idPackage are null`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = null,
                webDomain = null,
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
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

    @Test
    fun `buildUriOrNull should return title URI when domain and idPackage are empty`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = "",
                webDomain = "",
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
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

    @Test
    fun `buildUriOrNull should return null when title, domain, and idPackage are null`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = null,
                webDomain = null,
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
        )
        every { windowNode.title } returns null

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `buildUriOrNull should return null when title, domain, and idPackage are empty`() {
        // Setup
        val autofillView = AutofillView.Card.Number(
            data = autofillViewData.copy(
                idPackage = "",
                webDomain = "",
                webScheme = null,
            ),
        )
        val viewNodeTraversalData = ViewNodeTraversalData(
            autofillViews = listOf(
                autofillView,
            ),
            ignoreAutofillIds = emptyList(),
        )
        every { windowNode.title } returns ""

        // Test
        val actual = listOf(viewNodeTraversalData).buildUriOrNull(
            assistStructure = assistStructure,
        )

        // Verify
        assertNull(actual)
    }

    companion object {
        private const val ID_PACKAGE: String = "com.x8bit.bitwarden"
        private const val WEB_DOMAIN: String = "www.google.com"
        private const val WEB_SCHEME: String = "https"
    }
}
