package com.bitwarden.cxf.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A request to import the provider's credentials.
 *
 * @property uri the FileProvider uri that the importer will read the response from.
 * @property requestJson the request to import the provider's credentials.
 */
@Parcelize
data class ImportCredentialsRequestData(
    val uri: Uri,
    val requestJson: String,
) : Parcelable
