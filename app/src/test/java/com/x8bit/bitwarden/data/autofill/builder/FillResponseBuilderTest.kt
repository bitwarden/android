package com.x8bit.bitwarden.data.autofill.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.util.applyOverlayToDataset
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FillResponseBuilderTest {
    private lateinit var fillResponseBuilder: FillResponseBuilder

    private val dataset: Dataset = mockk()
    private val context: Context = mockk()
    private val fillResponse: FillResponse = mockk()
    private val remoteViews: RemoteViews = mockk()
    private val appInfo: AutofillAppInfo = AutofillAppInfo(
        context = context,
        packageName = PACKAGE_NAME,
        sdkInt = 17,
    )
    private val autofillIdOne: AutofillId = mockk()
    private val autofillIdTwo: AutofillId = mockk()
    private val filledItemOne: FilledItem = mockk {
        every { this@mockk.autofillId } returns autofillIdOne
    }
    private val filledItemTwo: FilledItem = mockk {
        every { this@mockk.autofillId } returns autofillIdTwo
    }

    @BeforeEach
    fun setup() {
        mockkConstructor(Dataset.Builder::class)
        mockkConstructor(FillResponse.Builder::class)
        mockkStatic(::buildAutofillRemoteViews)
        mockkStatic(FilledItem::applyOverlayToDataset)
        every { anyConstructed<Dataset.Builder>().build() } returns dataset
        every { anyConstructed<FillResponse.Builder>().build() } returns fillResponse

        fillResponseBuilder = FillResponseBuilderImpl()
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Dataset.Builder::class)
        unmockkConstructor(FillResponse.Builder::class)
        unmockkStatic(::buildAutofillRemoteViews)
        unmockkStatic(FilledItem::applyOverlayToDataset)
    }

    @Test
    fun `build should return null when filledItems empty`() {
        // Test
        val filledData = FilledData(
            filledItems = emptyList(),
            ignoreAutofillIds = emptyList(),
        )
        val actual = fillResponseBuilder.build(
            autofillAppInfo = appInfo,
            filledData = filledData,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `build should apply filledItems and ignore ignoreAutofillIds`() {
        // Setup
        val ignoredAutofillIdOne: AutofillId = mockk()
        val ignoredAutofillIdTwo: AutofillId = mockk()
        val ignoreAutofillIds = listOf(
            ignoredAutofillIdOne,
            ignoredAutofillIdTwo,
        )
        val filledItems = listOf(
            filledItemOne,
            filledItemTwo,
        )
        val filledData = FilledData(
            filledItems = filledItems,
            ignoreAutofillIds = ignoreAutofillIds,
        )
        every {
            buildAutofillRemoteViews(
                context = context,
                packageName = PACKAGE_NAME,
            )
        } returns remoteViews
        every {
            filledItemOne.applyOverlayToDataset(
                appInfo = appInfo,
                datasetBuilder = anyConstructed(),
                remoteViews = remoteViews,
            )
        } just runs
        every {
            filledItemTwo.applyOverlayToDataset(
                appInfo = appInfo,
                datasetBuilder = anyConstructed(),
                remoteViews = remoteViews,
            )
        } just runs
        mockBuilder<FillResponse.Builder> { it.addDataset(dataset) }
        mockBuilder<FillResponse.Builder> {
            it.setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
        }

        // Test
        val actual = fillResponseBuilder.build(
            autofillAppInfo = appInfo,
            filledData = filledData,
        )

        // Verify
        assertEquals(fillResponse, actual)

        verify(exactly = 1) {
            filledItemOne.applyOverlayToDataset(
                appInfo = appInfo,
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            filledItemTwo.applyOverlayToDataset(
                appInfo = appInfo,
                datasetBuilder = any(),
                remoteViews = remoteViews,
            )
            anyConstructed<FillResponse.Builder>().addDataset(dataset)
            anyConstructed<FillResponse.Builder>().setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
        }
    }

    companion object {
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
