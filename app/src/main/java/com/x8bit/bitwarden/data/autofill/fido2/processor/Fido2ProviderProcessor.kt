package com.x8bit.bitwarden.data.autofill.fido2.processor

import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CredentialEntry
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import java.util.concurrent.atomic.AtomicInteger

/**
 * A class to handle FIDO2 credential request processing. This includes save and autofill requests.
 */
interface Fido2ProviderProcessor {

    /**
     * Process the [BeginCreateCredentialRequest] and invoke the [callback] with the result.
     *
     * @param request The request data from the OS that contains data about the requesting provider.
     * @param cancellationSignal signal for observing cancellation requests. The system will use
     * this to notify us that the result is no longer needed and we should stop handling it in order
     * to save our resources.
     * @param callback the callback object to be used to notify the response or error
     */
    suspend fun processCreateCredentialRequest(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse?

    /**
     * Process the [BeginGetCredentialRequest] and invoke the [callback] with the result.
     *
     * @param beginGetCredentialOptions data from the OS that contains data about the requesting provider.
     * @param cancellationSignal signal for observing cancellation requests. The system will use
     * this to notify us that the result is no longer needed and we should stop handling it in order
     * to save our resources.
     * @param callback the callback object to be used to notify the response or error
     */
    suspend fun processGetCredentialRequest(
        requestCode: AtomicInteger,
        activeUserId: String,
        beginGetCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
    ): List<CredentialEntry>?

}
