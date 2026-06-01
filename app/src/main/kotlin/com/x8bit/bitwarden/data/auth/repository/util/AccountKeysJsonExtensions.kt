package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.network.model.AccountKeysJson
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState

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
): WrappedAccountCryptographicState = createWrappedAccountCryptographicState(
    privateKey = privateKey,
    securityState = this?.securityState?.securityState,
    signingKey = this?.signatureKeyPair?.wrappedSigningKey,
    signedPublicKey = this?.publicKeyEncryptionKeyPair?.signedPublicKey,
)
