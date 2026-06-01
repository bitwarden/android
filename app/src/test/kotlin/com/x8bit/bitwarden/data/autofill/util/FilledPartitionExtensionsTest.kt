package com.x8bit.bitwarden.data.autofill.util

import android.content.Context
import android.content.IntentSender
import android.content.res.Resources
import android.service.autofill.Dataset
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import com.bitwarden.core.data.util.mockBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.ui.autofill.buildAutofillRemoteViews
import com.x8bit.bitwarden.ui.autofill.util.createCipherInlinePresentationOrNull
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

class FilledPartitionExtensionsTest {
    private val autofillCipher: AutofillCipher = mockk {
        every { this@mockk.name } returns CIPHER_NAME
    }
    private val res: Resources = mockk()
    private val context: Context = mockk {
        every { this@mockk.resources } returns res
    }
    private val dataset: Dataset = mockk()
    private val filledItem: FilledItem = mockk()
    private val inlinePresentationSpec: InlinePresentationSpec = mockk()
    private val filledPartition = FilledPartition(
        autofillCipher = autofillCipher,
        filledItems = listOf(
            filledItem,
        ),
        inlinePresentationSpec = inlinePresentationSpec,
    )
    private val presentations: Presentations = mockk()
    private val remoteViews: RemoteViews = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(Dataset.Builder::class)
        mockkConstructor(Presentations.Builder::class)
        mockkStatic(::buildAutofillRemoteViews)
        mockkStatic(FilledItem::applyToDatasetPostTiramisu)
        mockkStatic(FilledItem::applyToDatasetPreTiramisu)
        mockkStatic(InlinePresentationSpec::createCipherInlinePresentationOrNull)
        every { anyConstructed<Dataset.Builder>().build() } returns dataset
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Dataset.Builder::class)
        unmockkConstructor(Presentations.Builder::class)
        unmockkStatic(::buildAutofillRemoteViews)
        unmockkStatic(FilledItem::applyToDatasetPostTiramisu)
        unmockkStatic(FilledItem::applyToDatasetPreTiramisu)
        unmockkStatic(InlinePresentationSpec::createCipherInlinePresentationOrNull)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildDataset should applyToDatasetPostTiramisu and set auth when sdkInt is at least 33 and has authIntentSender`() {
        // Setup
        val authIntentSender: IntentSender = mockk()
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 34,
        )
        val inlinePresentation: InlinePresentation = mockk()
        every {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns remoteViews
        every {
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns inlinePresentation
        mockBuilder<Presentations.Builder> { it.setInlinePresentation(inlinePresentation) }
        mockBuilder<Presentations.Builder> { it.setMenuPresentation(remoteViews) }
        every {
            filledItem.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
        } just runs
        every { anyConstructed<Presentations.Builder>().build() } returns presentations

        // Test
        val actual = filledPartition.buildDataset(
            authIntentSender = authIntentSender,
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            anyConstructed<Presentations.Builder>().setInlinePresentation(inlinePresentation)
            anyConstructed<Presentations.Builder>().setMenuPresentation(remoteViews)
            anyConstructed<Presentations.Builder>().build()
            filledItem.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildDataset should applyToDatasetPostTiramisu and doesn't set auth when sdkInt is at least 33 and null authIntentSender`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 34,
        )
        val inlinePresentation: InlinePresentation = mockk()
        every {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns remoteViews
        every {
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns inlinePresentation
        mockBuilder<Presentations.Builder> { it.setInlinePresentation(inlinePresentation) }
        mockBuilder<Presentations.Builder> { it.setMenuPresentation(remoteViews) }
        every {
            filledItem.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
        } just runs
        every { anyConstructed<Presentations.Builder>().build() } returns presentations

        // Test
        val actual = filledPartition.buildDataset(
            authIntentSender = null,
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            anyConstructed<Presentations.Builder>().setInlinePresentation(inlinePresentation)
            anyConstructed<Presentations.Builder>().setMenuPresentation(remoteViews)
            anyConstructed<Presentations.Builder>().build()
            filledItem.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildDataset should skip inline and applyToDatasetPreTiramisu when sdkInt is less than 30`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 18,
        )
        every {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns remoteViews
        every {
            filledItem.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
        } just runs

        // Test
        val actual = filledPartition.buildDataset(
            authIntentSender = null,
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            filledItem.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Suppress("Deprecation", "MaxLineLength")
    @Test
    fun `buildDataset should skip inline and applyToDatasetPreTiramisu when sdkInt is less than 33 but more than 29`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 30,
        )
        val inlinePresentation: InlinePresentation = mockk()
        every {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns remoteViews
        every {
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
        } returns inlinePresentation
        mockBuilder<Dataset.Builder> { it.setInlinePresentation(inlinePresentation) }
        every {
            filledItem.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
        } just runs

        // Test
        val actual = filledPartition.buildDataset(
            authIntentSender = null,
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            inlinePresentationSpec.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            anyConstructed<Dataset.Builder>().setInlinePresentation(inlinePresentation)
            filledItem.applyToDatasetPreTiramisu(
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
