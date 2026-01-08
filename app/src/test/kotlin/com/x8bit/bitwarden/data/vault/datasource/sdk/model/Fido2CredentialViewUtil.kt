package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.vault.Fido2CredentialView
import java.time.Instant

/**
 * Creates a [Fido2CredentialView] instance for testing.
 */
fun createMockFido2CredentialView(number: Int): Fido2CredentialView = Fido2CredentialView(
    credentialId = "mockCredentialId-$number",
    keyType = "mockKeyType-$number",
    keyAlgorithm = "mockKeyAlgorithm-$number",
    keyCurve = "mockKeyCurve-$number",
    keyValue = "mockKeyValue-$number",
    rpId = "mockRpId-$number",
    userHandle = "mockUserHandle-$number",
    userName = "mockUserName-$number",
    counter = "$number",
    rpName = "mockRpName-$number",
    userDisplayName = "mockUserDisplayName-$number",
    discoverable = "mockDiscoverable-$number",
    creationDate = Instant.parse("2023-10-27T12:00:00Z"),
)

/**
 * Creates a [createMockFido2CredentialAutofillView] instance for testing.
 */
fun createMockFido2CredentialAutofillView(
    number: Int,
): Fido2CredentialAutofillView = Fido2CredentialAutofillView(
    credentialId = "mockCredentialId-$number".toByteArray(),
    cipherId = "mockCipherId-$number",
    rpId = "mockRelyingPartyId-$number",
    userNameForUi = "mockUserNameForUi-$number",
    userHandle = "mockUserHandle-$number".toByteArray(),
    hasCounter = false,
)
