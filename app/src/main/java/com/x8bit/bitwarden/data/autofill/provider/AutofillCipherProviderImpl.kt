package com.x8bit.bitwarden.data.autofill.provider

import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.platform.util.takeIfUriMatches
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import kotlinx.coroutines.flow.first

/**
 * The default [AutofillCipherProvider] implementation. This service is used for getting currrent
 * [AutofillCipher]s.
 */
class AutofillCipherProviderImpl(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : AutofillCipherProvider {
    private val activeUserId: String? get() = authRepository.activeUserId

    override suspend fun isVaultLocked(): Boolean {
        val userId = activeUserId ?: return true

        // Wait for any unlocking actions to finish. This can be relevant on startup for Never lock
        // accounts.
        vaultRepository.vaultStateFlow.first { userId !in it.unlockingVaultUserIds }

        return !vaultRepository.isVaultUnlocked(userId = userId)
    }

    override suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card> {
        val cipherViews = getUnlockedCiphersOrNull() ?: return emptyList()

        return cipherViews
            .mapNotNull { cipherView ->
                cipherView
                    .takeIf { cipherView.type == CipherType.CARD }
                    ?.let { nonNullCipherView ->
                        AutofillCipher.Card(
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

        return cipherViews
            .mapNotNull { cipherView ->
                cipherView
                    .takeIf { cipherView.type == CipherType.LOGIN }
                    // TODO: Get global URI matching value from settings repo and
                    // TODO: perform more complex URI matching here (BIT-1461).
                    ?.takeIfUriMatches(
                        uri = uri,
                    )
                    ?.let { nonNullCipherView ->
                        AutofillCipher.Login(
                            name = nonNullCipherView.name,
                            password = nonNullCipherView.login?.password.orEmpty(),
                            subtitle = nonNullCipherView.subtitle.orEmpty(),
                            username = nonNullCipherView.login?.username.orEmpty(),
                        )
                    }
            }
    }

    /**
     * Get available [CipherView]s if possible.
     */
    private suspend fun getUnlockedCiphersOrNull(): List<CipherView>? =
        vaultRepository
            .ciphersStateFlow
            .takeUnless { isVaultLocked() }
            ?.first { it.data != null }
            ?.data
}
