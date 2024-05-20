package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.ProviderClearCredentialStateRequest

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
    fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    )

    /**
     * Process the [BeginGetCredentialRequest] and invoke the [callback] with the result.
     *
     * @param request The request data form the OS that contains data about the requesting provider.
     * @param cancellationSignal signal for observing cancellation requests. The system will use
     * this to notify us that the result is no longer needed and we should stop handling it in order
     * to save our resources.
     * @param callback the callback object to be used to notify the response or error
     */
    fun processGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    )

    /**
     * Process the [ProviderClearCredentialStateRequest] and invoke the [callback] with the result.
     *
     * @param request The request data form the OS that contains data about the requesting provider.
     * @param cancellationSignal signal for observing cancellation requests. The system will use
     * this to notify us that the result is no longer needed and we should stop handling it in order
     * to save our resources.
     * @param callback the callback object to be used to notify the response or error
     */
    fun processClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    )
}
