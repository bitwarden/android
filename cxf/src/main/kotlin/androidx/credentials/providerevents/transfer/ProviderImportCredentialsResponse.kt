package androidx.credentials.providerevents.transfer

import androidx.credentials.provider.CallingAppInfo

/**
 * Placeholder class representing the response to an import request.
 */
data class ProviderImportCredentialsResponse(
    val response: ImportCredentialsResponse,
    val callingAppInfo: CallingAppInfo,
)
