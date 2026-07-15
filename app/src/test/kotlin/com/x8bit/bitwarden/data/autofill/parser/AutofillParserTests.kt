package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.net.Uri
import android.net.Uri.parse
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.view.View
import android.view.ViewStructure.HtmlInfo
import android.view.autofill.AutofillId
import android.widget.inline.InlinePresentationSpec
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.autofill.manager.FillAssistManager
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.data.autofill.util.buildPackageNameOrNull
import com.x8bit.bitwarden.data.autofill.util.buildUriOrNull
import com.x8bit.bitwarden.data.autofill.util.getInlinePresentationSpecs
import com.x8bit.bitwarden.data.autofill.util.getMaxInlineSuggestionsCount
import com.x8bit.bitwarden.data.autofill.util.matchesSelectorClause
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import com.x8bit.bitwarden.data.autofill.util.toAutofillViewData
import com.x8bit.bitwarden.data.autofill.util.website
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
        every { this@mockk.htmlInfo } returns mockk(relaxed = true)
        every { this@mockk.idPackage } returns ID_PACKAGE
        every { this@mockk.idEntry } returns null
    }
    private val loginAutofillHint = View.AUTOFILL_HINT_USERNAME
    private val loginAutofillId: AutofillId = mockk()
    private val loginViewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillHints } returns arrayOf(loginAutofillHint)
        every { this@mockk.autofillId } returns loginAutofillId
        every { this@mockk.childCount } returns 0
        every { this@mockk.htmlInfo } returns mockk(relaxed = true)
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
        every { isFillAssistEnabled } answers { mockIsFillAssistEnabled }
        every { blockedAutofillUris } returns emptyList()
    }
    private val fillAssistManager: FillAssistManager = mockk()
    private val mutableFillAssistFlagFlow = MutableStateFlow(false)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlag(FlagKey.FillAssistTargetingRules)
        } answers {
            mutableFillAssistFlagFlow.value
        }
        every {
            getFeatureFlagFlow(FlagKey.FillAssistTargetingRules)
        } returns mutableFillAssistFlagFlow
    }

    private var mockIsInlineAutofillEnabled = true
    private var mockIsFillAssistEnabled = false

    @BeforeEach
    fun setup() {
        mockIsFillAssistEnabled = false
        // toAutofillView, website, and toAutofillViewData all compile into the same
        // ViewNodeExtensionsKt class, so one mockkStatic call covers all three.
        mockkStatic(AssistStructure.ViewNode::toAutofillView)
        // Default stub for toAutofillViewData (same mocked class — no separate mockkStatic needed).
        every {
            any<AssistStructure.ViewNode>().toAutofillViewData(autofillId = any(), website = any())
        } answers {
            AutofillView.Data(
                autofillId = firstArg(),
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = false,
                textValue = null,
                hasPasswordTerms = false,
                website = secondArg(),
            )
        }
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
            fillAssistManager = fillAssistManager,
            featureFlagManager = featureFlagManager,
        )

        mockkStatic(Uri::parse)
        every { parse(any()) } returns mockk {
            every { host } returns FILL_ASSIST_URI
        }
        mockkStatic(HtmlInfo::matchesSelectorClause)
        every { any<HtmlInfo>().matchesSelectorClause(any()) } returns false
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::toAutofillView)
        unmockkStatic(Uri::parse)
        unmockkStatic(HtmlInfo::matchesSelectorClause)
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
            every { this@mockk.childCount } returns 2
            every { this@mockk.idPackage } returns ID_PACKAGE
            every { this@mockk.website } returns WEBSITE
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

    @Suppress("MaxLineLength")
    @Test
    fun `parse should fall back to heuristics when fill-assist rules exist but only cover login and a card view is focused`() {
        // Setup: fill-assist enabled with login-only rules, but a card view is focused.
        mutableFillAssistFlagFlow.value = true
        mockIsFillAssistEnabled = true
        every {
            any<AutofillView>().buildUriOrNull(PACKAGE_NAME)
        } returns FILL_ASSIST_URI
        every { fillAssistManager.getFillAssistRules() } returns FillAssistRules(
            hostRules = mapOf(
                FILL_ASSIST_URI to listOf(
                    FillAssistRules.HostRule(
                        category = "account-login",
                        fields = mapOf(
                            "username" to listOf(
                                FillAssistRules.SelectorClause(
                                    tag = "input",
                                    id = "user",
                                    name = null,
                                    type = null,
                                    role = null,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns cardWindowNode
        val cardAutofillView = AutofillView.Card.ExpirationYear(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = FILL_ASSIST_URI,
            ),
            yearValue = null,
        )
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns cardAutofillView

        // Test
        val actual = parser.parse(autofillAppInfo = autofillAppInfo, fillRequest = fillRequest)

        // Verify: heuristic card view used
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = AutofillPartition.Card(views = listOf(cardAutofillView)),
            uri = FILL_ASSIST_URI,
        )
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should fall back to heuristics when fill-assist rules exist but only cover payment-card and a login view is focused`() {
        // Setup: fill-assist enabled with card-only rules, but a login view is focused.
        mutableFillAssistFlagFlow.value = true
        mockIsFillAssistEnabled = true
        every { any<AutofillView>().buildUriOrNull(PACKAGE_NAME) } returns FILL_ASSIST_URI
        every { fillAssistManager.getFillAssistRules() } returns FillAssistRules(
            hostRules = mapOf(
                FILL_ASSIST_URI to listOf(
                    FillAssistRules.HostRule(
                        category = "payment-card",
                        fields = mapOf(
                            "cardNumber" to listOf(
                                FillAssistRules.SelectorClause(
                                    tag = "input",
                                    id = "card-number",
                                    name = null,
                                    type = null,
                                    role = null,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns loginWindowNode
        val loginAutofillView = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = FILL_ASSIST_URI,
            ),
        )
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns loginAutofillView

        // Test
        val actual = parser.parse(autofillAppInfo = autofillAppInfo, fillRequest = fillRequest)

        // Verify: heuristic login view used
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = AutofillPartition.Login(views = listOf(loginAutofillView)),
            uri = FILL_ASSIST_URI,
        )
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should use fill-assist views when rules cover login and a login view is focused`() {
        // Setup: fill-assist with login rules, login view focused.
        // The heuristic and fill-assist paths produce views with DIFFERENT autofillIds so the
        // assertion proves which path was actually taken. If heuristics are used the partition
        // contains loginAutofillId; if fill-assist is used it contains fillAssistAutofillId.
        mutableFillAssistFlagFlow.value = true
        mockIsFillAssistEnabled = true
        every { any<AutofillView>().buildUriOrNull(PACKAGE_NAME) } returns FILL_ASSIST_URI
        every { fillAssistManager.getFillAssistRules() } returns FillAssistRules(
            hostRules = mapOf(
                FILL_ASSIST_URI to listOf(
                    FillAssistRules.HostRule(
                        category = "account-login",
                        fields = mapOf(
                            "username" to listOf(
                                FillAssistRules.SelectorClause(
                                    tag = "input",
                                    id = "user",
                                    name = null,
                                    type = null,
                                    role = null,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val heuristicLoginView = AutofillView.Login.Username(
            data = AutofillView.Data(
                autofillId = loginAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = FILL_ASSIST_URI,
            ),
        )
        val fillAssistAutofillId: AutofillId = mockk()
        val fillAssistLoginData = AutofillView.Data(
            autofillId = fillAssistAutofillId,
            autofillOptions = emptyList(),
            autofillType = AUTOFILL_TYPE,
            isFocused = true,
            textValue = null,
            hasPasswordTerms = false,
            website = WEBSITE,
        )
        every { any<HtmlInfo>().matchesSelectorClause(any()) } returns true
        every {
            loginViewNode.toAutofillViewData(autofillId = loginAutofillId, website = WEBSITE)
        } returns fillAssistLoginData
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns loginWindowNode
        every { loginViewNode.toAutofillView(parentWebsite = any()) } returns heuristicLoginView

        // Test
        val actual = parser.parse(autofillAppInfo = autofillAppInfo, fillRequest = fillRequest)

        // Verify: fill-assist views used — partition contains fillAssistAutofillId.
        // Heuristics would have produced loginAutofillId
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = AutofillPartition.Login(
                views = listOf(AutofillView.Login.Username(data = fillAssistLoginData)),
            ),
            uri = FILL_ASSIST_URI,
        )
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should use fill-assist when rules cover payment-card and a card view is focused`() {
        mutableFillAssistFlagFlow.value = true
        mockIsFillAssistEnabled = true
        every { any<AutofillView>().buildUriOrNull(PACKAGE_NAME) } returns FILL_ASSIST_URI
        every { fillAssistManager.getFillAssistRules() } returns FillAssistRules(
            hostRules = mapOf(
                FILL_ASSIST_URI to listOf(
                    FillAssistRules.HostRule(
                        category = "payment-card",
                        fields = mapOf(
                            "cardNumber" to listOf(
                                FillAssistRules.SelectorClause(
                                    tag = "input",
                                    id = "card-number",
                                    name = null,
                                    type = null,
                                    role = null,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val heuristicCardView = AutofillView.Card.ExpirationYear(
            data = AutofillView.Data(
                autofillId = cardAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = FILL_ASSIST_URI,
            ),
            yearValue = null,
        )
        val fillAssistAutofillId: AutofillId = mockk()
        val fillAssistCardData = AutofillView.Data(
            autofillId = fillAssistAutofillId,
            autofillOptions = emptyList(),
            autofillType = AUTOFILL_TYPE,
            isFocused = true,
            textValue = null,
            hasPasswordTerms = false,
            website = WEBSITE,
        )
        every { any<HtmlInfo>().matchesSelectorClause(any()) } returns true
        every {
            cardViewNode.toAutofillViewData(autofillId = cardAutofillId, website = WEBSITE)
        } returns fillAssistCardData
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns cardWindowNode
        every { cardViewNode.toAutofillView(parentWebsite = any()) } returns heuristicCardView

        // Test
        val actual = parser.parse(autofillAppInfo = autofillAppInfo, fillRequest = fillRequest)

        // Verify: fill-assist views used — partition contains fillAssistAutofillId.
        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = AutofillPartition.Card(
                views = listOf(AutofillView.Card.Number(data = fillAssistCardData)),
            ),
            uri = FILL_ASSIST_URI,
        )
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse should deduplicate when a child node returns an AutofillView with the same autofillId already claimed by a non-Unused parent view`() {
        // Setup
        val sharedAutofillId: AutofillId = mockk()
        val childViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.childCount } returns 0
            every { this@mockk.idPackage } returns null
            every { this@mockk.website } returns null
        }
        val parentViewNode: AssistStructure.ViewNode = mockk {
            every { this@mockk.childCount } returns 1
            every { this@mockk.getChildAt(0) } returns childViewNode
            every { this@mockk.idPackage } returns ID_PACKAGE
            every { this@mockk.website } returns WEBSITE
        }
        val windowNode: AssistStructure.WindowNode = mockk {
            every { this@mockk.rootViewNode } returns parentViewNode
        }
        every { assistStructure.windowNodeCount } returns 1
        every { assistStructure.getWindowNodeAt(0) } returns windowNode

        val sharedAutofillView = AutofillView.Login.Password(
            data = AutofillView.Data(
                autofillId = sharedAutofillId,
                autofillOptions = emptyList(),
                autofillType = AUTOFILL_TYPE,
                isFocused = true,
                textValue = null,
                hasPasswordTerms = false,
                website = URI,
            ),
        )
        every { parentViewNode.toAutofillView(parentWebsite = any()) } returns sharedAutofillView
        every { childViewNode.toAutofillView(parentWebsite = any()) } returns sharedAutofillView

        val expected = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = inlinePresentationSpecs,
            maxInlineSuggestionsCount = MAX_INLINE_SUGGESTION_COUNT,
            packageName = PACKAGE_NAME,
            partition = AutofillPartition.Login(views = listOf(sharedAutofillView)),
            uri = URI,
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

private const val FILL_ASSIST_URI: String = "https://example.com"

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
