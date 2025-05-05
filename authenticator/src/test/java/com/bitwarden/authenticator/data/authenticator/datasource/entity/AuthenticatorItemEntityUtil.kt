package com.bitwarden.authenticator.data.authenticator.datasource.entity

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager

fun createMockAuthenticatorItemEntity(number: Int): AuthenticatorItemEntity =
    AuthenticatorItemEntity(
        id = "mockId-$number",
        key = "mockKey-$number",
        type = AuthenticatorItemType.TOTP,
        algorithm = TotpCodeManager.ALGORITHM_DEFAULT,
        period = TotpCodeManager.PERIOD_SECONDS_DEFAULT,
        digits = TotpCodeManager.TOTP_DIGITS_DEFAULT,
        issuer = "mockIssuer-$number",
        userId = null,
        accountName = "mockAccountName-$number",
        favorite = false,
    )
