package com.x8bit.bitwarden.data.autofill.util

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.util.mockBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilledItemExtensionsTest {
    private val autofillId: AutofillId = mockk()
    private val context: Context = mockk()
    private val datasetBuilder: Dataset.Builder = mockk()
    private val field: Field = mockk()
    private val filledItem = FilledItem(
        autofillId = autofillId,
    )
    private val presentations: Presentations = mockk()
    private val remoteViews: RemoteViews = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(Dataset.Builder::class)
        mockkConstructor(Presentations.Builder::class)
        mockkConstructor(Field.Builder::class)
        every { anyConstructed<Presentations.Builder>().build() } returns presentations
        every { anyConstructed<Field.Builder>().build() } returns field
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Dataset.Builder::class)
        unmockkConstructor(Presentations.Builder::class)
        unmockkConstructor(Field.Builder::class)
    }

    @Suppress("Deprecation")
    @Test
    fun `applyOverlayToDataset should use setValue to set RemoteViews when before tiramisu`() {
        // Setup
        val appInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 1,
        )
        every {
            datasetBuilder.setValue(
                autofillId,
                null,
                remoteViews,
            )
        } returns datasetBuilder

        // Test
        filledItem.applyOverlayToDataset(
            appInfo = appInfo,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )

        // Verify
        verify(exactly = 1) {
            datasetBuilder.setValue(
                autofillId,
                null,
                remoteViews,
            )
        }
    }

    @Test
    fun `applyOverlayToDataset should use setField to set Presentation on or after Tiramisu`() {
        // Setup
        val appInfo = AutofillAppInfo(
            context = context,
            packageName = PACKAGE_NAME,
            sdkInt = 34,
        )
        mockBuilder<Presentations.Builder> { it.setDialogPresentation(remoteViews) }
        mockBuilder<Field.Builder> { it.setPresentations(presentations) }
        every {
            datasetBuilder.setField(
                autofillId,
                field,
            )
        } returns datasetBuilder

        // Test
        filledItem.applyOverlayToDataset(
            appInfo = appInfo,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )

        // Verify
        verify(exactly = 1) {
            anyConstructed<Presentations.Builder>().setDialogPresentation(remoteViews)
            anyConstructed<Field.Builder>().setPresentations(presentations)
            datasetBuilder.setField(
                autofillId,
                field,
            )
        }
    }

    companion object {
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
