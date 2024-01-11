package com.x8bit.bitwarden.data.autofill.util

import android.content.Context
import android.content.res.Resources
import android.service.autofill.Dataset
import android.service.autofill.Presentations
import android.widget.RemoteViews
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.util.mockBuilder
import com.x8bit.bitwarden.ui.autofill.buildAutofillRemoteViews
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
    private val res: Resources = mockk()
    private val context: Context = mockk {
        every { this@mockk.resources } returns res
    }
    private val dataset: Dataset = mockk()
    private val filledItem: FilledItem = mockk()
    private val filledPartition = FilledPartition(
        filledItems = listOf(
            filledItem,
        ),
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
        every { anyConstructed<Dataset.Builder>().build() } returns dataset
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Dataset.Builder::class)
        unmockkConstructor(Presentations.Builder::class)
        unmockkStatic(::buildAutofillRemoteViews)
        unmockkStatic(FilledItem::applyToDatasetPostTiramisu)
        unmockkStatic(FilledItem::applyToDatasetPreTiramisu)
    }

    @Test
    fun `buildDataset should applyToDatasetPostTiramisu when sdkInt is at least 33`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 34,
        )
        val title = "Bitwarden"
        every { res.getString(R.string.app_name) } returns title
        every {
            buildAutofillRemoteViews(
                packageName = PACKAGE_NAME,
                title = title,
            )
        } returns remoteViews
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
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                packageName = PACKAGE_NAME,
                title = title,
            )
            anyConstructed<Presentations.Builder>().setMenuPresentation(remoteViews)
            anyConstructed<Presentations.Builder>().build()
            filledItem.applyToDatasetPostTiramisu(
                datasetBuilder = any(),
                presentations = presentations,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    @Test
    fun `buildDataset should applyToDatasetPreTiramisu when sdkInt is less than 33`() {
        // Setup
        val autofillAppInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 18,
        )
        val title = "Bitwarden"
        every { res.getString(R.string.app_name) } returns title
        every {
            buildAutofillRemoteViews(
                packageName = PACKAGE_NAME,
                title = title,
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
            autofillAppInfo = autofillAppInfo,
        )

        // Verify
        assertEquals(dataset, actual)
        verify(exactly = 1) {
            buildAutofillRemoteViews(
                packageName = PACKAGE_NAME,
                title = title,
            )
            filledItem.applyToDatasetPreTiramisu(
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            anyConstructed<Dataset.Builder>().build()
        }
    }

    companion object {
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
