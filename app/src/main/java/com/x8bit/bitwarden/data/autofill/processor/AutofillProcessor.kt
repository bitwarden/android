package com.x8bit.bitwarden.data.autofill.processor

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo

/**
 * A class to handle autofill request processing. This includes save and fill requests.
 */
interface AutofillProcessor {
    /**
     * Process the autofill [FillRequest] and invoke the [fillCallback] with the result.
     *
     * @param autofillAppInfo App data that is required for the autofill [request] processing.
     * @param cancellationSignal A signal to listen to for cancellations.
     * @param fillCallback The callback to invoke when the [request] has been processed.
     * @param request The request data from the OS that contains data about the autofill hierarchy.
     */
    fun processFillRequest(
        autofillAppInfo: AutofillAppInfo,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
        request: FillRequest,
    )

    /**
     * Process the autofill [SaveRequest] and invoke the [saveCallback] with the result.
     *
     * @param autofillAppInfo App data that is required for the autofill [request] processing.
     * @param request The request data from the OS that contains data about the autofill hierarchy.
     * @param saveCallback The callback to invoke when the [request] has been processed.
     */
    fun processSaveRequest(
        autofillAppInfo: AutofillAppInfo,
        request: SaveRequest,
        saveCallback: SaveCallback,
    )
}
