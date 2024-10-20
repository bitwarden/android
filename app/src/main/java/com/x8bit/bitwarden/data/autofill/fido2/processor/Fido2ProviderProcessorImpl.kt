package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_CREATE_PASSKEY"
const val GET_PASSKEY_INTENT = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY"

/**
 * The default implementation of [Fido2ProviderProcessor]. Its purpose is to handle FIDO2 related
 * processing.
 */
@Suppress("LongParameterList")
@RequiresApi(Build.VERSION_CODES.S)
class Fido2ProviderProcessorImpl(
    private val context: Context,
    private val vaultRepository: VaultRepository,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val intentManager: IntentManager,
    private val clock: Clock,
) : Fido2ProviderProcessor {

    override suspend fun processCreateCredentialRequest(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse? {
        return handleCreatePasskeyQuery(
            requestCode = requestCode,
            userState = userState,
            request = request,
        )
    }

    private fun handleCreatePasskeyQuery(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse? {
        val requestJson = request
            .candidateQueryData
            .getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")

        if (requestJson.isNullOrEmpty()) return null

        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(
                userState.accounts.toCreateEntries(
                    activeUserId = userState.activeUserId,
                    requestCode = requestCode,
                )
            )
            .build()
    }

    private fun List<UserState.Account>.toCreateEntries(
        activeUserId: String,
        requestCode: AtomicInteger,
    ) = map {
        it.toCreateEntry(
            isActive = activeUserId == it.userId,
            requestCode = requestCode,
        )
    }

    private fun UserState.Account.toCreateEntry(
        isActive: Boolean,
        requestCode: AtomicInteger,
    ): CreateEntry {
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

    override suspend fun processGetCredentialRequest(
        requestCode: AtomicInteger,
        activeUserId: String,
        beginGetCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
    ): List<CredentialEntry> {
        return getMatchingFido2CredentialEntries(
            requestCode = requestCode,
            userId = activeUserId,
            beginGetCredentialOptions = beginGetCredentialOptions,
        )
    }

    @Throws(GetCredentialUnsupportedException::class)
    private suspend fun getMatchingFido2CredentialEntries(
        requestCode: AtomicInteger,
        userId: String,
        beginGetCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
    ): List<CredentialEntry> =
        beginGetCredentialOptions
            .flatMap { option ->
                val relyingPartyId = fido2CredentialManager
                    .getPasskeyAssertionOptionsOrNull(requestJson = option.requestJson)
                    ?.relyingPartyId
                    ?: throw GetCredentialUnknownException("Invalid data.")
                buildCredentialEntries(requestCode, userId, relyingPartyId, option)
            }

    private suspend fun buildCredentialEntries(
        requestCode: AtomicInteger,
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
                        requestCode = requestCode,
                        userId = userId,
                        option = option,
                    )
            }
        }
    }

    private fun List<Fido2CredentialAutofillView>.toCredentialEntries(
        requestCode: AtomicInteger,
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
                    .build()
            }

}
