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
     * Process the [BeginCreateCredentialRequest] and returns the result.
     *
     * @param requestCode The requestCode to be used for pending intents.
     * @param userState The userState of the currently active user.
     * @param request The request data from the OS that contains data about the requesting provider.
     */
    suspend fun processCreateCredentialRequest(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse?

    /**
     * Process the [BeginGetCredentialRequest] and returns the result.
     *
     * @param requestCode The requestCode to be used for pending intents.
     * @param activeUserId The id of the currently active user.
     * @param beginGetCredentialOptions The request data from the OS that contains data about the requesting provider.
     */
    suspend fun processGetCredentialRequest(
        requestCode: AtomicInteger,
        activeUserId: String,
        beginGetCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
    ): List<CredentialEntry>?

}
