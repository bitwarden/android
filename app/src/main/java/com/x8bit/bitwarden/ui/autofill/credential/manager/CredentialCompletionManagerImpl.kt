package com.x8bit.bitwarden.ui.autofill.credential.manager

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
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.credential.model.getCredentialResponseAction
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * Primary implementation of [CredentialCompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialCompletionManagerImpl(
    private val activity: Activity,
) : CredentialCompletionManager {

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

    override fun completePasswordRegistration(result: PasswordRegisterCredentialResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                is PasswordRegisterCredentialResult.Error -> {
                    PendingIntentHandler
                        .setCreateCredentialException(
                            intent = intent,
                            exception = CreateCredentialUnknownException(),
                        )
                }

                is PasswordRegisterCredentialResult.Success -> {
                    PendingIntentHandler
                        .setCreateCredentialResponse(
                            intent = intent,
                            response = CreatePasswordResponse(),
                        )
                }
            }
            it.setResult(Activity.RESULT_OK, intent)
            it.finish()
        }
    }

    override fun completePasswordAssertion(result: PasswordCredentialAssertionResult) {
        activity.also {
            val intent = Intent()
            when (result) {
                PasswordCredentialAssertionResult.Error -> {
                    PendingIntentHandler
                        .setGetCredentialException(
                            intent = intent,
                            exception = GetCredentialUnknownException(),
                        )
                }

                is PasswordCredentialAssertionResult.Success -> {
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
            }
            it.setResult(Activity.RESULT_OK, intent)
            it.finish()
        }
    }

    override fun completeGetCredentialRequest(
        fido2Result: Fido2GetCredentialsResult?,
        passwordResult: PasswordGetCredentialsResult?,
    ) {
        val resultIntent = Intent()
        val responseBuilder = BeginGetCredentialResponse.Builder()
        val fido2Entries: List<CredentialEntry> = when (fido2Result) {
            is Fido2GetCredentialsResult.Success -> fido2Result.credentials

            Fido2GetCredentialsResult.Error,
            null,
                -> emptyList()
        }

        val passwordEntries: List<CredentialEntry> = when (passwordResult) {
            is PasswordGetCredentialsResult.Success -> passwordResult.credentials

            PasswordGetCredentialsResult.Error,
            null,
                -> emptyList()
        }

        val entries: List<CredentialEntry> = fido2Entries.plus(passwordEntries)

        if (entries.isEmpty()) {
            PendingIntentHandler.setGetCredentialException(
                resultIntent,
                GetCredentialUnknownException(),
            )
        } else {
            PendingIntentHandler
                .setBeginGetCredentialResponse(
                    resultIntent,
                    responseBuilder
                        .setCredentialEntries(entries)
                        // Explicitly clear any pending authentication actions since we only
                        // display results from the active account.
                        .setAuthenticationActions(emptyList())
                        .addAction(getCredentialResponseAction(activity))
                        .build(),
                )
        }

        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}
