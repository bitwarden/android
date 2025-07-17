package com.x8bit.bitwarden.data.autofill.provider

import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.util.firstWithTimeoutOrNull
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import timber.log.Timber

/**
 * The duration, in milliseconds, we should wait while waiting for the vault status to not be
 * 'UNLOCKING' before proceeding.
 */
private const val VAULT_LOCKED_TIMEOUT_MS: Long = 500L

/**
 * The duration, in milliseconds, we should wait while retrieving ciphers before proceeding.
 */
private const val GET_CIPHERS_TIMEOUT_MS: Long = 2_000L

/**
 * The default [AutofillCipherProvider] implementation. This service is used for getting current
 * [AutofillCipher]s.
 */
class AutofillCipherProviderImpl(
    private val authRepository: AuthRepository,
    private val cipherMatchingManager: CipherMatchingManager,
    private val vaultRepository: VaultRepository,
) : AutofillCipherProvider {
    private val activeUserId: String? get() = authRepository.activeUserId

    override suspend fun isVaultLocked(): Boolean {
        val userId = activeUserId ?: return true

        // Wait for any unlocking actions to finish. This can be relevant on startup for Never lock
        // accounts.
        vaultRepository
            .vaultUnlockDataStateFlow
            .firstWithTimeoutOrNull(timeMillis = VAULT_LOCKED_TIMEOUT_MS) {
                it.statusFor(userId = userId) != VaultUnlockData.Status.UNLOCKING
            }

        return !vaultRepository.isVaultUnlocked(userId = userId)
    }

    override suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card> {
        val cipherListViews = getUnlockedCipherListViewsOrNull() ?: return emptyList()

        return cipherListViews
            .mapNotNull { cipherListView ->
                cipherListView
                    // We only care about non-deleted card ciphers.
                    .takeIf {
                        // Must be card type.
                        it.type is CipherListViewType.Card &&
                            // Must not be deleted.
                            it.deletedDate == null &&
                            // Must not require a reprompt.
                            it.reprompt == CipherRepromptType.NONE
                    }
                    ?.let { nonNullCipherListView ->
                        nonNullCipherListView.id?.let { cipherId ->
                            decryptCipherOrNull(cipherId = cipherId)?.let { cipherView ->
                                AutofillCipher.Card(
                                    cipherId = cipherView.id,
                                    name = cipherView.name,
                                    subtitle = cipherView.subtitle.orEmpty(),
                                    cardholderName = cipherView.card?.cardholderName.orEmpty(),
                                    code = cipherView.card?.code.orEmpty(),
                                    expirationMonth = cipherView.card?.expMonth.orEmpty(),
                                    expirationYear = cipherView.card?.expYear.orEmpty(),
                                    number = cipherView.card?.number.orEmpty(),
                                )
                            }
                        }
                    }
            }
    }

    override suspend fun getLoginAutofillCiphers(
        uri: String,
    ): List<AutofillCipher.Login> {
        val cipherViews = getUnlockedCipherListViewsOrNull() ?: return emptyList()
        // We only care about non-deleted login ciphers.
        val loginCiphers = cipherViews
            .filter {
                // Must be login type
                it.type is CipherListViewType.Login &&
                    // Must not be deleted.
                    it.deletedDate == null &&
                    // Must not require a reprompt.
                    it.reprompt == CipherRepromptType.NONE
            }

        return cipherMatchingManager
            // Filter for ciphers that match the uri in some way.
            .filterCiphersForMatches(
                cipherListViews = loginCiphers,
                matchUri = uri,
            )
            .mapNotNull { cipherListView ->
                cipherListView.id?.let { cipherId ->
                    decryptCipherOrNull(cipherId = cipherId)?.let { cipherView ->
                        AutofillCipher.Login(
                            cipherId = cipherView.id,
                            isTotpEnabled = cipherView.login?.totp != null,
                            name = cipherView.name,
                            password = cipherView.login?.password.orEmpty(),
                            subtitle = cipherView.subtitle.orEmpty(),
                            username = cipherView.login?.username.orEmpty(),
                        )
                    }
                }
            }
    }

    /**
     * Get available [CipherView]s if possible.
     */
    private suspend fun getUnlockedCipherListViewsOrNull(): List<CipherListView>? =
        vaultRepository
            .decryptCipherListResultStateFlow
            .takeUnless { isVaultLocked() }
            ?.firstWithTimeoutOrNull(timeMillis = GET_CIPHERS_TIMEOUT_MS) { it.data != null }
            ?.data
            ?.successes

    private suspend fun decryptCipherOrNull(cipherId: String): CipherView? =
        when (val result = vaultRepository.getCipher(cipherId = cipherId)) {
            GetCipherResult.CipherNotFound -> {
                Timber.e("Cipher not found for autofill.")
                null
            }
            is GetCipherResult.Failure -> {
                Timber.e(result.error, "Failed to decrypt cipher for autofill.")
                null
            }
            is GetCipherResult.Success -> result.cipherView
        }
}
