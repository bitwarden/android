package com.x8bit.bitwarden.data.credentials.model

import androidx.core.os.bundleOf

fun createMockProviderGetPasswordCredentialRequest(
    number: Int,
    userId: String = "mockId-$number",
    cipherId: String = "mockId-$number",
): ProviderGetPasswordCredentialRequest =
    ProviderGetPasswordCredentialRequest(
        userId = userId,
        cipherId = cipherId,
        isUserPreVerified = false,
        requestData = bundleOf(),
    )
