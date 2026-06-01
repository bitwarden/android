package com.x8bit.bitwarden.data.autofill.util

import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.bitwarden.core.data.util.mockBuilder
import com.x8bit.bitwarden.data.autofill.model.FilledItem
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
    private val autofillValue: AutofillValue = mockk()
    private val datasetBuilder: Dataset.Builder = mockk()
    private val field: Field = mockk()
    private val filledItem = FilledItem(
        autofillId = autofillId,
        value = autofillValue,
    )
    private val presentations: Presentations = mockk()
    private val remoteViews: RemoteViews = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(Field.Builder::class)
        every { anyConstructed<Field.Builder>().build() } returns field
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Field.Builder::class)
    }

    @Suppress("Deprecation")
    @Test
    fun `applyToDatasetPreTiramisu should use setValue to set RemoteViews`() {
        // Setup
        every {
            datasetBuilder.setValue(
                autofillId,
                autofillValue,
                remoteViews,
            )
        } returns datasetBuilder

        // Test
        filledItem.applyToDatasetPreTiramisu(
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )

        // Verify
        verify(exactly = 1) {
            datasetBuilder.setValue(
                autofillId,
                autofillValue,
                remoteViews,
            )
        }
    }

    @Test
    fun `applyToDatasetPostTiramisu should use setField to set presentations`() {
        // Setup
        mockBuilder<Field.Builder> { it.setValue(autofillValue) }
        mockBuilder<Field.Builder> { it.setPresentations(presentations) }
        every {
            datasetBuilder.setField(
                autofillId,
                field,
            )
        } returns datasetBuilder

        // Test
        filledItem.applyToDatasetPostTiramisu(
            datasetBuilder = datasetBuilder,
            presentations = presentations,
        )

        // Verify
        verify(exactly = 1) {
            anyConstructed<Field.Builder>().setValue(autofillValue)
            anyConstructed<Field.Builder>().setPresentations(presentations)
            datasetBuilder.setField(
                autofillId,
                field,
            )
        }
    }
}
