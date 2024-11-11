package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.util.toUriOrNull
import com.x8bit.bitwarden.data.autofill.manager.AutofillTotpManager
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import io.mockk.coEvery
import io.mockk.coVerify
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

class AccessibilityCompletionManagerTest {

    private val activity: Activity = mockk {
        every { finishAndRemoveTask() } just runs
    }
    private val accessibilityAutofillManager: AccessibilityAutofillManager = mockk()
    private val totpManager: AutofillTotpManager = mockk()
    private val fakeDispatcherManager: FakeDispatcherManager = FakeDispatcherManager()

    private val accessibilityCompletionManager: AccessibilityCompletionManager =
        AccessibilityCompletionManagerImpl(
            accessibilityAutofillManager = accessibilityAutofillManager,
            totpManager = totpManager,
            dispatcherManager = fakeDispatcherManager,
        )

    @BeforeEach
    fun setup() {
        fakeDispatcherManager.setMain(fakeDispatcherManager.unconfined)
        mockkStatic(
            Intent::getAutofillSelectionDataOrNull,
            String::toUriOrNull,
        )
    }

    @AfterEach
    fun tearDown() {
        fakeDispatcherManager.resetMain()
        unmockkStatic(
            Intent::getAutofillSelectionDataOrNull,
            String::toUriOrNull,
        )
    }

    @Test
    fun `completeAccessibilityAutofill when there is no Intent should finish the Activity`() {
        every { activity.intent } returns null

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = mockk(),
        )

        verify(exactly = 1) {
            activity.intent
            activity.finishAndRemoveTask()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAccessibilityAutofill when there is no AutofillSelectionData should finish the Activity`() {
        val mockIntent: Intent = mockk()
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillSelectionDataOrNull() } returns null

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = mockk(),
        )

        verify(exactly = 1) {
            activity.intent
            mockIntent.getAutofillSelectionDataOrNull()
            activity.finishAndRemoveTask()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAccessibilityAutofill when the AutofillSelectionData is for the incorrect framework should finish the Activity`() {
        val mockIntent: Intent = mockk()
        every { activity.intent } returns mockIntent
        val selectionData = AutofillSelectionData(
            framework = AutofillSelectionData.Framework.AUTOFILL,
            type = AutofillSelectionData.Type.LOGIN,
            uri = "",
        )
        every { mockIntent.getAutofillSelectionDataOrNull() } returns selectionData

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = mockk(),
        )

        verify(exactly = 1) {
            activity.intent
            mockIntent.getAutofillSelectionDataOrNull()
            activity.finishAndRemoveTask()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAccessibilityAutofill when the AutofillSelectionData is missing uri should finish the Activity`() {
        val mockIntent: Intent = mockk()
        every { activity.intent } returns mockIntent
        val selectionData = AutofillSelectionData(
            framework = AutofillSelectionData.Framework.ACCESSIBILITY,
            type = AutofillSelectionData.Type.LOGIN,
            uri = null,
        )
        every { mockIntent.getAutofillSelectionDataOrNull() } returns selectionData

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = mockk(),
        )

        verify(exactly = 1) {
            activity.intent
            mockIntent.getAutofillSelectionDataOrNull()
            activity.finishAndRemoveTask()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAccessibilityAutofill when the AutofillSelectionData contains an invalid uri should finish the Activity`() {
        val mockIntent: Intent = mockk()
        every { activity.intent } returns mockIntent
        val stringUri = "invalid uri"
        every { stringUri.toUriOrNull() } returns null

        val selectionData = AutofillSelectionData(
            framework = AutofillSelectionData.Framework.ACCESSIBILITY,
            type = AutofillSelectionData.Type.LOGIN,
            uri = stringUri,
        )
        every { mockIntent.getAutofillSelectionDataOrNull() } returns selectionData

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = mockk(),
        )

        verify(exactly = 1) {
            activity.intent
            mockIntent.getAutofillSelectionDataOrNull()
            activity.finishAndRemoveTask()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAccessibilityAutofill when the AutofillSelectionData is correct should set the accessibility action, copy the totp and finish the activity`() {
        val cipherView: CipherView = mockk()
        val mockIntent: Intent = mockk()
        every { activity.intent } returns mockIntent
        val stringUri = "androidapp://com.x8bit.bitwarden"
        val uri: Uri = mockk()
        every { stringUri.toUriOrNull() } returns uri
        val selectionData = AutofillSelectionData(
            framework = AutofillSelectionData.Framework.ACCESSIBILITY,
            type = AutofillSelectionData.Type.LOGIN,
            uri = stringUri,
        )
        every { mockIntent.getAutofillSelectionDataOrNull() } returns selectionData
        every {
            accessibilityAutofillManager.accessibilityAction = AccessibilityAction.AttemptFill(
                cipherView = cipherView,
                uri = uri,
            )
        } just runs
        coEvery { totpManager.tryCopyTotpToClipboard(cipherView = cipherView) } just runs

        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify(exactly = 1) {
            activity.intent
            mockIntent.getAutofillSelectionDataOrNull()
            accessibilityAutofillManager.accessibilityAction = AccessibilityAction.AttemptFill(
                cipherView = cipherView,
                uri = uri,
            )
            activity.finishAndRemoveTask()
        }
        coVerify(exactly = 1) {
            totpManager.tryCopyTotpToClipboard(cipherView = cipherView)
        }
    }
}
