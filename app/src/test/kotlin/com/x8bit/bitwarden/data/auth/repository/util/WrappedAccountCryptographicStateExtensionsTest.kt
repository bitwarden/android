package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.network.model.AccountKeysJson
import com.bitwarden.network.model.AccountKeysJson.PublicKeyEncryptionKeyPair
import com.bitwarden.network.model.AccountKeysJson.SecurityState
import com.bitwarden.network.model.AccountKeysJson.SignatureKeyPair
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class WrappedAccountCryptographicStateExtensionsTest {
    @Test
    fun `privateKey should return correct value`() {
        assertEquals("v1PrivateKey", V1_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE.privateKey)
        assertEquals("v2PrivateKey", V2_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE.privateKey)
    }

    @Test
    fun `accountKeysJson should return correct value`() {
        assertNull(V1_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE.accountKeysJson)
        assertEquals(
            AccountKeysJson(
                publicKeyEncryptionKeyPair = PublicKeyEncryptionKeyPair(
                    publicKey = "",
                    signedPublicKey = "signedPublicKey",
                    wrappedPrivateKey = "v2PrivateKey",
                ),
                signatureKeyPair = SignatureKeyPair(
                    wrappedSigningKey = "signingKey",
                    verifyingKey = "",
                ),
                securityState = SecurityState(
                    securityState = "securityState",
                    securityVersion = 2,
                ),
            ),
            V2_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE.accountKeysJson,
        )
    }
}

private val V1_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE: WrappedAccountCryptographicState =
    WrappedAccountCryptographicState.V1(
        privateKey = "v1PrivateKey",
    )

private val V2_WRAPPED_ACCOUNT_CRYPTOGRAPHIC_STATE: WrappedAccountCryptographicState =
    WrappedAccountCryptographicState.V2(
        privateKey = "v2PrivateKey",
        securityState = "securityState",
        signingKey = "signingKey",
        signedPublicKey = "signedPublicKey",
    )
