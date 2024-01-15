package com.x8bit.bitwarden.data.autofill.processor

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillProcessorTest {
    private lateinit var autofillProcessor: AutofillProcessor

    private val dispatcherManager: DispatcherManager = mockk()
    private val cancellationSignal: CancellationSignal = mockk()
    private val filledDataBuilder: FilledDataBuilder = mockk()
    private val fillResponseBuilder: FillResponseBuilder = mockk()
    private val parser: AutofillParser = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val appInfo: AutofillAppInfo = AutofillAppInfo(
        context = mockk(),
        packageName = "com.x8bit.bitwarden",
        sdkInt = 42,
    )
    private val fillCallback: FillCallback = mockk()

    @BeforeEach
    fun setup() {
        every { dispatcherManager.unconfined } returns testDispatcher

        autofillProcessor = AutofillProcessorImpl(
            dispatcherManager = dispatcherManager,
            filledDataBuilder = filledDataBuilder,
            fillResponseBuilder = fillResponseBuilder,
            parser = parser,
        )
    }

    @AfterEach
    fun teardown() {
        verify(exactly = 1) {
            dispatcherManager.unconfined
        }
        confirmVerified(
            cancellationSignal,
            dispatcherManager,
            filledDataBuilder,
            fillResponseBuilder,
            parser,
        )
    }

    @Test
    fun `processFillRequest should invoke callback with null when parse returns Unfillable`() {
        // Setup
        val autofillRequest = AutofillRequest.Unfillable
        val fillRequest: FillRequest = mockk()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { parser.parse(fillRequest) } returns autofillRequest
        every { fillCallback.onSuccess(null) } just runs

        // Test
        autofillProcessor.processFillRequest(
            autofillAppInfo = appInfo,
            cancellationSignal = cancellationSignal,
            fillCallback = fillCallback,
            request = fillRequest,
        )

        // Verify
        verify(exactly = 1) {
            cancellationSignal.setOnCancelListener(any())
            parser.parse(fillRequest)
            fillCallback.onSuccess(null)
        }
    }

    @Test
    fun `processFillRequest should invoke callback with filled response when has filledItems`() =
        runTest {
            // Setup
            val fillRequest: FillRequest = mockk()
            val filledData = FilledData(
                filledPartitions = listOf(mockk()),
                ignoreAutofillIds = emptyList(),
            )
            val fillResponse: FillResponse = mockk()
            val autofillRequest: AutofillRequest.Fillable = mockk()
            coEvery {
                filledDataBuilder.build(
                    autofillRequest = autofillRequest,
                )
            } returns filledData
            every { cancellationSignal.setOnCancelListener(any()) } just runs
            every { parser.parse(fillRequest) } returns autofillRequest
            every {
                fillResponseBuilder.build(
                    autofillAppInfo = appInfo,
                    filledData = filledData,
                )
            } returns fillResponse
            every { fillCallback.onSuccess(fillResponse) } just runs

            // Test
            autofillProcessor.processFillRequest(
                autofillAppInfo = appInfo,
                cancellationSignal = cancellationSignal,
                fillCallback = fillCallback,
                request = fillRequest,
            )

            // Verify
            coVerify(exactly = 1) {
                filledDataBuilder.build(
                    autofillRequest = autofillRequest,
                )
            }
            verify(exactly = 1) {
                cancellationSignal.setOnCancelListener(any())
                parser.parse(fillRequest)
                fillResponseBuilder.build(
                    autofillAppInfo = appInfo,
                    filledData = filledData,
                )
                fillCallback.onSuccess(fillResponse)
            }
        }
}
