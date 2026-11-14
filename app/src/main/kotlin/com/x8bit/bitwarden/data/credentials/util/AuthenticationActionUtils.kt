package com.x8bit.bitwarden.data.credentials.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.AuthenticationAction
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager

/**
 * Creates actions for signed-in accounts whose credentials are not already displayed.
 * [userIdWithEntries] identifies the queried vault, including an empty result, or is null when
 * no vault has been queried yet.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun List<UserState.Account>.toAuthenticationActions(
    context: Context,
    pendingIntentManager: CredentialManagerPendingIntentManager,
    userIdWithEntries: String?,
): List<AuthenticationAction> {
    val signedInAccounts = filter { it.isLoggedIn }
    return signedInAccounts
        .filter { it.userId != userIdWithEntries }
        .map { account ->
            AuthenticationAction(
                title = if (signedInAccounts.size == 1 && !account.isVaultUnlocked) {
                    context.getString(BitwardenString.unlock)
                } else {
                    "${account.email} (${account.environment.label})"
                },
                pendingIntent = pendingIntentManager.createFido2UnlockPendingIntent(
                    userId = account.userId,
                ),
            )
        }
}
