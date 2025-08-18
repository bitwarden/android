package com.x8bit.bitwarden.data.credentials.builder

import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.vault.CipherListView
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager
import com.x8bit.bitwarden.data.credentials.util.setBiometricPromptDataIfSupported
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager

/**
 * Primary implementation of [CredentialEntryBuilder].
 */
class CredentialEntryBuilderImpl(
    private val context: Context,
    private val pendingIntentManager: CredentialManagerPendingIntentManager,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
) : CredentialEntryBuilder {

    override fun buildPublicKeyCredentialEntries(
        userId: String,
        fido2CredentialAutofillViews: List<Fido2CredentialAutofillView>,
        beginGetPublicKeyCredentialOptions: List<BeginGetPublicKeyCredentialOption>,
        isUserVerified: Boolean,
    ): List<PublicKeyCredentialEntry> = beginGetPublicKeyCredentialOptions
        .flatMap { option ->
            fido2CredentialAutofillViews
                .toPublicKeyCredentialEntryList(
                    userId = userId,
                    option = option,
                    isUserVerified = isUserVerified,
                )
        }

    override fun buildPasswordCredentialEntries(
        userId: String,
        cipherListViews: List<CipherListView>,
        beginGetPasswordCredentialOptions: List<BeginGetPasswordOption>,
        isUserVerified: Boolean,
    ): List<PasswordCredentialEntry> = beginGetPasswordCredentialOptions
        .flatMap { option ->
            cipherListViews
                .toPasswordCredentialEntryList(
                    userId = userId,
                    option = option,
                    isUserVerified = isUserVerified,
                )
        }

    private fun List<Fido2CredentialAutofillView>.toPublicKeyCredentialEntryList(
        userId: String,
        option: BeginGetPublicKeyCredentialOption,
        isUserVerified: Boolean,
    ): List<PublicKeyCredentialEntry> = this
        .map { fido2AutofillView ->
            PublicKeyCredentialEntry
                .Builder(
                    context = context,
                    username = fido2AutofillView.userNameForUi
                        ?: context.getString(BitwardenString.no_username),
                    pendingIntent = pendingIntentManager.createFido2GetCredentialPendingIntent(
                        userId = userId,
                        credentialId = fido2AutofillView.credentialId.toString(),
                        cipherId = fido2AutofillView.cipherId,
                        isUserVerified = isUserVerified,
                    ),
                    beginGetPublicKeyCredentialOption = option,
                )
                .setIcon(
                    getCredentialEntryIcon(
                        isPasskey = true,
                    ),
                )
                .also { builder ->
                    if (!isUserVerified) {
                        builder.setBiometricPromptDataIfSupported(
                            cipher = biometricsEncryptionManager.getOrCreateCipher(userId),
                        )
                    }
                }
                .build()
        }

    private fun List<CipherListView>.toPasswordCredentialEntryList(
        userId: String,
        option: BeginGetPasswordOption,
        isUserVerified: Boolean,
    ): List<PasswordCredentialEntry> = this
        .map { cipherView ->
            PasswordCredentialEntry
                .Builder(
                    context = context,
                    username = cipherView.login?.username
                        ?: context.getString(BitwardenString.no_username),
                    pendingIntent = pendingIntentManager.createPasswordGetCredentialPendingIntent(
                        userId = userId,
                        cipherId = cipherView.id,
                        isUserVerified = isUserVerified,
                    ),
                    beginGetPasswordOption = option,
                )
                .setDisplayName(cipherView.name)
                .setAutoSelectAllowed(this.size == 1)
                .setIcon(getCredentialEntryIcon())
                .apply {
                    if (!isUserVerified) {
                        setBiometricPromptDataIfSupported(
                            cipher = biometricsEncryptionManager
                                .getOrCreateCipher(userId),
                        )
                    }
                }
                .build()
        }

    // TODO: [PM-20176] Enable web icons in credential entries
    // Leave web icons disabled until CredentialManager TransactionTooLargeExceptions
    // are addressed. See https://issuetracker.google.com/issues/355141766 for details.
    private fun getCredentialEntryIcon(
        isPasskey: Boolean = false,
    ): Icon = IconCompat
        .createWithResource(
            context,
            when {
                isPasskey -> BitwardenDrawable.ic_bw_passkey
                else -> BitwardenDrawable.ic_globe
            },
        )
        .toIcon(context)
}
