package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo

fun createMockFido2CredentialAssertionRequest(
    number: Int = 1,
    userId: String = "mockUserId-$number",
): Fido2CredentialAssertionRequest =
    Fido2CredentialAssertionRequest(
        userId = userId,
        cipherId = "mockCipherId-$number",
        credentialId = "mockCredentialId-$number",
        requestJson = "mockRequestJson-$number",
        clientDataHash = byteArrayOf(0),
        packageName = "mockPackageName-$number",
        signingInfo = SigningInfo(),
        origin = "mockOrigin-$number",
    )
