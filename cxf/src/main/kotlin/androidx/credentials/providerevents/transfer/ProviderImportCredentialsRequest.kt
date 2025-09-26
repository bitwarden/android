package androidx.credentials.providerevents.transfer

import android.net.Uri
import androidx.credentials.provider.CallingAppInfo

/**
 * Placeholder class for the request received by the provider after the query phase of the import
 * flow is complete i.e. the user was presented with a list of entries, and the user has now made
 * a selection from the list of [ExportEntry] presented on the selector UI.
 */
data class ProviderImportCredentialsRequest(
    val request: ImportCredentialsRequest,
    val callingAppInfo: CallingAppInfo,
    val uri: Uri,
    val credId: String,
)
