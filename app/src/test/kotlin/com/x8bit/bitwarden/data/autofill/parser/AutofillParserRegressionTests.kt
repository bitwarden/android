package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.view.View
import android.view.autofill.AutofillId
import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.data.autofill.util.buildPackageNameOrNull
import com.x8bit.bitwarden.data.autofill.util.buildUriOrNull
import com.x8bit.bitwarden.data.autofill.util.getInlinePresentationSpecs
import com.x8bit.bitwarden.data.autofill.util.getMaxInlineSuggestionsCount
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import com.x8bit.bitwarden.data.autofill.util.website
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillParserRegressionTests {
    private lateinit var parser: AutofillParser

    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val assistStructure: AssistStructure = mockk()
    private val fillContext: FillContext = mockk {
        every { structure } returns assistStructure
    }
    private val fillRequest: FillRequest = mockk {
        every { id } returns 55
        every { fillContexts } returns listOf(fillContext)
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { isInlineAutofillEnabled } returns true
        every { blockedAutofillUris } returns emptyList()
    }

    private val packageName = "com.example.app"
    private val uri = "https://example.com"
    private val inlinePresentationSpecs: List<InlinePresentationSpec> = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(AssistStructure.ViewNode::toAutofillView)
        mockkStatic(AssistStructure.ViewNode::website)
        mockkStatic(
            FillRequest::getMaxInlineSuggestionsCount,
            FillRequest::getInlinePresentationSpecs,
            AutofillView::buildUriOrNull,
            List<ViewNodeTraversalData>::buildPackageNameOrNull,
        )

        every {
            fillRequest.getInlinePresentationSpecs(any(), any())
        } returns inlinePresentationSpecs
        every {
            fillRequest.getMaxInlineSuggestionsCount(any(), any())
        } returns 5
        every {
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
        } returns packageName
        every { any<AutofillView>().buildUriOrNull(packageName) } returns uri

        parser = AutofillParserImpl(settingsRepository)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::toAutofillView)
        unmockkStatic(AssistStructure.ViewNode::website)
        unmockkStatic(
            FillRequest::getMaxInlineSuggestionsCount,
            FillRequest::getInlinePresentationSpecs,
            AutofillView::buildUriOrNull,
            List<ViewNodeTraversalData>::buildPackageNameOrNull,
        )
    }

    @Test
    fun `parse should promote Custom view to Username when above Password view`() {
        // Setup scenarios:
        // 1. A generic input (parsed as Custom)
        // 2. A password input (parsed as Password)
        // 3. The generic input is directly above the password input in the view hierarchy.

        val customId = mockk<AutofillId>()
        val passwordId = mockk<AutofillId>()

        // Mock Custom View (Generic Input)
        val customViewData = AutofillView.Data(
            autofillId = customId,
            autofillOptions = emptyList(),
            autofillType = View.AUTOFILL_TYPE_TEXT,
            isFocused = true,
            textValue = null,
            hasPasswordTerms = false,
            website = uri,
            hint = "Employee Number", // Generic Hint
            idEntry = "employee_id"
        )
        val customAutofillView = AutofillView.Login.Custom(
            data = customViewData,
            inputType = 1
        )
        
        // Mock Password View
        val passwordViewData = AutofillView.Data(
            autofillId = passwordId,
            autofillOptions = emptyList(),
            autofillType = View.AUTOFILL_TYPE_TEXT,
            isFocused = false,
            textValue = null,
            hasPasswordTerms = true,
            website = uri,
            hint = "Password"
        )
        val passwordAutofillView = AutofillView.Login.Password(
            data = passwordViewData
        )

        // Mock ViewNodes with relaxed=true to avoid missing answer exceptions
        val customNode = mockk<AssistStructure.ViewNode>(relaxed = true) {
            every { toAutofillView(any()) } returns customAutofillView
            every { website } returns uri
            every { idPackage } returns packageName
            every { autofillId } returns customId
        }
        val passwordNode = mockk<AssistStructure.ViewNode>(relaxed = true) {
            every { toAutofillView(any()) } returns passwordAutofillView
            every { website } returns uri
            every { idPackage } returns packageName
            every { autofillId } returns passwordId
        }

        // Mock Root Node containing children
        val rootNode = mockk<AssistStructure.ViewNode>(relaxed = true) {
            every { childCount } returns 2
            every { getChildAt(0) } returns customNode
            every { getChildAt(1) } returns passwordNode
            every { toAutofillView(any()) } returns null // Root itself is not a view
            every { autofillId } returns null
            every { website } returns uri
            every { idPackage } returns packageName
        }

        val windowNode = mockk<AssistStructure.WindowNode> {
            every { rootViewNode } returns rootNode
        }

        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns windowNode

        // Execute
        val result = parser.parse(autofillAppInfo, fillRequest)

        // Verify
        assertTrue(result is AutofillRequest.Fillable, "Result should be Fillable")
        val fillable = result as AutofillRequest.Fillable
        assertTrue(fillable.partition is AutofillPartition.Login, "Partition should be Login")
        val loginPartition = fillable.partition as AutofillPartition.Login
        
        // Assert that the first view in the partition is now a Username view, not Custom
        val firstView = loginPartition.views[0]
        assertTrue(firstView is AutofillView.Login.Username, "Expected Custom view to be promoted to Username. Found: ${firstView::class.java.simpleName}")
        
        assertEquals(customId, firstView.data.autofillId, "Username ID should match the original Custom view ID")
    }
}
