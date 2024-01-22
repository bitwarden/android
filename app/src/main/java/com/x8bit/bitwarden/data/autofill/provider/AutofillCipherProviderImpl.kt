package com.x8bit.bitwarden.data.autofill.provider

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
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
        // TODO: fulfill with real ciphers (BIT-1294)
        return if (isVaultLocked()) emptyList() else cardCiphers
    }

    override suspend fun getLoginAutofillCiphers(
        uri: String,
    ): List<AutofillCipher.Login> {
        // TODO: fulfill with real ciphers (BIT-1294)
        return if (isVaultLocked()) emptyList() else loginCiphers
    }
}

private val cardCiphers = listOf(
    AutofillCipher.Card(
        cardholderName = "John",
        code = "123",
        expirationMonth = "January",
        expirationYear = "1999",
        name = "John",
        number = "1234567890",
        subtitle = "123...",
    ),
    AutofillCipher.Card(
        cardholderName = "Doe",
        code = "456",
        expirationMonth = "December",
        expirationYear = "2024",
        name = "Doe",
        number = "0987654321",
        subtitle = "098...",
    ),
)
private val loginCiphers = listOf(
    AutofillCipher.Login(
        name = "Bitwarden1",
        password = "password123",
        subtitle = "John-Bitwarden",
        username = "John-Bitwarden",
    ),
    AutofillCipher.Login(
        name = "Bitwarden2",
        password = "password123",
        subtitle = "Doe-Bitwarden",
        username = "Doe-Bitwarden",
    ),
)
