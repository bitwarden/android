@file:OmitFromCoverage

package com.bitwarden.cxf.util

import android.content.Intent
import androidx.credentials.providerevents.playservices.IntentHandler
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.model.BitwardenImportCredentialsRequest

/**
 * Retrieves the [BitwardenImportCredentialsRequest] from the intent.
 */
fun Intent.getProviderImportCredentialsRequest(): BitwardenImportCredentialsRequest? = IntentHandler
    .retrieveProviderImportCredentialsRequest(this)
    ?.let {
        BitwardenImportCredentialsRequest(
            uri = it.uri,
            requestJson = it.request.requestJson,
            callingAppInfo = it.callingAppInfo,
        )
    }
