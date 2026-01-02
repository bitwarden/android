package com.x8bit.bitwarden.data.autofill.util

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.content.res.Resources
import android.service.autofill.Dataset
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import com.bitwarden.core.data.util.mockBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.ui.autofill.buildVaultItemAutofillRemoteViews
import com.x8bit.bitwarden.ui.autofill.util.createVaultItemInlinePresentationOrNull
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilledDataExtensionsTest {
    private val autofillCipher: AutofillCipher = mockk {
        every { this@mockk.name } returns CIPHER_NAME
    }
    private val autofillId: AutofillId = mockk()
    private val autofillValue: AutofillValue = mockk()
    private val res: Resources = mockk()
    private val context: Context = mockk {
        every { this@mockk.resources } returns res
    }
    private val dataset: Dataset = mockk()
    private val filledItem: FilledItem = mockk {
        every { autofillId } returns mockk()
    }
    private val filledItemPlaceholder = FilledItem(
        autofillId = autofillId,
        value = autofillValue,
    )
    private val inlinePresentationSpec: InlinePresentationSpec = mockk()
    private val filledPartition = FilledPartition(
        autofillCipher = autofillCipher,
        filledItems = listOf(
            filledItem,
        ),
        inlinePresentationSpec = inlinePresentationSpec,
    )
    private val filledData = FilledData(
        filledPartitions = listOf(filledPartition),
        ignoreAutofillIds = emptyList(),
        originalPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Username(
                    data = AutofillView.Data(
                        autofillId = autofillId,
                        autofillOptions = emptyList(),
                        autofillType = View.AUTOFILL_TYPE_TEXT,
                        isFocused = true,
                        textValue = null,
                        hasPasswordTerms = false,
                        website = "uri",
                    ),
                ),
            ),
        ),
        uri = "uri",
        vaultItemInlinePresentationSpec = inlinePresentationSpec,
        isVaultLocked = false,
    )
    private val mockIntentSender: IntentSender = mockk()
    private val pendingIntent: PendingIntent = mockk {
        every { intentSender } returns mockIntentSender
    }
    private val presentations: Presentations = mockk()
    private val remoteViews: RemoteViews = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(Dataset.Builder::class)
        mockkConstructor(Presentations.Builder::class)
        mockkStatic(::buildVaultItemAutofillRemoteViews)
        mockkStatic(AutofillValue::forText)
        mockkStatic(FilledItem::applyToDatasetPostTiramisu)
        mockkStatic(FilledItem::applyToDatasetPreTiramisu)
        mockkStatic(InlinePresentationSpec::createVaultItemInlinePresentationOrNull)
        mockkStatic(PendingIntent::class)
        every { anyConstructed<Dataset.Builder>().build() } returns dataset
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Dataset.Builder::class)
        unmockkConstructor(Presentations.Builder::class)
        unmockkStatic(::buildVaultItemAutofillRemoteViews)
        unmockkStatic(AutofillValue::forText)
        unmockkStatic(FilledItem::applyToDatasetPostTiramisu)
        unmockkStatic(FilledItem::applyToDatasetPreTiramisu)
        unmockkStatic(InlinePresentationSpec::createVaultItemInlinePresentationOrNull)
        unmockkStatic(PendingIntent::class)
    }

    @Test
    fun `fillableAutofillIds should return a list derived from the original partition`() {
        assertEquals(
            listOf(autofillId),
            filledData.fillableAutofillIds,
        )
    }

    @Test
    fun `buildVaultItemDataset should applyToDatasetPostTiramisu when sdkInt is at least 33`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 34,
        )
        val inlinePresentation: InlinePresentation = mockk()
        every { AutofillValue.forText(any()) } returns autofillValue
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntent
        every {
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
        } returns remoteViews
        every {
            inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                pendingIntent = any(),
                isLocked = false,
            )
        } returns inlinePresentation
        mockBuilder<Dataset.Builder> { it.setAuthentication(mockIntentSender) }
        mockBuilder<Presentations.Builder> { it.setInlinePresentation(inlinePresentation) }
        mockBuilder<Presentations.Builder> { it.setMenuPresentation(remoteViews) }
        every {
            filledItemPlaceholder.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
        } just runs
        every { anyConstructed<Presentations.Builder>().build() } returns presentations

        // Test
        val actual = filledData.buildVaultItemDataset(
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            AutofillValue.forText("PLACEHOLDER")
            PendingIntent.getActivity(any(), any(), any(), any())
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
            inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                pendingIntent = any(),
                isLocked = false,
            )
            anyConstructed<Dataset.Builder>().setAuthentication(mockIntentSender)
            anyConstructed<Presentations.Builder>().setInlinePresentation(inlinePresentation)
            anyConstructed<Presentations.Builder>().setMenuPresentation(remoteViews)
            anyConstructed<Presentations.Builder>().build()
            filledItemPlaceholder.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildVaultItemDataset should skip inline and applyToDatasetPreTiramisu when sdkInt is less than 30`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 18,
        )
        every { AutofillValue.forText(any()) } returns autofillValue
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntent
        every {
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
        } returns remoteViews
        mockBuilder<Dataset.Builder> { it.setAuthentication(mockIntentSender) }
        every {
            filledItemPlaceholder.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
        } just runs

        // Test
        val actual = filledData.buildVaultItemDataset(
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            AutofillValue.forText("PLACEHOLDER")
            PendingIntent.getActivity(any(), any(), any(), any())
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
            filledItemPlaceholder.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Suppress("Deprecation", "MaxLineLength")
    @Test
    fun `buildVaultItemDataset should skip inline and applyToDatasetPreTiramisu when sdkInt is less than 33 but more than 29`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 30,
        )
        val inlinePresentation: InlinePresentation = mockk()
        every { AutofillValue.forText(any()) } returns autofillValue
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntent
        every {
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
        } returns remoteViews
        every {
            inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                pendingIntent = any(),
                isLocked = false,
            )
        } returns inlinePresentation
        mockBuilder<Dataset.Builder> { it.setAuthentication(mockIntentSender) }
        mockBuilder<Dataset.Builder> { it.setInlinePresentation(inlinePresentation) }
        every {
            filledItemPlaceholder.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
        } just runs

        // Test
        val actual = filledData.buildVaultItemDataset(
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            AutofillValue.forText("PLACEHOLDER")
            PendingIntent.getActivity(any(), any(), any(), any())
            buildVaultItemAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                isLocked = false,
            )
            inlinePresentationSpec.createVaultItemInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                pendingIntent = any(),
                isLocked = false,
            )
            anyConstructed<Dataset.Builder>().setInlinePresentation(inlinePresentation)
            filledItemPlaceholder.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    companion object {
        private const val CIPHER_NAME: String = "Autofill Cipher"
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
