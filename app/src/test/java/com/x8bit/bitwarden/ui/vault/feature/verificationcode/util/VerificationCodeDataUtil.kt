package com.x8bit.bitwarden.ui.vault.feature.verificationcode.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem

fun createVerificationCodeItem(number: Int = 1) =
    VerificationCodeItem(
        code = "123456",
        totpCode = "mockTotp-$number",
        periodSeconds = 30,
        id = "mockId-$number",
        issueTime = 1698408000000,
        timeLeftSeconds = 30,
        name = "mockName-$number",
        uriLoginViewList = createMockLoginView(1).uris,
        username = "mockUsername-$number",
        hasPasswordReprompt = false,
        orgUsesTotp = false,
        orgId = null,
    )
