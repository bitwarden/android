package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CREDENTIAL_ID
import kotlin.random.Random

/**
 * Primary implementation of [Fido2CompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Fido2CompletionManagerImpl(
    private val activity: Activity,
) : Fido2CompletionManager {

    override fun completeFido2Registration(result: Fido2RegisterCredentialResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                is Fido2RegisterCredentialResult.Error -> {
                    PendingIntentHandler
                        .setCreateCredentialException(
                            intent = intent,
                            exception = CreateCredentialUnknownException(),
                        )
                }

                is Fido2RegisterCredentialResult.Success -> {
                    PendingIntentHandler
                        .setCreateCredentialResponse(
                            intent = intent,
                            response = CreatePublicKeyCredentialResponse(
                                registrationResponseJson = result.registrationResponse,
                            ),
                        )
                }

                is Fido2RegisterCredentialResult.Cancelled -> {
                    PendingIntentHandler
                        .setCreateCredentialException(
                            intent = intent,
                            exception = CreateCredentialCancellationException(),
                        )
                }
            }
            it.setResult(Activity.RESULT_OK, intent)
            it.finish()
        }
    }

    override fun completeFido2Assertion(result: Fido2CredentialAssertionResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                Fido2CredentialAssertionResult.Error -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialUnknownException(),
                        )
                }

                is Fido2CredentialAssertionResult.Success -> {
                    PendingIntentHandler
                        .setGetCredentialResponse(
                            intent = intent,
                            response = GetCredentialResponse(
                                credential = PublicKeyCredential(result.responseJson),
                            ),
                        )
                }
            }
            it.setResult(Activity.RESULT_OK, intent)
            it.finish()
        }
    }

    override fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsResult) {
        val entries = mutableListOf<CredentialEntry>()
        val resultIntent = Intent()
        val responseBuilder = BeginGetCredentialResponse.Builder()
        when (result) {
            is Fido2GetCredentialsResult.Success -> {
                result
                    .credentials
                    .onEach { credential ->
                        val credentialIntent = Intent(GET_PASSKEY_INTENT)
                            .setPackage(activity.packageName)
                            .putExtra(EXTRA_KEY_CIPHER_ID, credential.cipherId)
                            .putExtra(EXTRA_KEY_CREDENTIAL_ID, credential.credentialId.toString())
                        val pendingIntent = PendingIntent.getActivity(
                            activity,
                            Random.nextInt(),
                            credentialIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
                        )

                        entries.add(
                            PublicKeyCredentialEntry
                                .Builder(
                                    context = activity,
                                    username = credential.userNameForUi
                                        ?: activity.getString(R.string.no_username),
                                    pendingIntent = pendingIntent,
                                    beginGetPublicKeyCredentialOption = result.options,
                                )
                                .build(),
                        )
                    }

                PendingIntentHandler.setBeginGetCredentialResponse(
                    resultIntent,
                    responseBuilder
                        .setCredentialEntries(entries)
                        // Clear the existing authentication action so it is not displayed if the
                        // user does not have any matching credentials.
                        .setAuthenticationActions(emptyList())
                        .build(),
                )
            }

            Fido2GetCredentialsResult.Error,
            -> {
                PendingIntentHandler.setGetCredentialException(
                    resultIntent,
                    GetCredentialUnknownException(),
                )
            }
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}
