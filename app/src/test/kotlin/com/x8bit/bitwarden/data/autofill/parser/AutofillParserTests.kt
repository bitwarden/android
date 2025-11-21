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
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class AutofillParserTests {
    private lateinit var parser: AutofillParser

    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val assistStructure: AssistStructure = mockk()
    private val cardAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
    private val cardAutofillId: AutofillId = mockk()
    private val cardViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(cardAutofillHint)
        every { this@mockk.autofillId } returns cardAutofillId
        every { this@mockk.childCount } returns 0
        every { this@mockk.idPackage } returns ID_PACKAGE
        every { this@mockk.idEntry } returns null
    }
    private val loginAutofillHint = View.AUTOFILL_HINT_USERNAME
    private val loginAutofillId: AutofillId = mockk()
    private val loginViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(loginAutofillHint)
        every { this@mockk.autofillId } returns loginAutofillId
        every { this@mockk.childCount } returns 0
        every { this@mockk.idPackage } returns ID_PACKAGE
        every { this@mockk.idEntry } returns null
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
        every { id } returns 55
        every { this@mockk.fillContexts } returns listOf(fillContext)
    }
    private val inlinePresentationSpecs: List<InlinePresentationSpec> = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every { isInlineAutofillEnabled } answers { mockIsInlineAutofillEnabled }
        every { blockedAutofillUris } returns emptyList()
    }

    private var mockIsInlineAutofillEnabled = true

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
        every { cardViewNode.website } returns WEBSITE
        every { loginViewNode.website } returns WEBSITE
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
            null.getInlinePresentationSpecs(
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
        every {
            null.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
        } returns 0
        every {
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
        } returns PACKAGE_NAME
        every { any<AutofillView>().buildUriOrNull(PACKAGE_NAME) } returns URI
        parser = AutofillParserImpl(
            settingsRepository = settingsRepository,
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `parse should return Fillable with website in AutofillView from url bar for compatible browser`() {
        // Setup
        val website = "https://m.facebook.com"
        val packageName = "com.microsoft.emmx"
        every {
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
        } returns packageName
        every { assistStructure.windowNodeCount } returns 2
        // Override the idPackage to be Edge's package name.
        every { loginViewNode.idPackage } returns packageName
        every { assistStructure.getWindowNodeAt(0) } returns loginWindowNode
        val urlBarNode: AssistStructure.ViewNode = mockk {
            every { autofillHints } returns emptyArray()
            every { autofillId } returns null
            every { childCount } returns 0
            every { idEntry } returns "url_bar"
            every { idPackage } returns packageName
            every { webDomain } returns "m.facebook.com"
            every { webScheme } returns null
        }
        val urlBarWindowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns urlBarNode
        }
        every { assistStructure.getWindowNodeAt(1) } returns urlBarWindowNode
        val loginAutofillView: AutofillView.Login.Username = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = null,
            ),
        )
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                loginAutofillView.copy(data = loginAutofillView.data.copy(website = website)),
            ),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = packageName,
            partition = autofillPartition,
            uri = website,
        )

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(packageName)
        }
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
            every { this@mockk.idPackage } returns null
            every { this@mockk.isFocused } returns false
            every { this@mockk.toAutofillView(parentWebsite = any()) } returns null
            every { this@mockk.website } returns null
        }
        // `invalidChildViewNode` simulates the OS assigning a node's idPackage to "android", which
        // is not considered a valid app package name.
        val invalidChildAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
        val invalidChildAutofillId: AutofillId = mockk()
        val invalidChildViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns arrayOf(invalidChildAutofillHint)
            every { this@mockk.autofillId } returns invalidChildAutofillId
            every { this@mockk.childCount } returns 0
            every { this@mockk.idPackage } returns ID_PACKAGE_ANDROID
            every { this@mockk.isFocused } returns false
            every { this@mockk.toAutofillView(parentWebsite = any()) } returns null
            every { this@mockk.website } returns null
        }
        val parentAutofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val parentAutofillId: AutofillId = mockk()
        val parentAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = parentAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val parentViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns arrayOf(parentAutofillHint)
            every { this@mockk.autofillId } returns parentAutofillId
            every { this@mockk.idPackage } returns null
            every { this@mockk.toAutofillView(parentWebsite = any()) } returns parentAutofillView
            every { this@mockk.childCount } returns 2
            every { this@mockk.getChildAt(0) } returns childViewNode
            every { this@mockk.getChildAt(1) } returns invalidChildViewNode
            every { this@mockk.website } returns null
        }
        val windowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns parentViewNode
        }
        val autofillPartition = AutofillPartition.Card(
            views = listOf(parentAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = listOf(childAutofillId, invalidChildAutofillId),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            partition = autofillPartition,
            packageName = PACKAGE_NAME,
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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
        verify(exactly = 0) {
            any<AutofillView>().buildUriOrNull(ID_PACKAGE_ANDROID)
        }
    }

    @Test
    fun `parse should choose AutofillPartition Card when a Card view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Test
    fun `parse should choose AutofillPartition Login when a Login view is focused`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(loginAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should have Password AutofillView when the Password field is invalid, contains no other Password fields, and contains a password term`() {
        // Setup
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns loginWindowNode
        val unusedAutofillView: AutofillView.Unused = AutofillView.Unused(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = true,
                website = URI,
            ),
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Password(
            data = unusedAutofillView.data,
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(loginAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns unusedAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should have Username and Password AutofillView when the Username field is not identifiable but directly above the Password field in the hierarchy`() {
        // Setup
        val hiddenUserNameViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns emptyArray()
            every { this@mockk.autofillId } returns loginAutofillId
            every { this@mockk.childCount } returns 0
            every { this@mockk.idPackage } returns ID_PACKAGE
            every { this@mockk.website } returns WEBSITE
        }
        val passwordAutofillId = mockk<AutofillId>()
        val passwordViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns emptyArray()
            every { this@mockk.autofillId } returns passwordAutofillId
            every { this@mockk.childCount } returns 0
            every { this@mockk.idPackage } returns ID_PACKAGE
            every { this@mockk.website } returns WEBSITE
        }
        val rootAutofillId = mockk<AutofillId>()
        val rootViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.autofillHints } returns emptyArray()
            every { this@mockk.autofillId } returns rootAutofillId
            every { this@mockk.childCount } returns 0
            every { this@mockk.idPackage } returns ID_PACKAGE
            every { this@mockk.website } returns WEBSITE
            every { this@mockk.childCount } returns 2
            every { this@mockk.getChildAt(0) } returns hiddenUserNameViewNode
            every { this@mockk.getChildAt(1) } returns passwordViewNode
        }
        val windowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns rootViewNode
        }
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns windowNode
        val unusedAutofillView: AutofillView.Unused = AutofillView.Unused(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val loginUsernameAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val loginPasswordAutofillView: AutofillView.Login = AutofillView.Login.Password(
            data = AutofillView.Data(
                autofillId = passwordAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(loginUsernameAutofillView, loginPasswordAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = listOf(rootAutofillId),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { rootViewNode.toAutofillView(parentWebsite = any()) } returns null
        every {
            hiddenUserNameViewNode.toAutofillView(parentWebsite = any())
        } returns unusedAutofillView
        every {
            passwordViewNode.toAutofillView(parentWebsite = any())
        } returns loginPasswordAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Test
    fun `parse should choose first focused AutofillView for partition when there are multiple`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should choose first fillable AutofillView for partition when there is no focused view`() {
        // Setup
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Test
    fun `parse should return empty inline suggestions when inline autofill is disabled`() {
        // Setup
        mockIsInlineAutofillEnabled = false
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

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
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Test
    fun `parse should return empty inline suggestions when parsing an AssistStructure directly`() {
        // Setup
        mockIsInlineAutofillEnabled = false
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(cardAutofillView),
        )
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            packageName = PACKAGE_NAME,
            partition = autofillPartition,
            uri = URI,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

        // Test
        val actual = parser.parse(
            autofillAppInfo = autofillAppInfo,
            assistStructure = assistStructure,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            null.getInlinePresentationSpecs(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
            null.getMaxInlineSuggestionsCount(
                autofillAppInfo = autofillAppInfo,
                isInlineAutofillEnabled = false,
            )
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        }
    }

    @Test
    fun `parse should skip block listed URIs Login when a Login view is focused`() {
        // Setup all tests
        setupAssistStructureWithAllAutofillViewTypes()
        val cardAutofillView: AutofillView.Card = AutofillView.Card.ExpirationMonth(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
            monthValue = null,
        )
        val loginAutofillView: AutofillView.Login = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        val remoteBlockList = listOf(
            "blockListedUri.com",
            "blockListedAgainUri.com",
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView
        every { settingsRepository.blockedAutofillUris } returns remoteBlockList

        // A function for asserting that a block listed URI results in an unfillable request.
        fun testBlockListedUri(blockListedUri: String) {
            // Setup
            every { any<AutofillView>().buildUriOrNull(PACKAGE_NAME) } returns blockListedUri

            // Test
            val actual = parser.parse(
                autofillAppInfo = autofillAppInfo,
                fillRequest = fillRequest,
            )

            // Verify
            assertEquals(AutofillRequest.Unfillable, actual)
        }

        // Test all block listed URIs
        BLOCK_LISTED_URIS.forEach(::testBlockListedUri)
        remoteBlockList.forEach(::testBlockListedUri)

        // Verify all tests
        verify(exactly = BLOCK_LISTED_URIS.size + remoteBlockList.size) {
            any<List<ViewNodeTraversalData>>().buildPackageNameOrNull(assistStructure)
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
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

private val BLOCK_LISTED_URIS: List<String> = listOf(
    "androidapp://android",
    "androidapp://com.android.settings",
    "androidapp://com.x8bit.bitwarden",
    "androidapp://com.oneplus.applocker",
)
private const val AUTOFILL_TYPE: Int = View.AUTOFILL_TYPE_TEXT
private const val ID_PACKAGE: String = "com.x8bit.bitwarden"
private const val MAX_INLINE_SUGGESTION_COUNT: Int = 42
private const val PACKAGE_NAME: String = "com.google"
private const val URI: String = "androidapp://com.google"
private const val WEBSITE: String = "https://www.google.com"

// ID package assigned to some nodes in the autofill view hierarchy by the OS.
private const val ID_PACKAGE_ANDROID = "android"
