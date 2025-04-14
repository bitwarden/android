package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.core.os.bundleOf

fun createMockFido2GetCredentialsRequest(
    number: Int,
    userId: String = "mockUserId-$number",
): Fido2GetCredentialsRequest = Fido2GetCredentialsRequest(
    userId = userId,
    requestData = bundleOf(),
)
