package com.x8bit.bitwarden.data.credentials.model

import android.os.Bundle
import androidx.core.os.bundleOf

/**
 * Creates a mock [CreateCredentialRequest] with a given [number].
 */
fun createMockCreateCredentialRequest(
    number: Int,
    isUserPreVerified: Boolean = false,
    requestData: Bundle = bundleOf(),
): CreateCredentialRequest = CreateCredentialRequest(
    userId = "mockUserIdx-$number",
    isUserPreVerified = isUserPreVerified,
    requestData = requestData,
)
