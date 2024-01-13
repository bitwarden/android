package com.x8bit.bitwarden.data.autofill.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.util.mockBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FillResponseBuilderTest {
    private lateinit var fillResponseBuilder: FillResponseBuilder

    private val context: Context = mockk()
    private val dataset: Dataset = mockk()
    private val fillResponse: FillResponse = mockk()
    private val appInfo: AutofillAppInfo = AutofillAppInfo(
        context = context,
        packageName = PACKAGE_NAME,
        sdkInt = 17,
    )
    private val filledPartitionOne: FilledPartition = mockk {
        every { this@mockk.filledItems } returns listOf(mockk())
    }
    private val filledPartitionTwo: FilledPartition = mockk {
        every { this@mockk.filledItems } returns emptyList()
    }

    @BeforeEach
    fun setup() {
        mockkConstructor(FillResponse.Builder::class)
        mockkStatic(FilledPartition::buildDataset)
        every { anyConstructed<FillResponse.Builder>().build() } returns fillResponse

        fillResponseBuilder = FillResponseBuilderImpl()
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(FillResponse.Builder::class)
        mockkStatic(FilledPartition::buildDataset)
    }

    @Test
    fun `build should return null when filledPartitions is empty`() {
        // Test
        val filledData = FilledData(
            filledPartitions = emptyList(),
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
    fun `build should return null when filledPartitions contains no views`() {
        // Test
        val filledPartitions = FilledPartition(
            autofillCipher = mockk(),
            filledItems = emptyList(),
        )
        val filledData = FilledData(
            filledPartitions = listOf(
                filledPartitions,
            ),
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
    fun `build should apply FilledPartitions with filledItems and ignore ignoreAutofillIds`() {
        // Setup
        val ignoredAutofillIdOne: AutofillId = mockk()
        val ignoredAutofillIdTwo: AutofillId = mockk()
        val ignoreAutofillIds = listOf(
            ignoredAutofillIdOne,
            ignoredAutofillIdTwo,
        )
        val filledPartitions = listOf(
            filledPartitionOne,
            filledPartitionTwo,
        )
        val filledData = FilledData(
            filledPartitions = filledPartitions,
            ignoreAutofillIds = ignoreAutofillIds,
        )
        every {
            filledPartitionOne.buildDataset(
                autofillAppInfo = appInfo,
            )
        } returns dataset
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
            filledPartitionOne.buildDataset(
                autofillAppInfo = appInfo,
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
