package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.util.fillTextField
import com.x8bit.bitwarden.data.autofill.accessibility.util.isSystemPackage
import com.x8bit.bitwarden.data.autofill.accessibility.util.shouldSkipPackage
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionIntent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BitwardenAccessibilityProcessorTest {

    private val context: Context = mockk {
        every { startActivity(any()) } just runs
    }
    private val accessibilityParser: AccessibilityParser = mockk()
    private val accessibilityAutofillManager: AccessibilityAutofillManager = mockk()
    private val launcherPackageNameManager: LauncherPackageNameManager = mockk()
    private val powerManager: PowerManager = mockk()

    private val bitwardenAccessibilityProcessor: BitwardenAccessibilityProcessor =
        BitwardenAccessibilityProcessorImpl(
            context = context,
            accessibilityParser = accessibilityParser,
            accessibilityAutofillManager = accessibilityAutofillManager,
            launcherPackageNameManager = launcherPackageNameManager,
            powerManager = powerManager,
        )

    @BeforeEach
    fun setup() {
        mockkStatic(
            AccessibilityNodeInfo::isSystemPackage,
            AccessibilityNodeInfo::shouldSkipPackage,
        )
        mockkStatic(::createAutofillSelectionIntent)
        mockkStatic(Toast::class)
        every {
            Toast
                .makeText(context, R.string.autofill_tile_uri_not_found, Toast.LENGTH_LONG)
                .show()
        } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            AccessibilityNodeInfo::isSystemPackage,
            AccessibilityNodeInfo::shouldSkipPackage,
        )
        unmockkStatic(::createAutofillSelectionIntent)
        unmockkStatic(Toast::class)
    }

    @Test
    fun `processAccessibilityEvent with null event source should return`() {
        val event = mockk<AccessibilityEvent> {
            every { source } returns null
        }

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 0) {
            powerManager.isInteractive
        }
    }

    @Test
    fun `processAccessibilityEvent with powerManager not interactive should return`() {
        val event = mockk<AccessibilityEvent> {
            every { source } returns mockk()
        }
        every { powerManager.isInteractive } returns false

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 1) {
            powerManager.isInteractive
        }
    }

    @Test
    fun `processAccessibilityEvent with system package should return`() {
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns true
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
        }
        every { powerManager.isInteractive } returns true

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
        }
    }

    @Test
    fun `processAccessibilityEvent with skippable package should return`() {
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns true
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
        }
        every { powerManager.isInteractive } returns true

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
        }
    }

    @Test
    fun `processAccessibilityEvent with launcher package should return`() {
        val testPackageName = "com.google.android.launcher"
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
        }
        every { launcherPackageNameManager.launcherPackages } returns listOf(testPackageName)
        every { powerManager.isInteractive } returns true

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
        }
    }

    @Test
    fun `processAccessibilityEvent without accessibility action should return`() {
        val testPackageName = "com.android.chrome"
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
        }
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every { accessibilityAutofillManager.accessibilityAction } returns null
        every { powerManager.isInteractive } returns true

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { null }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.shouldSkipPackage
            node.isSystemPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
        }
    }

    @Test
    fun `processAccessibilityEvent with mismatched package name should return`() {
        val testPackageName = "com.android.chrome"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns "other.package.name"
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every {
            accessibilityAutofillManager.accessibilityAction
        } returns AccessibilityAction.AttemptParseUri
        every { accessibilityAutofillManager.accessibilityAction = null } just runs
        every { accessibilityParser.parseForUriOrPackageName(rootNode = node) } returns null

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
        }
    }

    @Test
    fun `processAccessibilityEvent with AttemptParseUri and a invalid uri should show a toast`() {
        val testPackageName = "com.android.chrome"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every {
            accessibilityAutofillManager.accessibilityAction
        } returns AccessibilityAction.AttemptParseUri
        every { accessibilityAutofillManager.accessibilityAction = null } just runs
        every { accessibilityParser.parseForUriOrPackageName(rootNode = node) } returns null

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
            accessibilityAutofillManager.accessibilityAction = null
            accessibilityParser.parseForUriOrPackageName(rootNode = node)
            Toast
                .makeText(context, R.string.autofill_tile_uri_not_found, Toast.LENGTH_LONG)
                .show()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processAccessibilityEvent with AttemptParseUri and a valid uri but no fields to fill display toast`() {
        val testPackageName = "com.android.chrome"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every {
            accessibilityAutofillManager.accessibilityAction
        } returns AccessibilityAction.AttemptParseUri
        every { accessibilityAutofillManager.accessibilityAction = null } just runs
        every {
            createAutofillSelectionIntent(
                context = context,
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = any(),
            )
        } returns mockk()
        val uri = mockk<Uri>()
        every { accessibilityParser.parseForUriOrPackageName(rootNode = node) } returns uri
        every {
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
        } returns mockk { every { hasFields } returns false }

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
            accessibilityAutofillManager.accessibilityAction = null
            accessibilityParser.parseForUriOrPackageName(rootNode = node)
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
            Toast
                .makeText(context, R.string.autofill_tile_uri_not_found, Toast.LENGTH_LONG)
                .show()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processAccessibilityEvent with AttemptParseUri and a valid uri should start the main activity`() {
        val testPackageName = "com.android.chrome"
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every {
            accessibilityAutofillManager.accessibilityAction
        } returns AccessibilityAction.AttemptParseUri
        every { accessibilityAutofillManager.accessibilityAction = null } just runs
        every {
            createAutofillSelectionIntent(
                context = context,
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = any(),
            )
        } returns mockk()
        val uri = mockk<Uri>()
        every { accessibilityParser.parseForUriOrPackageName(rootNode = node) } returns uri
        every {
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
        } returns mockk { every { hasFields } returns true }

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
            accessibilityAutofillManager.accessibilityAction = null
            accessibilityParser.parseForUriOrPackageName(rootNode = node)
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
            createAutofillSelectionIntent(
                context = context,
                framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                type = AutofillSelectionData.Type.LOGIN,
                uri = any(),
            )
            context.startActivity(any())
        }
    }

    @Test
    fun `processAccessibilityEvent with AttemptFill and no login data should return`() {
        val testPackageName = "com.android.chrome"
        val cipherView = mockk<CipherView> {
            every { login } returns null
        }
        val uri = mockk<Uri>()
        val attemptFill = AccessibilityAction.AttemptFill(cipherView = cipherView, uri = uri)
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every { accessibilityAutofillManager.accessibilityAction } returns attemptFill
        every { accessibilityAutofillManager.accessibilityAction = null } just runs

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
            accessibilityAutofillManager.accessibilityAction = null
            cipherView.login
        }
    }

    @Test
    fun `processAccessibilityEvent with AttemptFill and valid login data should fill the data`() {
        val testPackageName = "com.android.chrome"
        val testUsername = "testUsername"
        val testPassword = "testPassword1234"
        val loginView = mockk<LoginView> {
            every { username } returns testUsername
            every { password } returns testPassword
        }
        val cipherView = mockk<CipherView> {
            every { login } returns loginView
        }
        val mockUsernameField = mockk<AccessibilityNodeInfo> {
            every { fillTextField(testUsername) } just runs
        }
        val mockPasswordField = mockk<AccessibilityNodeInfo> {
            every { fillTextField(testPassword) } just runs
        }
        val fillableFields = FillableFields(
            usernameField = mockUsernameField,
            passwordFields = listOf(mockPasswordField),
        )
        val uri = mockk<Uri>()
        val attemptFill = AccessibilityAction.AttemptFill(cipherView = cipherView, uri = uri)
        val rootNode = mockk<AccessibilityNodeInfo> {
            every { packageName } returns testPackageName
        }
        val node = mockk<AccessibilityNodeInfo> {
            every { isSystemPackage } returns false
            every { shouldSkipPackage } returns false
            every { packageName } returns testPackageName
        }
        val event = mockk<AccessibilityEvent> {
            every { source } returns node
            every { packageName } returns testPackageName
        }
        every { powerManager.isInteractive } returns true
        every { launcherPackageNameManager.launcherPackages } returns emptyList()
        every { accessibilityAutofillManager.accessibilityAction } returns attemptFill
        every { accessibilityAutofillManager.accessibilityAction = null } just runs
        every {
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
        } returns fillableFields

        bitwardenAccessibilityProcessor.processAccessibilityEvent(event = event) { rootNode }

        verify(exactly = 1) {
            powerManager.isInteractive
            node.isSystemPackage
            node.shouldSkipPackage
            launcherPackageNameManager.launcherPackages
            accessibilityAutofillManager.accessibilityAction
            accessibilityAutofillManager.accessibilityAction = null
            cipherView.login
            accessibilityParser.parseForFillableFields(rootNode = node, uri = uri)
            mockUsernameField.fillTextField(testUsername)
            mockPasswordField.fillTextField(testPassword)
        }
    }
}
