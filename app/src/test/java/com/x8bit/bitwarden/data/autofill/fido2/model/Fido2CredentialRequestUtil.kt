package com.x8bit.bitwarden.data.autofill.fido2.model

import android.os.Bundle
import androidx.core.os.bundleOf

/**
 * Creates a mock [Fido2CreateCredentialRequest] with a given [number].
 */
fun createMockFido2CreateCredentialRequest(
    number: Int,
    requestData: Bundle = bundleOf(),
): Fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
    userId = "mockUserId-$number",
    requestData = requestData,
)
