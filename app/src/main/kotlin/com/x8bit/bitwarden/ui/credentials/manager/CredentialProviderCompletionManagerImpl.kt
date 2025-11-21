package com.x8bit.bitwarden.ui.credentials.manager

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult

/**
 * Primary implementation of [CredentialProviderCompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialProviderCompletionManagerImpl(
    private val activity: Activity,
) : CredentialProviderCompletionManager {

    override fun completeCredentialRegistration(result: CreateCredentialResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                is CreateCredentialResult.Error -> {
                    PendingIntentHandler
                        .setCreateCredentialException(
                            intent = intent,
                            exception = CreateCredentialUnknownException(
                                errorMessage = result.message.invoke(it.resources),
                            ),
                        )
                }

                is CreateCredentialResult.Success.Fido2CredentialRegistered -> {
                    PendingIntentHandler
                        .setCreateCredentialResponse(
                            intent = intent,
                            response = CreatePublicKeyCredentialResponse(
                                registrationResponseJson = result.responseJson,
                            ),
                        )
                }

                is CreateCredentialResult.Success.PasswordCreated -> {
                    PendingIntentHandler
                        .setCreateCredentialResponse(
                            intent = intent,
                            response = CreatePasswordResponse(),
                        )
                }

                is CreateCredentialResult.Cancelled -> {
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

    override fun completeFido2Assertion(result: AssertFido2CredentialResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                is AssertFido2CredentialResult.Error -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialUnknownException(
                                errorMessage = result.message.invoke(it.resources),
                            ),
                        )
                }

                is AssertFido2CredentialResult.Success -> {
                    PendingIntentHandler
                        .setGetCredentialResponse(
                            intent = intent,
                            response = GetCredentialResponse(
                                credential = PublicKeyCredential(result.responseJson),
                            ),
                        )
                }

                is AssertFido2CredentialResult.Cancelled -> {
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

    override fun completePasswordGet(result: GetPasswordCredentialResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                is GetPasswordCredentialResult.Error -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialUnknownException(
                                errorMessage = result.message.invoke(it.resources),
                            ),
                        )
                }

                is GetPasswordCredentialResult.Success -> {
                    PendingIntentHandler
                        .setGetCredentialResponse(
                            intent = intent,
                            response = GetCredentialResponse(
                                credential = PasswordCredential(
                                    id = result.credential.username ?: "",
                                    password = result.credential.password ?: "",
                                ),
                            ),
                        )
                }

                is GetPasswordCredentialResult.Cancelled -> {
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

    override fun completeProviderGetCredentialsRequest(result: GetCredentialsResult) {
        val resultIntent = Intent()
        val responseBuilder = BeginGetCredentialResponse.Builder()
        when (result) {
            is GetCredentialsResult.Success -> {
                PendingIntentHandler
                    .setBeginGetCredentialResponse(
                        resultIntent,
                        responseBuilder
                            .setCredentialEntries(result.credentialEntries)
                            // Explicitly clear any pending authentication actions since we only
                            // display results from the active account.
                            .setAuthenticationActions(emptyList())
                            .build(),
                    )
            }

            is GetCredentialsResult.Error -> {
                PendingIntentHandler.setGetCredentialException(
                    resultIntent,
                    GetCredentialUnknownException(
                        errorMessage = result.message.invoke(activity.resources),
                    ),
                )
            }
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}
