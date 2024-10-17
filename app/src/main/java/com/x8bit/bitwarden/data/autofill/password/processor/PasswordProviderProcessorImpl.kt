package com.x8bit.bitwarden.data.autofill.password.processor

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
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
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.bitwarden.core.Uuid
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.fido2.processor.UNLOCK_ACCOUNT_INTENT
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_CREATE_PASSWORD"
const val GET_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_GET_PASSWORD"

/**
 * The default implementation of [PasswordProviderProcessor]. Its purpose is to handle Password related
 * processing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class PasswordProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val intentManager: IntentManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : PasswordProviderProcessor {

    /**
     * The coroutine scope for launching asynchronous operations.
     */
    private val scope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    private val requestCode = AtomicInteger()

    override fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
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
            is BeginCreatePasswordCredentialRequest -> {
                handleCreatePassword(request)
            }

            else                                    -> null
        }
    }

    private fun handleCreatePassword(
        request: BeginCreatePasswordCredentialRequest,
    ): BeginCreateCredentialResponse? {
        println(request)
        println(
            request
                .candidateQueryData
        )
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
                pendingIntent = intentManager.createPasswordCreationPendingIntent(
                    CREATE_PASSWORD_INTENT,
                    userId,
                    requestCode.getAndIncrement(),
                ),
            )
            .setDescription(
                context.getString(
                    R.string.your_passkey_will_be_saved_to_your_bitwarden_vault_for_x, //TODO change text to your password will be saved
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
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
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
                pendingIntent = intentManager.createPasswordUnlockPendingIntent(
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
                val credentialEntries = getMatchingPasswordCredentialEntries(
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
    private fun getMatchingPasswordCredentialEntries(
        userId: String,
        request: BeginGetCredentialRequest,
    ): List<CredentialEntry> =
        request
            .beginGetCredentialOptions
            .flatMap { option ->
                if (option is BeginGetPasswordOption) {
                    if (option.allowedUserIds.isEmpty() || option.allowedUserIds.contains(userId)) {
                        buildCredentialEntries(
                            userId = userId,
                            callingPackage = request.callingAppInfo?.packageName,
                            option = option,
                        )
                    } else {
                        //userid did not match any in allowedUserIds
                        emptySet()
                    }
                } else {
                    throw GetCredentialUnsupportedException("Unsupported option.")
                }
            }

    private fun buildCredentialEntries(
        userId: String,
        callingPackage: String?,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> {
        //TODO get data and map correctly
        return listOf(
            Fido2CredentialAutofillView(
                credentialId = ByteArray(0),
                cipherId = Uuid(),
                rpId = "",
                userNameForUi = "userNameForUi",
                userHandle = ByteArray(0)
            )
        ).toCredentialEntries(
            userId = userId,
            option = option,
        )
    }


    private fun List<Fido2CredentialAutofillView>.toCredentialEntries(
        userId: String,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> =
        this
            .map {
                PasswordCredentialEntry
                    .Builder(
                        context = context,
                        username = it.userNameForUi ?: context.getString(R.string.no_username),
                        pendingIntent = intentManager
                            .createPasswordGetCredentialPendingIntent(
                                action = GET_PASSWORD_INTENT,
                                userId = userId,
                                credentialId = "",
                                cipherId = it.cipherId,
                                requestCode = requestCode.getAndIncrement(),
                            ),
                        beginGetPasswordOption = option,
                    )
                    .build()
            }

    override fun processClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        //TODO("Not yet implemented")
    }

}
