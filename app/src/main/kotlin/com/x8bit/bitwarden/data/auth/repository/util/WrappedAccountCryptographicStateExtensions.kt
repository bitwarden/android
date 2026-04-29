package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.network.model.AccountKeysJson
import com.bitwarden.network.model.AccountKeysJson.PublicKeyEncryptionKeyPair
import com.bitwarden.network.model.AccountKeysJson.SecurityState
import com.bitwarden.network.model.AccountKeysJson.SignatureKeyPair

/**
 * The user's encryption private key, wrapped by the user key.
 */
val WrappedAccountCryptographicState.privateKey: String
    get() = when (this) {
        is WrappedAccountCryptographicState.V1 -> this.privateKey
        is WrappedAccountCryptographicState.V2 -> this.privateKey
    }

/**
 * Converts the [WrappedAccountCryptographicState] into a [AccountKeysJson].
 *
 * @receiver `WrappedAccountCryptographicState` to convert to `AccountEncryptionKeysJson`.
 */
val WrappedAccountCryptographicState.accountKeysJson: AccountKeysJson?
    get() = when (this) {
        is WrappedAccountCryptographicState.V1 -> null
        is WrappedAccountCryptographicState.V2 -> AccountKeysJson(
            publicKeyEncryptionKeyPair = PublicKeyEncryptionKeyPair(
                publicKey = "",
                signedPublicKey = this.signedPublicKey,
                wrappedPrivateKey = this.privateKey,
            ),
            signatureKeyPair = SignatureKeyPair(
                wrappedSigningKey = this.signingKey,
                verifyingKey = "",
            ),
            securityState = SecurityState(
                securityState = this.securityState,
                securityVersion = 2,
            ),
        )
    }
