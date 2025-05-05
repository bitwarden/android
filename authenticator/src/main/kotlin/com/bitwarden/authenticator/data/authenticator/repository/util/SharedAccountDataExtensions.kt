package com.bitwarden.authenticator.data.authenticator.repository.util

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticatorbridge.model.SharedAccountData

/**
 * Convert a list of [SharedAccountData.Account] to a list of [AuthenticatorItem].
 */
fun List<SharedAccountData.Account>.toAuthenticatorItems(): List<AuthenticatorItem> =
    flatMap { sharedAccount ->
        sharedAccount.totpUris.mapNotNull { totpUriString ->
            runCatching {
                val uri = Uri.parse(totpUriString)
                val issuer = uri.getQueryParameter(TotpCodeManager.ISSUER_PARAM)
                val label = uri.pathSegments
                    .firstOrNull()
                    ?.removePrefix("$issuer:")

                AuthenticatorItem(
                    source = AuthenticatorItem.Source.Shared(
                        userId = sharedAccount.userId,
                        nameOfUser = sharedAccount.name,
                        email = sharedAccount.email,
                        environmentLabel = sharedAccount.environmentLabel,
                    ),
                    otpUri = totpUriString,
                    issuer = issuer,
                    label = label,
                )
            }
                .getOrNull()
        }
    }
