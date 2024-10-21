package com.x8bit.bitwarden.data.autofill.password.processor

import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import java.util.concurrent.atomic.AtomicInteger

/**
 * A class to handle Password credential request processing. This includes save and autofill requests.
 */
interface PasswordProviderProcessor {

    /**
     * Process the [BeginCreateCredentialRequest] and returns the result.
     *
     * @param requestCode The requestCode to be used for pending intents.
     * @param userState The userState of the currently active user.
     * @param request The request data from the OS that contains data about the requesting provider.
     */
    suspend fun processCreateCredentialRequest(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePasswordCredentialRequest,
    ): BeginCreateCredentialResponse

    /**
     * Process the [BeginGetCredentialRequest] and returns the result.
     *
     * @param requestCode The requestCode to be used for pending intents.
     * @param activeUserId The id of the currently active user.
     * @param callingAppInfo The info of the callingAppInfo because it's not present in [BeginGetPasswordOption].
     * @param beginGetPasswordOptions The request data from the OS that contains data about the requesting provider.
     */
    suspend fun processGetCredentialRequest(
        requestCode: AtomicInteger,
        activeUserId: String,
        callingAppInfo: CallingAppInfo?,
        beginGetPasswordOptions: List<BeginGetPasswordOption>,
    ): List<CredentialEntry>?

}
