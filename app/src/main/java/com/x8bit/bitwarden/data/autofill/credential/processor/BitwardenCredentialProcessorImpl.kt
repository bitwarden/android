package com.x8bit.bitwarden.data.autofill.credential.processor

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnsupportedException
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.credential.UNLOCK_ACCOUNT_INTENT
import com.x8bit.bitwarden.data.autofill.credential.model.getCredentialResponseAction
import com.x8bit.bitwarden.data.autofill.fido2.processor.Fido2ProviderProcessor
import com.x8bit.bitwarden.data.autofill.password.processor.PasswordProviderProcessor
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * The default implementation of [BitwardenCredentialProcessor]. Its purpose is to handle FIDO2 or Password related
 * processing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class BitwardenCredentialProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val intentManager: IntentManager,
    private val fido2Processor: Fido2ProviderProcessor,
    private val passwordProcessor: PasswordProviderProcessor,
    dispatcherManager: DispatcherManager,
) : BitwardenCredentialProcessor {

    private val requestCode = AtomicInteger()

    private val scope = CoroutineScope(dispatcherManager.unconfined)

    override fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val userState = authRepository.userStateFlow.value
        if (userState == null) {
            callback.onError(CreateCredentialUnknownException("Active user is required."))
            return
        }

        val createCredentialJob = scope.launch {
            when (request) {
                is BeginCreatePublicKeyCredentialRequest -> {
                    fido2Processor.processCreateCredentialRequest(
                        requestCode = requestCode,
                        userState = userState,
                        request = request,
                    )
                }

                is BeginCreatePasswordCredentialRequest -> {
                    passwordProcessor.processCreateCredentialRequest(
                        requestCode = requestCode,
                        userState = userState,
                        request = request,
                    )
                }

                else -> null
            }?.let {
                callback.onResult(it)
            } ?: callback.onError(CreateCredentialUnknownException())
        }

        cancellationSignal.setOnCancelListener {
            if (createCredentialJob.isActive) {
                createCredentialJob.cancel()
            }
            callback.onError(CreateCredentialCancellationException())
        }
    }

    override fun processGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        // If the user is not logged in, return an error.
        val userState = authRepository.userStateFlow.value
        if (userState == null) {
            callback.onError(GetCredentialUnknownException("Active user is required."))
            return
        }

        // Return an unlock action if the current account is locked.
        if (!userState.activeAccount.isVaultUnlocked) {
            val authenticationAction = AuthenticationAction(
                title = context.getString(R.string.unlock),
                pendingIntent = intentManager.createCredentialUnlockPendingIntent(
                    action = UNLOCK_ACCOUNT_INTENT,
                    userId = userState.activeUserId,
                    requestCode = requestCode.getAndIncrement(),
                ),
            )

            callback.onResult(
                BeginGetCredentialResponse(
                    authenticationActions = listOf(authenticationAction),
                ),
            )
            return
        }

        val getCredentialJob = scope.launch {
            try {
                val fidoCredentials = fido2Processor.processGetCredentialRequest(
                    requestCode = requestCode,
                    activeUserId = userState.activeUserId,
                    beginGetCredentialOptions = request.beginGetCredentialOptions.filterIsInstance<BeginGetPublicKeyCredentialOption>(),
                ) ?: emptyList()
                val passwordCredentials = passwordProcessor.processGetCredentialRequest(
                    requestCode = requestCode,
                    activeUserId = userState.activeUserId,
                    callingAppInfo = request.callingAppInfo,
                    beginGetPasswordOptions = request.beginGetCredentialOptions.filterIsInstance<BeginGetPasswordOption>(),
                ) ?: emptyList()

                callback.onResult(
                    BeginGetCredentialResponse(
                        credentialEntries = fidoCredentials.plus(passwordCredentials),
                        // Explicitly clear any pending authentication actions since we only
                        // display results from the active account.
                        authenticationActions = emptyList(),
                        actions = listOf(getCredentialResponseAction(context)),
                    ),
                )
            } catch (e: GetCredentialException) {
                callback.onError(e)
            }
        }

        cancellationSignal.setOnCancelListener {
            callback.onError(GetCredentialCancellationException())
            getCredentialJob.cancel()
        }

    }

    override fun processClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // no-op: RFU
        callback.onError(ClearCredentialUnsupportedException())
    }
}
