package com.bitwarden.cxf.model

import android.net.Uri
import androidx.credentials.provider.CallingAppInfo

/**
 * A request to import the provider's credentials.
 *
 * @property uri the FileProvider uri that the importer will read the response from.
 * @property requestJson the request to import the provider's credentials.
 * @property callingAppInfo the caller's app info.
 */
data class BitwardenImportCredentialsRequest(
    val uri: Uri,
    val requestJson: String,
    val callingAppInfo: CallingAppInfo,
)
