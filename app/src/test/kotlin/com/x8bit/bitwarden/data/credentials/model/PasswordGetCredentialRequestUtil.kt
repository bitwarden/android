package com.x8bit.bitwarden.data.credentials.model

import androidx.core.os.bundleOf

fun createMockProviderGetPasswordCredentialRequest(
    userId: String = "mockUserId",
    cipherId: String = "mockCipherId",
): ProviderGetPasswordCredentialRequest =
    ProviderGetPasswordCredentialRequest(
        userId = userId,
        cipherId = cipherId,
        isUserPreVerified = false,
        requestData = bundleOf(),
    )
