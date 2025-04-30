package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.credentials.provider.BiometricPromptData
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.data.manager.DispatcherManager
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher

private const val CREATE_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_CREATE_PASSKEY"
const val GET_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY"
const val UNLOCK_ACCOUNT_INTENT = "com.x8bit.bitwarden.fido2.ACTION_UNLOCK_ACCOUNT"

/**
 * The default implementation of [Fido2ProviderProcessor]. Its purpose is to handle FIDO2 related
 * processing.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@RequiresApi(Build.VERSION_CODES.S)
class Fido2ProviderProcessorImpl(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val intentManager: IntentManager,
    private val clock: Clock,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val featureFlagManager: FeatureFlagManager,
    dispatcherManager: DispatcherManager,
) : Fido2ProviderProcessor {

    private val requestCode = AtomicInteger()
    private val ioScope = CoroutineScope(dispatcherManager.io)

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

        val createCredentialJob = ioScope.launch {
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
        val getCredentialJob = ioScope.launch {
            getMatchingFido2CredentialEntries(
                userId = userState.activeUserId,
                request = request,
            )
                .onSuccess {
                    callback.onResult(
                        BeginGetCredentialResponse(credentialEntries = it),
                    )
                }
                .onFailure { callback.onError(GetCredentialUnknownException(it.message)) }
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
        val entryBuilder = CreateEntry
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
            .setAutoSelectAllowed(true)

        if (isVaultUnlocked &&
            featureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyCreation)
        ) {
            biometricsEncryptionManager
                .getOrCreateCipher(userId)
                ?.let { entryBuilder.setBiometricPromptDataIfSupported(cipher = it) }
        }
        return entryBuilder.build()
    }

    private suspend fun getMatchingFido2CredentialEntries(
        userId: String,
        request: BeginGetCredentialRequest,
    ): Result<List<CredentialEntry>> =
        request
            .beginGetCredentialOptions
            .firstNotNullOfOrNull { it as? BeginGetPublicKeyCredentialOption }
            ?.let {
                fido2CredentialManager
                    .getPublicKeyCredentialEntries(
                        userId = userId,
                        option = it,
                    )
            }
            ?: GetCredentialUnsupportedException("Unsupported option.").asFailure()

    private fun CreateEntry.Builder.setBiometricPromptDataIfSupported(
        cipher: Cipher,
    ): CreateEntry.Builder {
        return if (isBuildVersionBelow(Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
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
