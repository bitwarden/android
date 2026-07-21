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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
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
    fun `buildFillAssistViews should return empty list when there are no window nodes`() {
        val assistStructure: AssistStructure = mockk {
            every { windowNodeCount } returns 0
        }

        val actual = assistStructure.buildFillAssistViews(
            hostRules = emptyList(),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Test
    fun `buildFillAssistViews should exclude node with null htmlInfo`() {
        val viewNode = createViewNode(htmlInfo = null)
        val assistStructure = createAssistStructure(viewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should return Login Username when htmlInfo matches username clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should return Login Email when htmlInfo matches email clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every {
            viewNode.toAutofillViewData(
                autofillId = autofillId,
                website = null,
            )
        } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "email" to listOf(selectorClause(tag = "input", id = "email")),
            ),
        )

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Email(data = data)), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should return Login Password when htmlInfo matches password clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = mapOf(
                "password" to listOf(selectorClause(tag = "input", id = "pass")),
            ),
        )

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Password(data = data)), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should return Login Password when htmlInfo matches newPassword clause`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-registration",
            fields = mapOf(
                "newPassword" to listOf(selectorClause(tag = "input", id = "new-pass")),
            ),
        )

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Password(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should map all card field keys to correct AutofillView subtypes`() {
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
            val assistStructure = createAssistStructure(viewNode)
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

            val actual = assistStructure.buildFillAssistViews(
                hostRules = listOf(hostRule),
                urlBarWebsite = null,
            )

            assertEquals(
                listOf(expectedView),
                actual,
                "Failed for field key: $fieldKey",
            )
        }
    }

    @Test
    fun `buildFillAssistViews should exclude node whose matched field key is unknown`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        every {
            viewNode.toAutofillViewData(autofillId = autofillId, website = null)
        } returns autofillData()

        val hostRule = FillAssistRules.HostRule(
            category = "unknown",
            fields = mapOf(
                "unknownFieldKey" to listOf(selectorClause(tag = "input", id = "mystery")),
            ),
        )

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should pick first mapped key when multiple keys match the same node`() {
        // Two field keys ("email" and "username") both match the same node. The implementation
        // iterates in insertion order and selects the first key whose mapping is non-null.
        // Since "email" is listed first, it wins and produces a Login.Email view.
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val hostRule = FillAssistRules.HostRule(
            category = "account-login",
            fields = linkedMapOf(
                "email" to listOf(selectorClause(tag = "input", id = "shared")),
                "username" to listOf(selectorClause(tag = "input", id = "shared")),
            ),
        )

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Email(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should traverse child nodes recursively`() {
        val childHtmlInfo = createHtmlInfo()
        val childViewNode = createViewNode(htmlInfo = childHtmlInfo)
        val childData = autofillData()
        every {
            childViewNode.toAutofillViewData(autofillId = autofillId, website = null)
        } returns childData

        val rootHtmlInfo = createHtmlInfo(matches = false)
        val rootViewNode = createViewNode(
            htmlInfo = rootHtmlInfo,
            children = listOf(childViewNode),
        )

        val assistStructure = createAssistStructure(rootViewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = childData)), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should propagate urlBarWebsite as website when node has no website`() {
        val urlBarWebsite = "https://example.com"
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo, website = null)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData(website = urlBarWebsite)
        every {
            viewNode.toAutofillViewData(autofillId = autofillId, website = urlBarWebsite)
        } returns data

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = urlBarWebsite,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should prefer node's own website over urlBarWebsite`() {
        val nodeWebsite = "https://node.example.com"
        val urlBarWebsite = "https://urlbar.example.com"
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo, website = nodeWebsite)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData(website = nodeWebsite)
        every {
            viewNode.toAutofillViewData(autofillId = autofillId, website = nodeWebsite)
        } returns data

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = urlBarWebsite,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should exclude node with no autofillId`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo, autofillId = null)
        val assistStructure = createAssistStructure(viewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Test
    fun `buildFillAssistViews should not match when htmlInfo tag differs from clause tag`() {
        val htmlInfo = createHtmlInfo(matches = false)
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should match when htmlInfo has no attributes and all clause attrs are null`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
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

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should not match when htmlInfo has no attributes but clause requires id`() {
        val htmlInfo = createHtmlInfo(matches = false)
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Test
    fun `buildFillAssistViews should match when htmlInfo has correct id attribute`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should not match when htmlInfo id attribute is wrong`() {
        val htmlInfo = createHtmlInfo(matches = false)
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should require all non-null clause attributes to match (AND logic)`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
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

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFillAssistViews should not match when one of multiple required attributes is wrong`() {
        val htmlInfo = createHtmlInfo(matches = false)
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)

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

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(emptyList<AutofillView>(), actual)
    }

    @Test
    fun `buildFillAssistViews should skip null clause attributes (not required)`() {
        // Clause only requires tag + id; node has extra attributes which should be ignored.
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
        val data = autofillData()
        every { viewNode.toAutofillViewData(autofillId = autofillId, website = null) } returns data

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should match when clause has null tag (tag check skipped)`() {
        val htmlInfo = createHtmlInfo()
        val viewNode = createViewNode(htmlInfo = htmlInfo)
        val assistStructure = createAssistStructure(viewNode)
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

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(hostRule),
            urlBarWebsite = null,
        )

        assertEquals(listOf(AutofillView.Login.Username(data = data)), actual)
    }

    @Test
    fun `buildFillAssistViews should return views from all children when only some match`() {
        val matchingChildHtmlInfo = createHtmlInfo()
        val matchingChild = createViewNode(htmlInfo = matchingChildHtmlInfo)
        val matchingChildData = autofillData()
        every {
            matchingChild.toAutofillViewData(autofillId = autofillId, website = null)
        } returns matchingChildData

        val nonMatchingChildHtmlInfo = createHtmlInfo(matches = false)
        val nonMatchingChild = createViewNode(htmlInfo = nonMatchingChildHtmlInfo)

        val rootHtmlInfo = createHtmlInfo(matches = false)
        val rootViewNode = createViewNode(
            htmlInfo = rootHtmlInfo,
            children = listOf(matchingChild, nonMatchingChild),
        )

        val assistStructure = createAssistStructure(rootViewNode)

        val actual = assistStructure.buildFillAssistViews(
            hostRules = listOf(usernameHostRule(tag = "input", id = "user")),
            urlBarWebsite = null,
        )

        assertEquals(
            listOf(
                AutofillView.Login.Username(
                    data = matchingChildData,
                ),
            ),
            actual,
        )
        // Sanity check that traversal visited multiple children.
        assertTrue(actual.size == 1)
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
        fields = mapOf(
            "username" to listOf(selectorClause(tag = tag, id = id)),
        ),
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
        children: List<AssistStructure.ViewNode> = emptyList(),
    ): AssistStructure.ViewNode = mockk {
        every { this@mockk.htmlInfo } returns htmlInfo
        every { this@mockk.autofillId } returns autofillId
        every { this@mockk.website } returns website
        every { this@mockk.childCount } returns children.size
        children.forEachIndexed { index, child ->
            every { this@mockk.getChildAt(index) } returns child
        }
    }

    private fun createAssistStructure(
        rootViewNode: AssistStructure.ViewNode,
    ): AssistStructure {
        val windowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns rootViewNode
        }
        return mockk {
            every { windowNodeCount } returns 1
            every { getWindowNodeAt(0) } returns windowNode
        }
    }
}
