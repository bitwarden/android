package com.bitwarden.cxf.importer

import android.content.Context
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult

/**
 * F-Droid implementation of [CredentialExchangeImporter]. Credential exchange relies on the
 * Play Services backend, which is not available on F-Droid builds, so import is unsupported and
 * always reports a failure if invoked.
 */
@OmitFromCoverage
@Suppress("UnusedParameter")
internal class CredentialExchangeImporterImpl(
    activity: Context,
) : CredentialExchangeImporter {

    override fun isSupported(): Boolean = false

    override suspend fun importCredentials(
        credentialTypes: List<String>,
    ): ImportCredentialsSelectionResult = ImportCredentialsSelectionResult.Failure(
        error = ImportCredentialsUnknownErrorException(),
    )
}
