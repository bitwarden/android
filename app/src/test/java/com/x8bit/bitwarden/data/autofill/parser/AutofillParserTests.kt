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
import com.x8bit.bitwarden.data.autofill.util.buildUriOrNull
import com.x8bit.bitwarden.data.autofill.util.getInlinePresentationSpecs
import com.x8bit.bitwarden.data.autofill.util.getMaxInlineSuggestionsCount
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillParserTests {
    private lateinit var parser: AutofillParser

    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val autofillViewData = AutofillView.Data(
        autofillId = mockk(),
        isFocused = true,
        idPackage = null,
        webDomain = null,
        webScheme = null,
    )
    private val assistStructure: AssistStructure = mockk()
    private val cardAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
    private val cardAutofillId: AutofillId = mockk()
    private val cardViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(cardAutofillHint)
        every { this@mockk.autofillId } returns cardAutofillId
        every { this@mockk.childCount } returns 0
    }
    private val loginAutofillHint = View.AUTOFILL_HINT_USERNAME
    private val loginAutofillId: AutofillId = mockk()
    private val loginViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(loginAutofillHint)
        every { this@mockk.autofillId } returns loginAutofillId
        every { this@mockk.childCount } returns 0
    }
    private val cardWindowNode: AssistStructure.WindowNode = mockk {
        every { this@mockk.rootViewNode } returns cardViewNode
    }
    private val loginWindowNode: AssistStructure.WindowNode = mockk {
        every { this@mockk.rootViewNode } returns loginViewNode
    }
    private val fillContext: FillContext = mockk {
        every { this@mockk.structure } returns assistStructure
    }
    private val fillRequest: FillRequest = mockk {
        every { this@mockk.fillContexts } returns listOf(fillContext)
    }
    private val inlinePresentationSpecs: List<InlinePresentationSpec> = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every { isInlineAutofillEnabled } answers { mockIsInlineAutofillEnabled }
    }

    private var mockIsInlineAutofillEnabled = true

    @BeforeEach
    fun setup() {
        mockkStatic(AssistStructure.ViewNode::toAutofillView)
        mockkStatic(
            FillRequest::getMaxInlineSuggestionsCount,
            FillRequest::getInlinePresentationSpecs,
        )
        mockkStatic(List<ViewNodeTraversalData>::buildUriOrNull)
        every {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
        } returns inlinePresentationSpecs
        every {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
        } returns emptyList()
        every {
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
        } returns MAX_INLINE_SUGGESTION_COUNT
        every {
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
        } returns 0
        every { any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure) } returns URI
        parser = AutofillParserImpl(
            settingsRepository = settingsRepository,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::toAutofillView)
        unmockkStatic(
            FillRequest::getMaxInlineSuggestionsCount,
            FillRequest::getInlinePresentationSpecs,
        )
        unmockkStatic(List<ViewNodeTraversalData>::buildUriOrNull)
    }

    @Test
    fun `parse should return Unfillable when no contexts`() {
        // Setup
        val expected = AutofillRequest.Unfillable
        every { fillRequest.fillContexts } returns emptyList()

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should return Unfillable when windowNodeCount is 0`() {
        // Setup
        val expected = AutofillRequest.Unfillable
        every { assistStructure.windowNodeCount } returns 0

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should return Fillable when at least one node valid, ignores the invalid nodes`() {
        // Setup
        val childAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
        val childAutofillId: AutofillId = mockk()
        val childViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns arrayOf(childAutofillHint)
            every { this@mockk.autofillId } returns childAutofillId
            every { this@mockk.childCount } returns 0
            every { this@mockk.isFocused } returns false
            every { this@mockk.toAutofillView() } returns null
        }
        val parentAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val parentAutofillId: AutofillId = mockk()
        val parentAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = autofillViewData.copy(
                autofillId = parentAutofillId,
                isFocused = true,
            ),
        )
        val parentViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns arrayOf(parentAutofillHint)
            every { this@mockk.autofillId } returns parentAutofillId
            every { this@mockk.toAutofillView() } returns parentAutofillView
            every { this@mockk.childCount } returns 1
            every { this@mockk.getChildAt(0) } returns childViewNode
        }
        val windowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns parentViewNode
        }
        val autofillPartition = AutofillPartition.Card(
            views = listOf(parentAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = listOf(childAutofillId),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            partition = autofillPartition,
            uri = URI,
        )
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns windowNode

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure)
        }
    }

    @Test
    fun `parse should choose AutofillPartition Card when a Card view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = autofillViewData.copy(
                autofillId = cardAutofillId,
                isFocused = true,
            ),
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = autofillViewData.copy(
                autofillId = loginAutofillId,
                isFocused = false,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure)
        }
    }

    @Test
    fun `parse should choose AutofillPartition Login when a Login view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = autofillViewData.copy(
                autofillId = cardAutofillId,
                isFocused = false,
            ),
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = autofillViewData.copy(
                autofillId = loginAutofillId,
                isFocused = true,
            ),
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(loginAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure)
        }
    }

    @Test
    fun `parse should choose first focused AutofillView for partition when there are multiple`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = autofillViewData.copy(
                autofillId = cardAutofillId,
                isFocused = true,
            ),
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = autofillViewData.copy(
                autofillId = loginAutofillId,
                isFocused = true,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = true,
            )
            any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure)
        }
    }

    @Test
    fun `parse should return empty inline suggestions when inline autofill is disabled`() {
        // Setup
        mockIsInlineAutofillEnabled = false
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = autofillViewData.copy(
                autofillId = cardAutofillId,
                isFocused = true,
            ),
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = autofillViewData.copy(
                autofillId = loginAutofillId,
                isFocused = true,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            fillRequest.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
            fillRequest.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
            any<List<ViewNodeTraversalData>>().buildUriOrNull(assistStructure)
        }
    }

    /**
     * Setup [assistStructure] to return window nodes with each [AutofillView] type (card and login)
     * so we can test how different window node configurations produce different partitions.
     */
    private fun setupAssistStructureWithAllAutofillViewTypes() {
        every { assistStructure.windowNodeCount } returns 2
        every { assistStructure.getWindowNodeAt(0) } returns cardWindowNode
        every { assistStructure.getWindowNodeAt(1) } returns loginWindowNode
    }
}

private const val MAX_INLINE_SUGGESTION_COUNT: Int = 42
private const val URI: String = "androidapp://com.x8bit.bitwarden"
