package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.network.model.AccountKeysJson
import com.bitwarden.network.model.AccountKeysJson.PublicKeyEncryptionKeyPair
import com.bitwarden.network.model.AccountKeysJson.SignatureKeyPair
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountKeysExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toAccountCryptographicState returns V2 when securityState, signedPublicKey and signingKey are non-null`() {
        val result = ACCOUNT_KEYS.toAccountCryptographicState(privateKey = PRIVATE_KEY)

        assertEquals(
            WrappedAccountCryptographicState.V2(
                privateKey = PRIVATE_KEY,
                signedPublicKey = SIGNED_PUBLIC_KEY,
                signingKey = SIGNING_KEY,
                securityState = SECURITY_STATE,
            ),
            result,
        )
    }

    @Test
    fun `toAccountCryptographicState returns V1 when securityState is null`() {
        val result = ACCOUNT_KEYS
            .copy(securityState = null)
            .toAccountCryptographicState(privateKey = PRIVATE_KEY)

        assertEquals(
            WrappedAccountCryptographicState.V1(privateKey = PRIVATE_KEY),
            result,
        )
    }

    @Test
    fun `toAccountCryptographicState returns V1 when signedPublicKey is null`() {
        val result = ACCOUNT_KEYS
            .copy(
                publicKeyEncryptionKeyPair = ACCOUNT_KEYS
                    .publicKeyEncryptionKeyPair
                    .copy(signedPublicKey = null),
            )
            .toAccountCryptographicState(privateKey = PRIVATE_KEY)

        assertEquals(
            WrappedAccountCryptographicState.V1(privateKey = PRIVATE_KEY),
            result,
        )
    }

    @Test
    fun `toAccountCryptographicState returns V1 when signingKey is null`() {
        val result = ACCOUNT_KEYS
            .copy(signatureKeyPair = null)
            .toAccountCryptographicState(privateKey = PRIVATE_KEY)

        assertEquals(
            WrappedAccountCryptographicState.V1(privateKey = PRIVATE_KEY),
            result,
        )
    }

    @Test
    fun `toAccountCryptographicState returns V1 when AccountKeysJson is null`() {
        val result = null.toAccountCryptographicState(privateKey = PRIVATE_KEY)

        assertEquals(
            WrappedAccountCryptographicState.V1(privateKey = PRIVATE_KEY),
            result,
        )
    }
}

private const val PRIVATE_KEY = "test-private-key"
private const val SECURITY_STATE = "test-security-state"
private const val SIGNING_KEY = "test-signing-key"
private const val SIGNED_PUBLIC_KEY = "test-signed-public-key"

private val ACCOUNT_KEYS = AccountKeysJson(
    signatureKeyPair = SignatureKeyPair(
        wrappedSigningKey = SIGNING_KEY,
        verifyingKey = "verifying-key",
    ),
    publicKeyEncryptionKeyPair = PublicKeyEncryptionKeyPair(
        wrappedPrivateKey = PRIVATE_KEY,
        signedPublicKey = SIGNED_PUBLIC_KEY,
        publicKey = "public-key",
    ),
    securityState = AccountKeysJson.SecurityState(
        securityState = SECURITY_STATE,
        securityVersion = 2,
    ),
)
