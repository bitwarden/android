package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillParserTests {
    private lateinit var parser: AutofillParser

    private val assistStructure: AssistStructure = mockk()
    private val cardAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
    private val cardAutofillId: AutofillId = mockk()
    private val cardViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(cardAutofillHint)
        every { this@mockk.autofillId } returns cardAutofillId
        every { this@mockk.childCount } returns 0
    }
    private val identityAutofillHint = View.AUTOFILL_HINT_NAME
    private val identityAutofillId: AutofillId = mockk()
    private val identityViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(identityAutofillHint)
        every { this@mockk.autofillId } returns identityAutofillId
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
    private val identityWindowNode: AssistStructure.WindowNode = mockk {
        every { this@mockk.rootViewNode } returns identityViewNode
    }
    private val loginWindowNode: AssistStructure.WindowNode = mockk {
        every { this@mockk.rootViewNode } returns loginViewNode
    }

    @BeforeEach
    fun setup() {
        mockkStatic(AssistStructure.ViewNode::toAutofillView)
        parser = AutofillParserImpl()
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::toAutofillView)
    }

    @Test
    fun `parse should return Unfillable when windowNodeCount is 0`() {
        // Setup
        val expected = AutofillRequest.Unfillable
        every { assistStructure.windowNodeCount } returns 0

        // Test
        val actual = parser.parse(assistStructure)

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
            autofillId = parentAutofillId,
            isFocused = true,
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
            partition = autofillPartition,
        )
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns windowNode

        // Test
        val actual = parser.parse(assistStructure)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should choose AutofillPartition Card when a Card view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            autofillId = cardAutofillId,
            isFocused = true,
        )
        val identityAutofillView: AutofillView.Identity = AutofillView.Identity.Name(
            autofillId = identityAutofillId,
            isFocused = false,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            autofillId = loginAutofillId,
            isFocused = false,
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            partition = autofillPartition,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { identityViewNode.toAutofillView() } returns identityAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(assistStructure)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should choose AutofillPartition Identity when an Identity view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            autofillId = cardAutofillId,
            isFocused = false,
        )
        val identityAutofillView: AutofillView.Identity = AutofillView.Identity.Name(
            autofillId = identityAutofillId,
            isFocused = true,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            autofillId = loginAutofillId,
            isFocused = false,
        )
        val autofillPartition = AutofillPartition.Identity(
            views = listOf(identityAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            partition = autofillPartition,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { identityViewNode.toAutofillView() } returns identityAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(assistStructure)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should choose AutofillPartition Login when a Login view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            autofillId = cardAutofillId,
            isFocused = false,
        )
        val identityAutofillView: AutofillView.Identity = AutofillView.Identity.Name(
            autofillId = identityAutofillId,
            isFocused = false,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            autofillId = loginAutofillId,
            isFocused = true,
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(loginAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            partition = autofillPartition,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { identityViewNode.toAutofillView() } returns identityAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(assistStructure)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `parse should choose first focused AutofillView for partition when there are multiple`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            autofillId = cardAutofillId,
            isFocused = true,
        )
        val identityAutofillView: AutofillView.Identity = AutofillView.Identity.Name(
            autofillId = identityAutofillId,
            isFocused = true,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            autofillId = loginAutofillId,
            isFocused = false,
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            partition = autofillPartition,
        )
        every { cardViewNode.toAutofillView() } returns cardAutofillView
        every { identityViewNode.toAutofillView() } returns identityAutofillView
        every { loginViewNode.toAutofillView() } returns loginAutofillView

        // Test
        val actual = parser.parse(assistStructure)

        // Verify
        assertEquals(expected, actual)
    }

    /**
     * Setup [assistStructure] to return window nodes with each [AutofillView] type (card, identity,
     * and login) so we can test how different window node configurations produce different
     * partitions.
     */
    private fun setupAssistStructureWithAllAutofillViewTypes() {
        every { assistStructure.windowNodeCount } returns 3
        every { assistStructure.getWindowNodeAt(0) } returns cardWindowNode
        every { assistStructure.getWindowNodeAt(1) } returns identityWindowNode
        every { assistStructure.getWindowNodeAt(2) } returns loginWindowNode
    }
}
