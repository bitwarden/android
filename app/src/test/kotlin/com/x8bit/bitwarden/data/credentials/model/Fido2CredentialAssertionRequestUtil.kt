package com.x8bit.bitwarden.data.credentials.model

import androidx.core.os.bundleOf

fun createMockFido2CredentialAssertionRequest(
    number: Int = 1,
    userId: String = "mockUserId-$number",
    cipherId: String = "mockCipherId-$number",
): Fido2CredentialAssertionRequest =
    Fido2CredentialAssertionRequest(
        userId = userId,
        cipherId = cipherId,
        credentialId = "mockCredentialId-$number",
        isUserPreVerified = false,
        requestData = bundleOf(),
    )
