package com.x8bit.bitwarden.ui.vault.feature.verificationcode.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem

fun createVerificationCodeItem() =
    VerificationCodeItem(
        code = "123456",
        totpCode = "mockTotp-1",
        periodSeconds = 30,
        id = "mockId-1",
        issueTime = 1698408000000,
        timeLeftSeconds = 30,
        name = "mockName-1",
        uriLoginViewList = createMockLoginView(1).uris,
        username = "mockUsername-1",
    )
