package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.content.Context
import android.graphics.drawable.Icon
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
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_CREATE_PASSKEY"
const val GET_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY"
const val UNLOCK_ACCOUNT_INTENT = "com.x8bit.bitwarden.fido2.ACTION_UNLOCK_ACCOUNT"

/**
 * The default implementation of [Fido2ProviderProcessor]. Its purpose is to handle FIDO2 related
 * processing.
 */
@Suppress("LongParameterList")
@RequiresApi(Build.VERSION_CODES.S)
class Fido2ProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val intentManager: IntentManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : Fido2ProviderProcessor {

    private val requestCode = AtomicInteger()
    private val scope = CoroutineScope(dispatcherManager.unconfined)

    override fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val userId = authRepository.activeUserId
        if (userId == null) {
            callback.onError(CreateCredentialUnknownException("Active user is required."))
            return
        }

        val createCredentialJob = scope.launch {
            processCreateCredentialRequest(request = request)
                ?.let { callback.onResult(it) }
                ?: callback.onError(CreateCredentialUnknownException())
        }
        cancellationSignal.setOnCancelListener {
            if (createCredentialJob.isActive) {
                createCredentialJob.cancel()
            }
            callback.onError(CreateCredentialCancellationException())
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

    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse? {
        val requestJson = request
            .candidateQueryData
            .getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")

        if (requestJson.isNullOrEmpty()) return null

        val userState = authRepository.userStateFlow.value ?: return null

        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(userState.accounts.toCreateEntries(userState.activeUserId))
            .build()
    }

    private fun List<UserState.Account>.toCreateEntries(activeUserId: String) =
        map { it.toCreateEntry(isActive = activeUserId == it.userId) }

    private fun UserState.Account.toCreateEntry(isActive: Boolean): CreateEntry {
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
            // Set the last used time to "now" so the active account is the default option in the
            // system prompt.
            .setLastUsedTime(if (isActive) clock.instant() else null)
            .build()
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
                pendingIntent = intentManager.createFido2UnlockPendingIntent(
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

        // Otherwise, find all matching credentials from the current vault.
        val getCredentialJob = scope.launch {
            try {
                val credentialEntries = getMatchingFido2CredentialEntries(
                    userId = userState.activeUserId,
                    request = request,
                )

                callback.onResult(
                    BeginGetCredentialResponse(
                        credentialEntries = credentialEntries,
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

    @Throws(GetCredentialUnsupportedException::class)
    private suspend fun getMatchingFido2CredentialEntries(
        userId: String,
        request: BeginGetCredentialRequest,
    ): List<CredentialEntry> =
        request
            .beginGetCredentialOptions
            .flatMap { option ->
                if (option is BeginGetPublicKeyCredentialOption) {
                    val relyingPartyId = fido2CredentialManager
                        .getPasskeyAssertionOptionsOrNull(requestJson = option.requestJson)
                        ?.relyingPartyId
                        ?: throw GetCredentialUnknownException("Invalid data.")
                    buildCredentialEntries(userId, relyingPartyId, option)
                } else {
                    throw GetCredentialUnsupportedException("Unsupported option.")
                }
            }

    private suspend fun buildCredentialEntries(
        userId: String,
        relyingPartyId: String,
        option: BeginGetPublicKeyCredentialOption,
    ): List<CredentialEntry> {
        val cipherViews = vaultRepository
            .ciphersStateFlow
            .value
            .data
            ?.filter { it.isActiveWithFido2Credentials }
            ?: emptyList()
        val result = vaultRepository
            .getDecryptedFido2CredentialAutofillViews(cipherViews)
        return when (result) {
            DecryptFido2CredentialAutofillViewResult.Error -> {
                throw GetCredentialUnknownException("Error decrypting credentials.")
            }

            is DecryptFido2CredentialAutofillViewResult.Success -> {
                result
                    .fido2CredentialAutofillViews
                    .filter { it.rpId == relyingPartyId }
                    .toCredentialEntries(
                        userId = userId,
                        option = option,
                    )
            }
        }
    }

    private fun List<Fido2CredentialAutofillView>.toCredentialEntries(
        userId: String,
        option: BeginGetPublicKeyCredentialOption,
    ): List<CredentialEntry> =
        this
            .map {
                PublicKeyCredentialEntry
                    .Builder(
                        context = context,
                        username = it.userNameForUi ?: context.getString(R.string.no_username),
                        pendingIntent = intentManager
                            .createFido2GetCredentialPendingIntent(
                                action = GET_PASSKEY_INTENT,
                                userId = userId,
                                credentialId = it.credentialId.toString(),
                                cipherId = it.cipherId,
                                requestCode = requestCode.getAndIncrement(),
                            ),
                        beginGetPublicKeyCredentialOption = option,
                    )
                    .setIcon(
                        Icon
                            .createWithResource(
                                context,
                                R.drawable.ic_bw_passkey,
                            ),
                    )
                    .build()
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
