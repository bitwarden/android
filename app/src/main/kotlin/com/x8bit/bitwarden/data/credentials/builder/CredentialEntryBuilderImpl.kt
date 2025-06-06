package com.x8bit.bitwarden.data.credentials.builder

import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.credentials.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.credentials.processor.GET_PASSWORD_INTENT
import com.x8bit.bitwarden.data.credentials.util.setBiometricPromptDataIfSupported
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlin.random.Random

/**
 * Primary implementation of [CredentialEntryBuilder].
 */
class CredentialEntryBuilderImpl(
    private val context: Context,
    private val intentManager: IntentManager,
    private val featureFlagManager: FeatureFlagManager,
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
        passwordCredentialAutofillViews: List<AutofillCipher.Login>,
        beginGetPasswordCredentialOptions: List<BeginGetPasswordOption>,
        isUserVerified: Boolean,
    ): List<PasswordCredentialEntry> = beginGetPasswordCredentialOptions
        .flatMap { option ->
            passwordCredentialAutofillViews
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
                        ?: context.getString(R.string.no_username),
                    pendingIntent = intentManager
                        .createFido2GetCredentialPendingIntent(
                            action = GET_PASSKEY_INTENT,
                            userId = userId,
                            credentialId = fido2AutofillView.credentialId.toString(),
                            cipherId = fido2AutofillView.cipherId,
                            isUserVerified = isUserVerified,
                            requestCode = Random.nextInt(),
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
                            cipher = biometricsEncryptionManager
                                .getOrCreateCipher(userId),
                            isSingleTapAuthEnabled = featureFlagManager
                                .getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication),
                        )
                    }
                }
                .build()
        }

    private fun List<AutofillCipher.Login>.toPasswordCredentialEntryList(
        userId: String,
        option: BeginGetPasswordOption,
        isUserVerified: Boolean,
    ): List<PasswordCredentialEntry> = this
        .map { cipherView ->
            PasswordCredentialEntry
                .Builder(
                    context = context,
                    username = cipherView.username,
                    pendingIntent = intentManager
                        .createPasswordGetCredentialPendingIntent(
                            action = GET_PASSWORD_INTENT,
                            userId = userId,
                            cipherId = cipherView.cipherId,
                            isUserVerified = isUserVerified,
                            requestCode = Random.nextInt(),
                        ),
                    beginGetPasswordOption = option,
                )
                .setDisplayName(cipherView.name)
                .setIcon(
                    getCredentialEntryIcon(
                        isPassword = true,
                    ),
                )
                .also { builder ->
                    if (!isUserVerified) {
                        builder.setBiometricPromptDataIfSupported(
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
        isPassword: Boolean = false,
    ): Icon = IconCompat
        .createWithResource(
            context,
            when {
                isPasskey -> R.drawable.ic_bw_passkey
                isPassword -> R.drawable.ic_key
                else -> R.drawable.ic_globe
            },
        )
        .toIcon(context)
}
