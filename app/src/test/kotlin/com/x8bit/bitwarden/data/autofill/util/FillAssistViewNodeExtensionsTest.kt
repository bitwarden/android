package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.ViewStructure.HtmlInfo
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FillAssistViewNodeExtensionsTest {

    private val autofillId: AutofillId = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(AssistStructure.ViewNode::toAutofillViewData)
        mockkStatic(HtmlInfo::matchesSelectorClause)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::toAutofillViewData)
        unmockkStatic(HtmlInfo::matchesSelectorClause)
    }

    @Test
    fun `toFillAssistView should return null when autofillId is null`() {
        val viewNode = createViewNode(htmlInfo = createHtmlInfo(), autofillId = null)

        val actual = viewNode.toFillAssistView(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            website = null,
        )

        assertNull(actual)
    }

    @Test
    fun `toFillAssistView should return null when htmlInfo is null`() {
        val viewNode = createViewNode(htmlInfo = null)

        val actual = viewNode.toFillAssistView(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            website = null,
        )

        assertNull(actual)
    }

    @Test
    fun `toFillAssistView should return null when htmlInfo does not match`() {
        val viewNode = createViewNode(htmlInfo = createHtmlInfo(matches = false))

        val actual = viewNode.toFillAssistView(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            website = null,
        )

        assertNull(actual)
    }

    @Test
    fun `toFillAssistView should return Login Username when htmlInfo matches username clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val actual = viewNode.toFillAssistView(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            website = null,
        )

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Test
    fun `toFillAssistView should return Login Password when htmlInfo matches password clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf("password" to listOf(selectorClause(tag = "input", id = "pass"))),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Password(data = data), actual)
    }

    @Test
    fun `toFillAssistView should return Login Password when htmlInfo matches newPassword clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-registration",
            fields = mapOf("newPassword" to listOf(selectorClause(tag = "input", id = "new-pass"))),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Password(data = data), actual)
    }

    @Test
    fun `toFillAssistView should map all card field keys to correct AutofillView subtypes`() {
        val cardFieldExpectations: List<Pair<String, AutofillView>> = listOf(
            "cardNumber" to AutofillView.Card.Number(data = autofillData()),
            "cardholderName" to AutofillView.Card.CardholderName(data = autofillData()),
            "cardExpirationDate" to AutofillView.Card.ExpirationDate(data = autofillData()),
            "cardExpirationMonth" to AutofillView.Card.ExpirationMonth(
                data = autofillData(),
                monthValue = null,
            ),
            "cardExpirationYear" to AutofillView.Card.ExpirationYear(
                data = autofillData(),
                yearValue = null,
            ),
            "cardCvv" to AutofillView.Card.SecurityCode(data = autofillData()),
            "cardType" to AutofillView.Card.Brand(data = autofillData(), brandValue = null),
        )

        cardFieldExpectations.forEach { (fieldKey, expectedView) ->
            val htmlInfo = createHtmlInfo()
            val viewNode = createViewNode(htmlInfo = htmlInfo)
            val data = autofillData()
            every {
                viewNode.toAutofillViewData(autofillId = autofillId, website = null)
            } returns data

            val hostRule = FillAssistRules.HostRule(
                category = "payment-card",
                fields = mapOf(
                    fieldKey to listOf(selectorClause(tag = "input", id = "field-$fieldKey")),
                ),
            )

            val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

            assertEquals(expectedView, actual, "Failed for field key: $fieldKey")
        }
    }

    @Test
    fun `toFillAssistView should return null when matched field key is unknown`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        every {
            viewNode.toAutofillViewData(autofillId = autofillId, website = null)
        } returns autofillData()

        val hostRule = FillAssistRules.HostRule(
            category = "unknown",
            fields = mapOf("unknownFieldKey" to listOf(selectorClause(tag = "input", id = "x"))),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFillAssistView should pick first mapped key when multiple keys match the same node`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = linkedMapOf(
                "email" to listOf(selectorClause(tag = "input", id = "shared")),
                "username" to listOf(selectorClause(tag = "input", id = "shared")),
            ),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFillAssistView should use provided website when building AutofillView Data`() {
        val website = "https://example.com"
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo, website = null)
        val data = autofillData(website = website)
        every {
            viewNode.toAutofillViewData(autofillId = autofillId, website = website)
        } returns data

        val actual = viewNode.toFillAssistView(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            website = website,
        )

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFillAssistView should match when htmlInfo has no attributes and all clause attrs are null`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "username" to listOf(
                    FillAssistRules.SelectorClause(
                        tag = "input",
                        id = null,
                        name = null,
                        type = null,
                        role = null,
                    ),
                ),
            ),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFillAssistView should require all non-null clause attributes to match (AND logic)`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "username" to listOf(
                    FillAssistRules.SelectorClause(
                        tag = "input",
                        id = "user",
                        name = "username",
                        type = "text",
                        role = "textbox",
                    ),
                ),
            ),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFillAssistView should return null when one of multiple required attributes is wrong`() {
        val htmlInfo = createHtmlInfo(matches = false)
        val viewNode = createViewNode(htmlInfo = htmlInfo)

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "username" to listOf(
                    FillAssistRules.SelectorClause(
                        tag = "input",
                        id = "user",
                        name = "username",
                        type = "text",
                        role = "textbox",
                    ),
                ),
            ),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertNull(actual)
    }

    @Test
    fun `toFillAssistView should match when clause has null tag (tag check skipped)`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "username" to listOf(
                    FillAssistRules.SelectorClause(
                        tag = null,
                        id = "user",
                        name = null,
                        type = null,
                        role = null,
                    ),
                ),
            ),
        )

        val actual = viewNode.toFillAssistView(hostRules = listOf(hostRule), website = null)

        assertEquals(AutofillView.Login.Username(data = data), actual)
    }

    private fun autofillData(website: String? = null): AutofillView.Data = AutofillView.Data(
        autofillId = autofillId,
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = false,
        textValue = null,
        hasPasswordTerms = false,
        website = website,
    )

    private fun usernameHostRule(
        tag: String?,
        id: String?,
    ): FillAssistRules.HostRule = FillAssistRules.HostRule(
        category = "account-login",
        fields = mapOf("username" to listOf(selectorClause(tag = tag, id = id))),
    )

    private fun selectorClause(
        tag: String? = null,
        id: String? = null,
        name: String? = null,
        type: String? = null,
        role: String? = null,
    ): FillAssistRules.SelectorClause = FillAssistRules.SelectorClause(
        tag = tag,
        id = id,
        name = name,
        type = type,
        role = role,
    )

    private fun createHtmlInfo(matches: Boolean = true): HtmlInfo = mockk<HtmlInfo>().also {
        every { it.matchesSelectorClause(any()) } returns matches
    }

    private fun createViewNode(
        htmlInfo: HtmlInfo?,
        autofillId: AutofillId? = this.autofillId,
        website: String? = null,
    ): AssistStructure.ViewNode = mockk {
        every { this@mockk.htmlInfo } returns htmlInfo
        every { this@mockk.autofillId } returns autofillId
        every { this@mockk.website } returns website
    }
}
