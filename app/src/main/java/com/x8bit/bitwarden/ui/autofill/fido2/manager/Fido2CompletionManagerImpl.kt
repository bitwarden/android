package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Primary implementation of [Fido2CompletionManager].
 */
@OmitFromCoverage
class Fido2CompletionManagerImpl(
    private val activity: Activity,
) : Fido2CompletionManager {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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
}
