package com.bitwarden.cxf.manager

import android.app.Activity
import android.content.Intent
import androidx.credentials.providerevents.IntentHandler
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import com.bitwarden.cxf.manager.model.ExportCredentialsResult

/**
 * Primary implementation of [CredentialExchangeCompletionManager].
 */
internal class CredentialExchangeCompletionManagerImpl(
    private val activity: Activity,
) : CredentialExchangeCompletionManager {

    override fun completeCredentialExport(exportResult: ExportCredentialsResult) {
        val intent = Intent()
        when (exportResult) {
            is ExportCredentialsResult.Failure -> {
                IntentHandler.setImportCredentialsException(
                    intent = intent,
                    exception = exportResult.error,
                )
            }

            is ExportCredentialsResult.Success -> {
                IntentHandler.setImportCredentialsResponse(
                    context = activity,
                    uri = exportResult.uri,
                    response = ImportCredentialsResponse(
                        responseJson = exportResult.payload,
                    ),
                )
            }
        }
        activity.apply {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}
