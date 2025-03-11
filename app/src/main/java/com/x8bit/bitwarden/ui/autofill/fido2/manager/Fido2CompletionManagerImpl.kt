package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2AssertionCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2GetCredentialsCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2RegistrationCompletion

/**
 * Primary implementation of [Fido2CompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Fido2CompletionManagerImpl(
    private val activity: Activity,
) : Fido2CompletionManager {

    override fun completeFido2Registration(result: Fido2RegistrationCompletion) {
        activity.also {
            val intent = Intent()
            when (result) {
                is Fido2RegistrationCompletion.Error -> {
                    PendingIntentHandler
                        .setCreateCredentialException(
                            intent = intent,
                            exception = CreateCredentialUnknownException(),
                        )
                }

                is Fido2RegistrationCompletion.Success -> {
                    PendingIntentHandler
                        .setCreateCredentialResponse(
                            intent = intent,
                            response = CreatePublicKeyCredentialResponse(
                                registrationResponseJson = result.responseJson,
                            ),
                        )
                }

                is Fido2RegistrationCompletion.Cancelled -> {
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

    override fun completeFido2Assertion(result: Fido2AssertionCompletion) {
        activity.also {
            val intent = Intent()
            when (result) {
                is Fido2AssertionCompletion.Error -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialUnknownException(),
                        )
                }

                is Fido2AssertionCompletion.Success -> {
                    PendingIntentHandler
                        .setGetCredentialResponse(
                            intent = intent,
                            response = GetCredentialResponse(
                                credential = PublicKeyCredential(result.responseJson),
                            ),
                        )
                }

                is Fido2AssertionCompletion.Cancelled -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialCancellationException(),
                        )
                }
            }
            it.setResult(Activity.RESULT_OK, intent)
            it.finish()
        }
    }

    override fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsCompletion) {
        val resultIntent = Intent()
        val responseBuilder = BeginGetCredentialResponse.Builder()
        when (result) {
            is Fido2GetCredentialsCompletion.Success -> {
                PendingIntentHandler
                    .setBeginGetCredentialResponse(
                        resultIntent,
                        responseBuilder
                            .setCredentialEntries(result.entries)
                            // Explicitly clear any pending authentication actions since we only
                            // display results from the active account.
                            .setAuthenticationActions(emptyList())
                            .build(),
                    )
            }

            is Fido2GetCredentialsCompletion.Error -> {
                PendingIntentHandler.setGetCredentialException(
                    resultIntent,
                    GetCredentialUnknownException(),
                )
            }

            Fido2GetCredentialsCompletion.Cancelled -> {
                PendingIntentHandler.setGetCredentialException(
                    resultIntent,
                    GetCredentialCancellationException(),
                )
            }
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}
