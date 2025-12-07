package com.x8bit.bitwarden.data.credentials.processor

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.credentials.CredentialManager
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
import androidx.credentials.provider.BiometricPromptData
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import javax.crypto.Cipher

/**
 * The default implementation of [CredentialProviderProcessor]. Its purpose is to handle
 * [CredentialManager] requests from other applications.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@RequiresApi(Build.VERSION_CODES.S)
class CredentialProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val bitwardenCredentialManager: BitwardenCredentialManager,
    private val pendingIntentManager: CredentialManagerPendingIntentManager,
    private val clock: Clock,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    dispatcherManager: DispatcherManager,
) : CredentialProviderProcessor {

    private val ioScope = CoroutineScope(dispatcherManager.io)

    override fun processCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        Timber.d("Create credential request received.")
        val userId = authRepository.activeUserId
        if (userId == null) {
            Timber.w("No active user. Cannot create credential.")
            callback.onError(CreateCredentialUnknownException("Active user is required."))
            return
        }

        val createCredentialJob = ioScope.launch {
            (handleCreatePasskeyQuery(request) ?: handleCreatePasswordQuery(request))
                ?.let { callback.onResult(it) }
                ?: run {
                    Timber.w("Unknown create credential request.")
                    callback.onError(CreateCredentialUnknownException())
                }
        }
        cancellationSignal.setOnCancelListener {
            if (createCredentialJob.isActive) {
                createCredentialJob.cancel()
            }
            Timber.d("Create credential request cancelled by system.")
            callback.onError(CreateCredentialCancellationException())
        }
    }

    override fun processGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        Timber.d("Get credential request received.")
        // If the user is not logged in, return an error.
        val userState = authRepository.userStateFlow.value
        if (userState == null) {
            Timber.w("No active user. Cannot get credentials.")
            callback.onError(GetCredentialUnknownException("Active user is required."))
            return
        }

        // Return an unlock action if the current account is locked.
        if (!userState.activeAccount.isVaultUnlocked) {
            Timber.d("Vault is locked. Requesting unlock.")
            val authenticationAction = AuthenticationAction(
                title = context.getString(BitwardenString.unlock),
                pendingIntent = pendingIntentManager.createFido2UnlockPendingIntent(
                    userId = userState.activeUserId,
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
        val getCredentialJob = ioScope.launch {
            bitwardenCredentialManager
                .getCredentialEntries(
                    getCredentialsRequest = GetCredentialsRequest(
                        userId = userState.activeUserId,
                        BeginGetCredentialRequest.asBundle(request),
                    ),
                )
                .onSuccess {
                    Timber.d("Credentials retrieved.")
                    callback.onResult(BeginGetCredentialResponse(credentialEntries = it))
                }
                .onFailure {
                    Timber.w("Error getting credentials.")
                    callback.onError(GetCredentialUnknownException(it.message))
                }
        }
        cancellationSignal.setOnCancelListener {
            Timber.d("Get credential request cancelled by system.")
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
        Timber.w("Unsupported clear credential state request received.")
        callback.onError(ClearCredentialUnsupportedException())
    }

    private fun handleCreatePasskeyQuery(
        request: BeginCreateCredentialRequest,
    ): BeginCreateCredentialResponse? {
        if (request !is BeginCreatePublicKeyCredentialRequest) return null

        val requestJson = request
            .candidateQueryData
            .getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")

        if (requestJson.isNullOrEmpty()) return null

        val userState = authRepository.userStateFlow.value ?: return null

        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(
                userState.accounts.toCreatePasskeyEntry(userState.activeUserId),
            )
            .build()
    }

    private fun List<UserState.Account>.toCreatePasskeyEntry(
        activeUserId: String,
    ): List<CreateEntry> = map { it.toCreatePasskeyEntry(isActive = activeUserId == it.userId) }

    private fun UserState.Account.toCreatePasskeyEntry(
        isActive: Boolean,
    ): CreateEntry {
        val accountName = name ?: email
        val entryBuilder = CreateEntry
            .Builder(
                accountName = accountName,
                pendingIntent = pendingIntentManager.createFido2CreationPendingIntent(
                    userId = userId,
                ),
            )
            .setDescription(
                context.getString(
                    BitwardenString.your_passkey_will_be_saved_to_your_bitwarden_vault_for_x,
                    accountName,
                ),
            )
            // Set the last used time to "now" so the active account is the default option in the
            // system prompt.
            .setLastUsedTime(if (isActive) clock.instant() else null)
            .setAutoSelectAllowed(true)

        if (isVaultUnlocked) {
            biometricsEncryptionManager
                .getOrCreateCipher(userId)
                ?.let { entryBuilder.setBiometricPromptDataIfSupported(cipher = it) }
        }
        return entryBuilder.build()
    }

    private fun handleCreatePasswordQuery(
        request: BeginCreateCredentialRequest,
    ): BeginCreateCredentialResponse? {
        if (request !is BeginCreatePasswordCredentialRequest) return null

        val userState = authRepository.userStateFlow.value ?: return null

        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(
                userState.accounts.toCreatePasswordEntry(userState.activeUserId),
            )
            .build()
    }

    private fun List<UserState.Account>.toCreatePasswordEntry(
        activeUserId: String,
    ) = map { it.toCreatePasswordEntry(isActive = activeUserId == it.userId) }

    private fun UserState.Account.toCreatePasswordEntry(
        isActive: Boolean,
    ): CreateEntry {
        val accountName = name ?: email
        val entryBuilder = CreateEntry
            .Builder(
                accountName = accountName,
                pendingIntent = pendingIntentManager.createPasswordCreationPendingIntent(
                    userId = userId,
                ),
            )
            .setDescription(
                context.getString(
                    BitwardenString.your_password_will_be_saved_to_your_bitwarden_vault_for_x,
                    accountName,
                ),
            )
            // Set the last used time to "now" so the active account is the default option in the
            // system prompt.
            .setLastUsedTime(if (isActive) clock.instant() else null)
            .setAutoSelectAllowed(true)

        if (isVaultUnlocked) {
            biometricsEncryptionManager
                .getOrCreateCipher(userId)
                ?.let { entryBuilder.setBiometricPromptDataIfSupported(cipher = it) }
        }
        return entryBuilder.build()
    }

    private fun CreateEntry.Builder.setBiometricPromptDataIfSupported(
        cipher: Cipher,
    ): CreateEntry.Builder {
        return if (!isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            this
        } else {
            setBiometricPromptData(
                biometricPromptData = buildPromptDataWithCipher(cipher),
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun buildPromptDataWithCipher(
        cipher: Cipher,
    ): BiometricPromptData = BiometricPromptData.Builder()
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setCryptoObject(BiometricPrompt.CryptoObject(cipher))
        .build()
}
