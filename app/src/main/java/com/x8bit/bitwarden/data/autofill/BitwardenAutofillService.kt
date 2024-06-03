package com.x8bit.bitwarden.data.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.annotation.Keep
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.processor.AutofillProcessor
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The [AutofillService] implementation for the app. This fulfills autofill requests from other
 * applications.
 */
@OmitFromCoverage
@Keep
@AndroidEntryPoint
class BitwardenAutofillService : AutofillService() {

    /**
     * A processor to handle the autofill fulfillment. We want to keep this service light because
     * it isn't easily tested.
     */
    @Inject
    lateinit var processor: AutofillProcessor

    /**
     * App information for the autofill feature.
     */
    private val autofillAppInfo: AutofillAppInfo
        get() = AutofillAppInfo(
            context = applicationContext,
            packageName = packageName,
            sdkInt = Build.VERSION.SDK_INT,
        )

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback,
    ) {
        processor.processFillRequest(
            autofillAppInfo = autofillAppInfo,
            cancellationSignal = cancellationSignal,
            fillCallback = fillCallback,
            request = request,
        )
    }

    override fun onSaveRequest(
        saverRequest: SaveRequest,
        saveCallback: SaveCallback,
    ) {
        processor.processSaveRequest(
            autofillAppInfo = autofillAppInfo,
            request = saverRequest,
            saveCallback = saveCallback,
        )
    }
}
