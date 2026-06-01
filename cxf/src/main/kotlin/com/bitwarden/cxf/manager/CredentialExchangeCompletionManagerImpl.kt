package com.bitwarden.cxf.manager

import android.app.Activity
import android.content.Intent
import androidx.credentials.providerevents.IntentHandler
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import java.time.Clock

private const val CXF_FORMAT_VERSION_MAJOR = 1
private const val CXF_FORMAT_VERSION_MINOR = 0

/**
 * Primary implementation of [CredentialExchangeCompletionManager].
 *
 * @param activity The [Activity] that initiated the credential exchange operation.
 */
internal class CredentialExchangeCompletionManagerImpl(
    private val activity: Activity,
    private val clock: Clock,
) : CredentialExchangeCompletionManager {

    override fun completeCredentialExport(exportResult: ExportCredentialsResult) {
        val intent = Intent()
        when (exportResult) {
            is ExportCredentialsResult.Failure -> {
                finishWithError(intent = intent, error = exportResult.error)
            }

            is ExportCredentialsResult.Success -> {
                val headerJson = """
                    {
                        "version": {
                            "major": $CXF_FORMAT_VERSION_MAJOR,
                            "minor": $CXF_FORMAT_VERSION_MINOR
                        },
                        "exporterRpId": "${activity.packageName}",
                        "exporterDisplayName": "${activity.applicationInfo.name}",
                        "timestamp": ${clock.instant().epochSecond},
                        "accounts": [${exportResult.payload}]
                    }
                """
                    .trimIndent()

                IntentHandler.setImportCredentialsResponse(
                    context = activity,
                    intent = intent,
                    uri = exportResult.uri,
                    response = ImportCredentialsResponse(
                        responseJson = headerJson,
                    ),
                )
            }
        }
        activity.apply {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun finishWithError(error: ImportCredentialsException, intent: Intent) {
        IntentHandler.setImportCredentialsException(
            intent = intent,
            exception = error,
        )
        activity.apply {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}
