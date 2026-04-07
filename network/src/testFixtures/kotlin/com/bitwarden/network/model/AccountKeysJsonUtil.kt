package com.bitwarden.network.model

/**
 * Create a mock set of private keys with a given [number].
 */
fun createMockAccountKeysJson(
    number: Int,
): AccountKeysJson =
    AccountKeysJson(
        publicKeyEncryptionKeyPair = createMockPublicKeyEncryptionKeyPair(number = number),
        securityState = createMockSecurityState(number = number),
        signatureKeyPair = createMockSignatureKeyPair(number = number),
    )

/**
 * Create a mock [AccountKeysJson.SecurityState] with a given [number].
 */
fun createMockSecurityState(
    number: Int,
    securityState: String = "mockSecurityState-$number",
    securityVersion: Int = number,
): AccountKeysJson.SecurityState =
    AccountKeysJson.SecurityState(
        securityState = securityState,
        securityVersion = securityVersion,
    )

/**
 * Create a mock [AccountKeysJson.PublicKeyEncryptionKeyPair] with a given
 * number.
 */
fun createMockPublicKeyEncryptionKeyPair(
    number: Int,
    publicKey: String = "mockPublicKey-$number",
    wrappedPrivateKey: String = "mockWrappedPrivateKey-$number",
    signedPublicKey: String? = "mockSignedPublicKey-$number",
): AccountKeysJson.PublicKeyEncryptionKeyPair =
    AccountKeysJson.PublicKeyEncryptionKeyPair(
        publicKey = publicKey,
        wrappedPrivateKey = wrappedPrivateKey,
        signedPublicKey = signedPublicKey,
    )

/**
 * Create a mock [AccountKeysJson.SignatureKeyPair] with a given number.
 */
fun createMockSignatureKeyPair(
    number: Int,
    wrappedSigningKey: String = "mockWrappedSigningKey-$number",
    verifyingKey: String = "mockVerifyingKey-$number",
): AccountKeysJson.SignatureKeyPair =
    AccountKeysJson.SignatureKeyPair(
        wrappedSigningKey = wrappedSigningKey,
        verifyingKey = verifyingKey,
    )

/**
 * Create a mock set of account keys with null nested fields for testing null-safety.
 */
fun createMockAccountKeysJsonWithNullFields(
    number: Int,
): AccountKeysJson =
    AccountKeysJson(
        publicKeyEncryptionKeyPair = createMockPublicKeyEncryptionKeyPair(
            number = number,
            signedPublicKey = null,
        ),
        securityState = null,
        signatureKeyPair = null,
    )
