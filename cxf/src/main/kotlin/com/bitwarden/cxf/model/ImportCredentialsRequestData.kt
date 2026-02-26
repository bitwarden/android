package com.bitwarden.cxf.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A request to import the provider's credentials.
 *
 * @property uri the FileProvider uri that the importer will read the response from.
 * @property credentialTypes the credential types that the requester supports.
 * @property knownExtensions the known extensions that the importer supports.
 */
@Parcelize
data class ImportCredentialsRequestData(
    val uri: Uri,
    val credentialTypes: Set<String>,
    val knownExtensions: Set<String>,
) : Parcelable
