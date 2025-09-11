@file:Suppress("unused")

package androidx.credentials.providerevents.playservices

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsRequest

/**
 * A stub implementation of the Credential Provider Events IntentHandler class.
 */
object IntentHandler {

    /**
     * Stub implementation of the setImportCredentialsException method.
     */
    fun setImportCredentialsException(intent: Intent, exception: ImportCredentialsException) {
        // Stub implementation
    }

    /**
     * Stub implementation of the setImportCredentialsResponse method.
     */
    fun setImportCredentialsResponse(
        context: Activity,
        uri: Uri,
        response: ImportCredentialsResponse,
    ) {
        // Stub implementation
    }

    /**
     * Stub implementation of the retrieveImportCredentialsException method.
     */
    @Suppress("FunctionOnlyReturningConstant")
    fun retrieveProviderImportCredentialsRequest(
        intent: Intent,
    ): ProviderImportCredentialsRequest? {
        // Stub implementation
        return null
    }
}
