package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val PRIVATE_KEY = "test-private-key"
private const val SECURITY_STATE = "test-security-state"
private const val SIGNING_KEY = "test-signing-key"
private const val SIGNED_PUBLIC_KEY = "test-signed-public-key"

class WrappedAccountCryptographicStateExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `createWrappedAccountCryptographicState returns V2 when securityState, signedPublicKey and signingKey are non-null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = SECURITY_STATE,
            signedPublicKey = SIGNED_PUBLIC_KEY,
            signingKey = SIGNING_KEY,
        )

        val v2State = result as WrappedAccountCryptographicState.V2
        assertEquals(PRIVATE_KEY, v2State.privateKey)
        assertEquals(SECURITY_STATE, v2State.securityState)
        assertEquals(SIGNED_PUBLIC_KEY, v2State.signedPublicKey)
        assertEquals(SIGNING_KEY, v2State.signingKey)
    }

    @Test
    fun `createWrappedAccountCryptographicState returns V1 when securityState is null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = null,
            signedPublicKey = SIGNED_PUBLIC_KEY,
            signingKey = SIGNING_KEY,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }

    @Test
    fun `createWrappedAccountCryptographicState returns V1 when signedPublicKey is null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = SECURITY_STATE,
            signedPublicKey = null,
            signingKey = SIGNING_KEY,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }

    @Test
    fun `createWrappedAccountCryptographicState returns V1 when signingKey is null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = SECURITY_STATE,
            signedPublicKey = SIGNED_PUBLIC_KEY,
            signingKey = null,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createWrappedAccountCryptographicState returns V1 when both securityState and signedPublicKey are null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = null,
            signedPublicKey = null,
            signingKey = SIGNING_KEY,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createWrappedAccountCryptographicState returns V1 when both securityState and signingKey are null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = null,
            signedPublicKey = SIGNED_PUBLIC_KEY,
            signingKey = null,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createWrappedAccountCryptographicState returns V1 when both signedPublicKey and signingKey are null`() {
        val result = createWrappedAccountCryptographicState(
            privateKey = PRIVATE_KEY,
            securityState = SECURITY_STATE,
            signedPublicKey = null,
            signingKey = null,
        )

        val v1State = result as WrappedAccountCryptographicState.V1
        assertEquals(PRIVATE_KEY, v1State.privateKey)
    }
}
