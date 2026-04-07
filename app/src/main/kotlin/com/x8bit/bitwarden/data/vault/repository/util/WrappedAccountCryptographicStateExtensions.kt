package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState

/**
 * Creates a [WrappedAccountCryptographicState] based on the available cryptographic parameters.
 *
 * Returns [WrappedAccountCryptographicState.V2] if signing key, signed public key, and security
 * state are all present, otherwise returns [WrappedAccountCryptographicState.V1].
 *
 * @param privateKey The user's wrapped private key.
 * @param securityState The user's signed security state (V2 only).
 * @param signingKey The user's wrapped signing key (V2 only).
 * @param signedPublicKey The user's signed public key (V2 only).
 */
fun createWrappedAccountCryptographicState(
    privateKey: String,
    securityState: String?,
    signingKey: String?,
    signedPublicKey: String?,
): WrappedAccountCryptographicState {
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
