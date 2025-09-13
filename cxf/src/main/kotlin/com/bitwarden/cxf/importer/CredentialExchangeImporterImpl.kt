package com.bitwarden.cxf.importer

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult

/**
 * Default implementation of [CredentialExchangeImporter].
 *
 * @param activityContext The context of the activity that is importing credentials.
 * @param providerEventsManager The [ProviderEventsManager] instance used for managing provider
 * events. If not provided, a default instance will be created using the provided [activityContext].
 * It is only meant to be used for testing purposes.
 */
internal class CredentialExchangeImporterImpl(
    private val activityContext: Context,
    @param:VisibleForTesting
    private val providerEventsManager: ProviderEventsManager =
        ProviderEventsManager.create(activityContext),
) : CredentialExchangeImporter {

    override suspend fun importCredentials(
        credentialTypes: List<String>,
    ): ImportCredentialsSelectionResult {
        return try {
            val response = providerEventsManager.importCredentials(
                context = activityContext,
                request = ImportCredentialsRequest(
                    // TODO: Link to the correct documentation once it's available.
                    requestJson = """
                    {
                      "importer": "${activityContext.packageName}",
                      "credentialTypes": [
                        ${credentialTypes.joinToString { "\"$it\"" }}
                      ]
                    }
                    """
                        .trimIndent(),
                ),
            )
            ImportCredentialsSelectionResult.Success(
                response = response.response.responseJson,
                callingAppInfo = response.callingAppInfo,
            )
        } catch (_: ImportCredentialsCancellationException) {
            ImportCredentialsSelectionResult.Cancelled
        } catch (e: ImportCredentialsException) {
            ImportCredentialsSelectionResult.Failure(error = e)
        }
    }
}
