package com.x8bit.bitwarden.data.autofill.password.processor

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

private const val CREATE_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_CREATE_PASSWORD"
const val GET_PASSWORD_INTENT = "com.x8bit.bitwarden.data.autofill.password.ACTION_GET_PASSWORD"

/**
 * The default implementation of [PasswordProviderProcessor]. Its purpose is to handle Password related
 * processing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class PasswordProviderProcessorImpl(
    private val context: Context,
    private val autofillCipherProvider: AutofillCipherProvider,
    private val intentManager: IntentManager,
    private val clock: Clock,
) : PasswordProviderProcessor {

    override suspend fun processCreateCredentialRequest(
        requestCode: AtomicInteger,
        userState: UserState,
        request: BeginCreatePasswordCredentialRequest,
    ): BeginCreateCredentialResponse {
        return BeginCreateCredentialResponse.Builder()
            .setCreateEntries(
                userState.accounts.toCreateEntries(
                    requestCode = requestCode,
                    activeUserId = userState.activeUserId
                )
            )
            .build()
    }

    private fun List<UserState.Account>.toCreateEntries(
        requestCode: AtomicInteger,
        activeUserId: String
    ) = map {
        it.toCreateEntry(
            requestCode = requestCode,
            isActive = activeUserId == it.userId,
        )
    }

    private fun UserState.Account.toCreateEntry(
        requestCode: AtomicInteger,
        isActive: Boolean,
    ): CreateEntry {
        val accountName = name ?: email
        return CreateEntry
            .Builder(
                accountName = accountName,
                pendingIntent = intentManager.createPasswordCreationPendingIntent(
                    CREATE_PASSWORD_INTENT,
                    userId,
                    requestCode.getAndIncrement(),
                ),
            )
            .setDescription(
                context.getString(
                    R.string.your_passkey_will_be_saved_to_your_bitwarden_vault_for_x, //TODO change text to your password will be saved
                    accountName,
                ),
            )
            // Set the last used time to "now" so the active account is the default option in the
            // system prompt.
            .setLastUsedTime(if (isActive) clock.instant() else null)
            .build()
    }

    override suspend fun processGetCredentialRequest(
        requestCode: AtomicInteger,
        activeUserId: String,
        callingAppInfo: CallingAppInfo?,
        beginGetPasswordOptions: List<BeginGetPasswordOption>,
    ): List<CredentialEntry> {
        return getMatchingPasswordCredentialEntries(
            requestCode = requestCode,
            userId = activeUserId,
            callingAppInfo = callingAppInfo,
            beginGetPasswordOptions = beginGetPasswordOptions,
        )
    }

    @Throws(GetCredentialUnsupportedException::class)
    private suspend fun getMatchingPasswordCredentialEntries(
        requestCode: AtomicInteger,
        userId: String,
        callingAppInfo: CallingAppInfo?,
        beginGetPasswordOptions: List<BeginGetPasswordOption>,
    ): List<CredentialEntry> =
        beginGetPasswordOptions.flatMap { option ->
            if (option.allowedUserIds.isEmpty() || option.allowedUserIds.contains(userId)) {
                buildCredentialEntries(
                    requestCode = requestCode,
                    userId = userId,
                    matchUri = callingAppInfo?.origin
                        ?: callingAppInfo?.packageName
                            ?.toAndroidAppUriString(),
                    option = option,
                )
            } else {
                //userid did not match any in allowedUserIds
                emptySet()
            }
        }

    private suspend fun buildCredentialEntries(
        requestCode: AtomicInteger,
        userId: String,
        matchUri: String?,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> {
        return autofillCipherProvider.getLoginAutofillCiphers(
            uri = matchUri ?: return emptyList(),
        ).toCredentialEntries(
            requestCode = requestCode,
            userId = userId,
            option = option,
        )
    }

    private fun List<AutofillCipher.Login>.toCredentialEntries(
        requestCode: AtomicInteger,
        userId: String,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> =
        this
            .mapNotNull {
                PasswordCredentialEntry
                    .Builder(
                        context = context,
                        username = it.username,
                        pendingIntent = intentManager
                            .createPasswordGetCredentialPendingIntent(
                                action = GET_PASSWORD_INTENT,
                                id = option.id,
                                userId = userId,
                                cipherId = it.cipherId ?: return@mapNotNull null,
                                requestCode = requestCode.getAndIncrement(),
                            ),
                        beginGetPasswordOption = option,
                    )
                    .build()
            }

}
