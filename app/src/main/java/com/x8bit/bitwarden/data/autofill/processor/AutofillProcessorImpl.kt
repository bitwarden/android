package com.x8bit.bitwarden.data.autofill.processor

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.SaveInfoBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.util.createAutofillSavedItemIntentSender
import com.x8bit.bitwarden.data.autofill.util.toAutofillSaveItem
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The default implementation of [AutofillProcessor]. Its purpose is to handle autofill related
 * processing.
 */
@Suppress("LongParameterList")
class AutofillProcessorImpl(
    dispatcherManager: DispatcherManager,
    private val policyManager: PolicyManager,
    private val filledDataBuilder: FilledDataBuilder,
    private val fillResponseBuilder: FillResponseBuilder,
    private val parser: AutofillParser,
    private val saveInfoBuilder: SaveInfoBuilder,
    private val settingsRepository: SettingsRepository,
) : AutofillProcessor {

    /**
     * The coroutine scope for launching asynchronous operations.
     */
    private val scope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    /**
     * The job being used to process the fill request.
     */
    private var job: Job = Job().apply { complete() }

    override fun processFillRequest(
        autofillAppInfo: AutofillAppInfo,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
        request: FillRequest,
    ) {
        // Set the listener so that any long running work is cancelled when it is no longer needed.
        cancellationSignal.setOnCancelListener { job.cancel() }
        // Process the OS data and handle invoking the callback with the result.
        job.cancel()
        job = scope.launch {
            process(
                autofillAppInfo = autofillAppInfo,
                fillCallback = fillCallback,
                fillRequest = request,
            )
        }
    }

    override fun processSaveRequest(
        autofillAppInfo: AutofillAppInfo,
        request: SaveRequest,
        saveCallback: SaveCallback,
    ) {
        if (settingsRepository.isAutofillSavePromptDisabled) {
            saveCallback.onSuccess()
            return
        }

        if (policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP).any()) {
            saveCallback.onSuccess()
            return
        }

        request
            .fillContexts
            .lastOrNull()
            ?.structure
            ?.let { assistStructure ->
                val autofillRequest = parser.parse(
                    assistStructure = assistStructure,
                    autofillAppInfo = autofillAppInfo,
                )

                when (autofillRequest) {
                    is AutofillRequest.Fillable -> {
                        val intentSender = createAutofillSavedItemIntentSender(
                            autofillAppInfo = autofillAppInfo,
                            autofillSaveItem = autofillRequest.toAutofillSaveItem(),
                        )

                        saveCallback.onSuccess(intentSender)
                    }

                    AutofillRequest.Unfillable -> saveCallback.onSuccess()
                }
            }
            ?: saveCallback.onSuccess()
    }

    /**
     * Process the [fillRequest] and invoke the [FillCallback] with the response.
     */
    private suspend fun process(
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
                // Fulfill the [autofillRequest].
                val filledData = filledDataBuilder.build(
                    autofillRequest = autofillRequest,
                )
                val saveInfo = saveInfoBuilder.build(
                    autofillPartition = autofillRequest.partition,
                    fillRequest = fillRequest,
                    packageName = autofillRequest.packageName,
                )

                // Load the filledData and saveInfo into a FillResponse.
                val response = fillResponseBuilder.build(
                    autofillAppInfo = autofillAppInfo,
                    filledData = filledData,
                    saveInfo = saveInfo,
                )

                @Suppress("TooGenericExceptionCaught")
                try {
                    fillCallback.onSuccess(response)
                } catch (e: RuntimeException) {
                    // This is to catch any TransactionTooLargeExceptions that could occur here.
                    // These exceptions get wrapped as a RuntimeException.
                    Timber.e(e, "Autofill Error")
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
