package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.service.autofill.Dataset
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionResultIntent
import com.x8bit.bitwarden.data.autofill.util.getAutofillAssistStructureOrNull
import com.x8bit.bitwarden.data.autofill.util.toAutofillAppInfo
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

class AutofillCompletionManagerTest {
    private val activity: Activity = mockk {
        every { finish() } just runs
        every { setResult(any()) } just runs
        every { setResult(any(), any()) } just runs
    }
    private val assistStructure: AssistStructure = mockk()
    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val autofillParser: AutofillParser = mockk()
    private val cipherView: CipherView = mockk()
    private val dataset: Dataset = mockk()
    private val dispatcherManager = FakeDispatcherManager()
    private val fillableRequest: AutofillRequest.Fillable = mockk()
    private val filledDataBuilder: FilledDataBuilder = mockk()
    private val filledPartition: FilledPartition = mockk()
    private val mockIntent: Intent = mockk()
    private val resultIntent: Intent = mockk()

    private val autofillCompletionManager: AutofillCompletionManager =
        AutofillCompletionManagerImpl(
            autofillParser = autofillParser,
            dispatcherManager = dispatcherManager,
            filledDataBuilderProvider = { filledDataBuilder },
        )

    @BeforeEach
    fun setUp() {
        dispatcherManager.setMain(dispatcherManager.unconfined)
        mockkStatic(::createAutofillSelectionResultIntent)
        mockkStatic(Activity::toAutofillAppInfo)
        mockkStatic(FilledPartition::buildDataset)
        mockkStatic(Intent::getAutofillAssistStructureOrNull)
        every { activity.toAutofillAppInfo() } returns autofillAppInfo
    }

    @AfterEach
    fun tearDown() {
        dispatcherManager.resetMain()
        unmockkStatic(::createAutofillSelectionResultIntent)
        unmockkStatic(Activity::toAutofillAppInfo)
        unmockkStatic(FilledPartition::buildDataset)
        unmockkStatic(Intent::getAutofillAssistStructureOrNull)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there is no Intent present should cancel and finish the Activity`() {
        every { activity.intent } returns null

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there is no AssistStructure present should cancel and finish the Activity`() {
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns null

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when the request is not fillable should cancel and finish the Activity`() {
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns AutofillRequest.Unfillable

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there are no filled partitions should cancel and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns emptyList()
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there is a filled partition should build a dataset, place it in a result Intent, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every { filledPartition.buildDataset(autofillAppInfo = autofillAppInfo) } returns dataset
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(autofillAppInfo = autofillAppInfo)
            createAutofillSelectionResultIntent(dataset = dataset)
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }
}
