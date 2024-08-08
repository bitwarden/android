package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo
import android.os.Bundle

fun createMockFido2GetCredentialsRequest(
    number: Int,
    signingInfo: SigningInfo = SigningInfo(),
    origin: String? = null,
): Fido2GetCredentialsRequest = Fido2GetCredentialsRequest(
    candidateQueryData = Bundle(),
    id = "mockId-$number",
    requestJson = "requestJson-$number",
    clientDataHash = null,
    packageName = "mockPackageName-$number",
    signingInfo = signingInfo,
    origin = origin,
)
