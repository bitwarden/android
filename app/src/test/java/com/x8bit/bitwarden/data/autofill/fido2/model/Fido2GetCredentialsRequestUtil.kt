package com.x8bit.bitwarden.data.autofill.fido2.model

import android.os.Bundle

fun createMockFido2GetCredentialsRequest(
    number: Int,
): Fido2GetCredentialsRequest = Fido2GetCredentialsRequest(
    candidateQueryData = Bundle(),
    id = "mockId-$number",
    userId = "mockUserId-$number",
    requestJson = "requestJson-$number",
)
