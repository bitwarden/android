package com.bitwarden.authenticator.data.authenticator.repository.util

import androidx.core.net.toUri
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticatorbridge.model.SharedAccountData

/**
 * Convert a list of [SharedAccountData.Account] to a list of [AuthenticatorItem].
 */
fun List<SharedAccountData.Account>.toAuthenticatorItems(): List<AuthenticatorItem> =
    flatMap { sharedAccount ->
        sharedAccount.cipherData.mapNotNull { cipherData ->
            runCatching {
                val uri = cipherData.uri.toUri()
                val issuer = uri
                    .getQueryParameter(TotpCodeManager.ISSUER_PARAM)
                    ?.takeUnless { it.isBlank() }
                    ?: cipherData.name.takeUnless {
                        // TODO: PM-34085 The cipher name will never be blank once we
                        // TODO: remove the legacy support.
                        it.isBlank()
                    }
                val label = uri
                    .pathSegments
                    .firstOrNull()
                    ?.removePrefix("$issuer:")
                    ?.takeUnless { it.isBlank() }
                    ?: cipherData.username

                AuthenticatorItem(
                    source = AuthenticatorItem.Source.Shared(
                        userId = sharedAccount.userId,
                        nameOfUser = sharedAccount.name,
                        email = sharedAccount.email,
                        environmentLabel = sharedAccount.environmentLabel,
                    ),
                    otpUri = cipherData.uri,
                    issuer = issuer,
                    label = label,
                )
            }
                .getOrNull()
        }
    }
