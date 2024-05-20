package com.x8bit.bitwarden.data.autofill.fido2.processor

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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_CREATE_PASSKEY"

/**
 * The default implementation of [Fido2ProviderProcessor]. Its purpose is to handle FIDO2 related
 * processing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class Fido2ProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val intentManager: IntentManager,
    dispatcherManager: DispatcherManager,
) : Fido2ProviderProcessor {

    private val requestCode = AtomicInteger()
    private val scope = CoroutineScope(dispatcherManager.unconfined)

    override fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        cancellationSignal.setOnCancelListener {
            callback.onError(CreateCredentialCancellationException())
            scope.cancel()
        }

        val userId = authRepository.activeUserId
        if (userId == null) {
            callback.onError(CreateCredentialUnknownException("Active user is required."))
            return
        }

        scope.launch {
            processCreateCredentialRequest(request = request)
                ?.let { callback.onResult(it) }
                ?: callback.onError(CreateCredentialUnknownException())
        }
    }

    private fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
    ): BeginCreateCredentialResponse? {
        return when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                handleCreatePasskeyQuery(request)
            }

            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse? {
        val requestJson = request
            .candidateQueryData
            .getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")

        if (requestJson.isNullOrEmpty()) return null

        val userState = authRepository.userStateFlow.value ?: return null

        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(userState.accounts.toCreateEntries())
            .build()
    }

    private fun List<UserState.Account>.toCreateEntries() = map { it.toCreateEntry() }

    private fun UserState.Account.toCreateEntry(): CreateEntry {
        val accountName = name ?: email
        return CreateEntry
            .Builder(
                accountName = accountName,
                pendingIntent = intentManager.createFido2CreationPendingIntent(
                    CREATE_PASSKEY_INTENT,
                    userId,
                    requestCode.getAndIncrement(),
                ),
            )
            .setDescription(
                context.getString(
                    R.string.your_passkey_will_be_saved_to_your_bitwarden_vault_for_x,
                    accountName,
                ),
            )
            .build()
    }

    override fun processGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        // no-op: RFU
        callback.onError(GetCredentialUnsupportedException())
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
