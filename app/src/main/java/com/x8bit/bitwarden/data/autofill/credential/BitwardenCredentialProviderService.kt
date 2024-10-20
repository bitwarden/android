package com.x8bit.bitwarden.data.autofill.credential

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.x8bit.bitwarden.data.autofill.credential.processor.BitwardenCredentialProcessor
import com.x8bit.bitwarden.data.autofill.credential.processor.BitwardenCredentialProcessorImpl
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val UNLOCK_ACCOUNT_INTENT = "com.x8bit.bitwarden.fido2.ACTION_UNLOCK_ACCOUNT"

/**
 * The [CredentialProviderService] for the app. This fulfills FIDO2 credential requests from other
 * applications.
 */
@OmitFromCoverage
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Keep
@AndroidEntryPoint
class BitwardenCredentialProviderService : CredentialProviderService() {

    /**
     * A processor to handle the credential fulfillment. We keep the service light because it
     * isn't easily testable.
     */
    @Inject
    lateinit var processor: BitwardenCredentialProcessor

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        processor.processCreateCredentialRequest(
            request,
            cancellationSignal,
            callback,
        )
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        processor.processGetCredentialRequest(
            request,
            cancellationSignal,
            callback,
        )
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        processor.processClearCredentialStateRequest(
            request,
            cancellationSignal,
            callback,
        )
    }
}
