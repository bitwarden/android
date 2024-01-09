package com.x8bit.bitwarden.data.autofill.processor

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo

/**
 * A class to handle autofill request processing. This includes save and fill requests.
 */
interface AutofillProcessor {
    /**
     * Process the autofill [FillRequest] and invoke the [fillCallback] with the result.
     *
     * @param autofillAppInfo app data that is required for the autofill [request] processing.
     * @param fillCallback the callback to invoke when the [request] has been processed.
     * @param request the request data from the OS that contains data about the autofill hierarchy.
     */
    fun processFillRequest(
        autofillAppInfo: AutofillAppInfo,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
        request: FillRequest,
    )
}
