package com.x8bit.bitwarden.data.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import dagger.hilt.android.AndroidEntryPoint

/**
 * The [AutofillService] implementation for the app. This fulfills autofill requests from other
 * applications.
 */
@AndroidEntryPoint
class BitwardenAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
    ) {
        // TODO: parse request and perform dummy autofill (BIT-1314)
    }

    override fun onSaveRequest(
        saverRequest: SaveRequest,
        saveCallback: SaveCallback,
    ) {
        // TODO: add save request behavior (BIT-1299)
    }
}
