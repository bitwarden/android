package com.x8bit.bitwarden.ui.autofill.password.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.password.processor.GET_PASSWORD_INTENT
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Primary implementation of [PasswordCompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasswordCompletionManagerImpl(
    private val activity: Activity,
    private val intentManager: IntentManager,
) : PasswordCompletionManager {

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

    override fun completePasswordGetCredentialRequest(result: PasswordGetCredentialsResult) {
        val resultIntent = Intent()
        val responseBuilder = BeginGetCredentialResponse.Builder()
        when (result) {
            is PasswordGetCredentialsResult.Success -> {
                val entries = result
                    .credentials
                    .map {
                        val pendingIntent = intentManager
                            .createPasswordGetCredentialPendingIntent(
                                action = GET_PASSKEY_INTENT,
                                userId = result.userId,
                                id = result.option.id,
                                cipherId = it.cipherId!!, //TODO
                                requestCode = Random.nextInt(),
                            )
                        PasswordCredentialEntry
                            .Builder(
                                context = activity,
                                username = it.username,
                                pendingIntent = pendingIntent,
                                beginGetPasswordOption = result.option,
                            )
                            .build()
                    }
                PendingIntentHandler
                    .setBeginGetCredentialResponse(
                        resultIntent,
                        responseBuilder
                            .setCredentialEntries(entries)
                            // Explicitly clear any pending authentication actions since we only
                            // display results from the active account.
                            .setAuthenticationActions(emptyList())
                            .build(),
                    )
            }

            PasswordGetCredentialsResult.Error -> {
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
