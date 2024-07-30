package com.x8bit.bitwarden.data.autofill.provider

import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.util.firstWithTimeoutOrNull
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor

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
        val cipherViews = getUnlockedCiphersOrNull() ?: return emptyList()

        return cipherViews
            .mapNotNull { cipherView ->
                cipherView
                    // We only care about non-deleted card ciphers.
                    .takeIf {
                        // Must be card type.
                        cipherView.type == CipherType.CARD &&
                            // Must not be deleted.
                            cipherView.deletedDate == null &&
                            // Must not require a reprompt.
                            it.reprompt == CipherRepromptType.NONE
                    }
                    ?.let { nonNullCipherView ->
                        AutofillCipher.Card(
                            cipherId = cipherView.id,
                            name = nonNullCipherView.name,
                            subtitle = nonNullCipherView.subtitle.orEmpty(),
                            cardholderName = nonNullCipherView.card?.cardholderName.orEmpty(),
                            code = nonNullCipherView.card?.code.orEmpty(),
                            expirationMonth = nonNullCipherView.card?.expMonth.orEmpty(),
                            expirationYear = nonNullCipherView.card?.expYear.orEmpty(),
                            number = nonNullCipherView.card?.number.orEmpty(),
                        )
                    }
            }
    }

    override suspend fun getLoginAutofillCiphers(
        uri: String,
    ): List<AutofillCipher.Login> {
        val cipherViews = getUnlockedCiphersOrNull() ?: return emptyList()
        // We only care about non-deleted login ciphers.
        val loginCiphers = cipherViews
            .filter {
                // Must be login type
                it.type == CipherType.LOGIN &&
                    // Must not be deleted.
                    it.deletedDate == null &&
                    // Must not require a reprompt.
                    it.reprompt == CipherRepromptType.NONE
            }

        return cipherMatchingManager
            // Filter for ciphers that match the uri in some way.
            .filterCiphersForMatches(
                ciphers = loginCiphers,
                matchUri = uri,
            )
            .map { cipherView ->
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

    /**
     * Get available [CipherView]s if possible.
     */
    private suspend fun getUnlockedCiphersOrNull(): List<CipherView>? =
        vaultRepository
            .ciphersStateFlow
            .takeUnless { isVaultLocked() }
            ?.firstWithTimeoutOrNull(timeMillis = GET_CIPHERS_TIMEOUT_MS) { it.data != null }
            ?.data
}
