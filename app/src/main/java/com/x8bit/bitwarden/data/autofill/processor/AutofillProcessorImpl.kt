package com.x8bit.bitwarden.data.autofill.processor

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The default implementation of [AutofillProcessor]. Its purpose is to handle autofill related
 * processing.
 */
class AutofillProcessorImpl(
    dispatcherManager: DispatcherManager,
    private val filledDataBuilder: FilledDataBuilder,
    private val fillResponseBuilder: FillResponseBuilder,
    private val parser: AutofillParser,
) : AutofillProcessor {

    /**
     * The coroutine scope for launching asynchronous operations.
     */
    private val scope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    override fun processFillRequest(
        autofillAppInfo: AutofillAppInfo,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
        request: FillRequest,
    ) {
        // Set the listener so that any long running work is cancelled when it is no longer needed.
        cancellationSignal.setOnCancelListener { scope.cancel() }
        // Process the OS data and handle invoking the callback with the result.
        process(
            autofillAppInfo = autofillAppInfo,
            fillCallback = fillCallback,
            fillRequest = request,
        )
    }

    /**
     * Process the [fillRequest] and invoke the [FillCallback] with the response.
     */
    private fun process(
        autofillAppInfo: AutofillAppInfo,
        fillCallback: FillCallback,
        fillRequest: FillRequest,
    ) {
        // Parse the OS data into an [AutofillRequest] for easier processing.
        val autofillRequest = parser.parse(
            autofillAppInfo = autofillAppInfo,
            fillRequest = fillRequest,
        )
        when (autofillRequest) {
            is AutofillRequest.Fillable -> {
                scope.launch {
                    // Fulfill the [autofillRequest].
                    val filledData = filledDataBuilder.build(
                        autofillRequest = autofillRequest,
                    )

                    // Load the [filledData] into a [FillResponse].
                    val response = fillResponseBuilder.build(
                        autofillAppInfo = autofillAppInfo,
                        filledData = filledData,
                    )

                    fillCallback.onSuccess(response)
                }
            }

            AutofillRequest.Unfillable -> {
                // If we are unable to fulfill the request, we should invoke the callback
                // with null. This effectively disables autofill for this view set and
                // allows the [AutofillService] to be unbound.
                fillCallback.onSuccess(null)
            }
        }
    }
}
