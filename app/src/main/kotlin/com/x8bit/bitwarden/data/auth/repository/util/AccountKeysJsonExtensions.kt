package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.network.model.AccountKeysJson

/**
 * Creates a [WrappedAccountCryptographicState] based on the available cryptographic parameters.
 *
 * Returns [WrappedAccountCryptographicState.V2] if signing key, signed public key, and security
 * state are all present, otherwise returns [WrappedAccountCryptographicState.V1].
 *
 * @receiver The users account keys.
 * @param privateKey The user's wrapped private key.
 */
fun AccountKeysJson?.toAccountCryptographicState(
    privateKey: String,
): WrappedAccountCryptographicState {
    val securityState = this?.securityState?.securityState
    val signingKey = this?.signatureKeyPair?.wrappedSigningKey
    val signedPublicKey = this?.publicKeyEncryptionKeyPair?.signedPublicKey
    return if (signingKey != null && securityState != null && signedPublicKey != null) {
        WrappedAccountCryptographicState.V2(
            privateKey = privateKey,
            securityState = securityState,
            signingKey = signingKey,
            signedPublicKey = signedPublicKey,
        )
    } else {
        WrappedAccountCryptographicState.V1(
            privateKey = privateKey,
        )
    }
}
