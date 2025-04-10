package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.core.os.bundleOf

fun createMockFido2CredentialAssertionRequest(
    number: Int = 1,
    userId: String = "mockUserId-$number",
): Fido2CredentialAssertionRequest =
    Fido2CredentialAssertionRequest(
        userId = userId,
        cipherId = "mockCipherId-$number",
        credentialId = "mockCredentialId-$number",
        isUserPreVerified = false,
        requestData = bundleOf(),
    )
