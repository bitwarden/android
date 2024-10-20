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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_CREATE_PASSWORD"
const val GET_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_GET_PASSWORD"
const val UNLOCK_ACCOUNT_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_UNLOCK_ACCOUNT"

/**
 * The default implementation of [PasswordProviderProcessor]. Its purpose is to handle Password related
 * processing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class PasswordProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val autofillCipherProvider: AutofillCipherProvider,
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
                handleCreatePassword()
            }

            else                                    -> null
        }
    }

    private fun handleCreatePassword(): BeginCreateCredentialResponse? {
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
    private suspend fun getMatchingPasswordCredentialEntries(
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
                            matchUri = request.callingAppInfo?.origin
                                ?: request.callingAppInfo?.packageName
                                    ?.toAndroidAppUriString(),
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

    private suspend fun buildCredentialEntries(
        userId: String,
        matchUri: String?,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> {
        return autofillCipherProvider.getLoginAutofillCiphers(
            uri = matchUri ?: return emptyList(),
        ).toCredentialEntries(
            userId = userId,
            option = option,
        )
    }

    private fun List<AutofillCipher.Login>.toCredentialEntries(
        userId: String,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> =
        this
            .mapNotNull {
                PasswordCredentialEntry
                    .Builder(
                        context = context,
                        username = it.username,
                        pendingIntent = intentManager
                            .createPasswordGetCredentialPendingIntent(
                                action = GET_PASSWORD_INTENT,
                                id = option.id,
                                userId = userId,
                                cipherId = it.cipherId ?: return@mapNotNull null,
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
