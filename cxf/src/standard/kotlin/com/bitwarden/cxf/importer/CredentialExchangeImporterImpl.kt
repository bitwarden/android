package com.bitwarden.cxf.importer

import android.content.Context
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.KnownExtensions
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import timber.log.Timber

/**
 * Default implementation of [CredentialExchangeImporter].
 *
 * @param activity The context of the activity that is importing credentials.
 * @param providerEventsManager The [ProviderEventsManager] instance used for managing provider
 * events. If not provided, a default instance will be created using the provided [activity].
 * It is only meant to be used for testing purposes.
 */
internal class CredentialExchangeImporterImpl(
    private val activity: Context,
    private val providerEventsManager: ProviderEventsManager =
        ProviderEventsManager.create(activity),
) : CredentialExchangeImporter {

    override suspend fun importCredentials(
        credentialTypes: List<String>,
    ): ImportCredentialsSelectionResult = try {
        val response = providerEventsManager.importCredentials(
            context = activity,
            request = ImportCredentialsRequest(
                credentialTypes = credentialTypes.toSet(),
                knownExtensions = setOf(KnownExtensions.KNOWN_EXTENSION_SHARED),
            ),
        )
        ImportCredentialsSelectionResult.Success(
            response = response.response.responseJson,
            callingAppInfo = response.callingAppInfo,
        )
    } catch (e: ImportCredentialsCancellationException) {
        Timber.e(e, "User cancelled import from selected credential manager.")
        ImportCredentialsSelectionResult.Cancelled
    } catch (e: ImportCredentialsException) {
        Timber.e(e, "Failed to import items from selected credential manager.")
        ImportCredentialsSelectionResult.Failure(error = e)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Timber.e(e, "Failed to import items from selected credential manager.")
        ImportCredentialsSelectionResult.Failure(
            error = ImportCredentialsUnknownErrorException(),
        )
    }
}
