package com.x8bit.bitwarden.ui.autofill.password.manager

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * Primary implementation of [PasswordCompletionManager] when the build version is
 * UPSIDE_DOWN_CAKE (34) or above.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasswordCompletionManagerImpl(
    private val activity: Activity,
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

    override fun completePasswordGetCredentialRequest(result: PasswordGetCredentialsResult) {
        val resultIntent = Intent()
        when (result) {
            is PasswordGetCredentialsResult.Success -> {
                PendingIntentHandler
                    .setGetCredentialResponse(
                        resultIntent,
                        response = GetCredentialResponse(
                            credential = PasswordCredential(
                                id = result.credential.username ?: "",
                                password = result.credential.password ?: "",
                            ),
                        ),
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
