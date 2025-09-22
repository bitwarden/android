package androidx.credentials.providerevents

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.SigningInfo
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Placeholder interface representing a provider events manager.
 */
interface ProviderEventsManager {

    /**
     * Register as a credential export source.
     */
    fun registerExport(request: RegisterExportRequest): RegisterExportResponse

    /**
     * Begin the process of importing credentials.
     */
    fun importCredentials(
        context: Context,
        request: ImportCredentialsRequest,
    ): ProviderImportCredentialsResponse

    @OmitFromCoverage
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Create a new instance of [ProviderEventsManager].
         */
        fun create(context: Context): ProviderEventsManager = StubProviderEventsManager()
    }
}

/**
 * Stub implementation of [ProviderEventsManager].
 */
@OmitFromCoverage
internal class StubProviderEventsManager : ProviderEventsManager {
    override fun registerExport(request: RegisterExportRequest): RegisterExportResponse {
        return RegisterExportResponse()
    }

    override fun importCredentials(
        context: Context,
        request: ImportCredentialsRequest,
    ): ProviderImportCredentialsResponse {
        @SuppressLint("VisibleForTests")
        return ProviderImportCredentialsResponse(
            response = ImportCredentialsResponse(
                responseJson = CANNED_RESPONSE,
            ),
            callingAppInfo = CallingAppInfo(
                packageName = context.packageName,
                signingInfo = SigningInfo(),
                origin = null,
            ),
        )
    }
}

private val CANNED_RESPONSE = """
    {
      "id": "3zGV3pmoSs6mT7IEAPXfOw",
      "username": "",
      "email": "user@email.com",
      "fullName": "Test User",
      "collections": [],
      "items": [
        {
          "id": "8cCs0RV_ViySk7KCACA",
          "creationAt": 1739325421,
          "modifiedAt": 1739325421,
          "title": "test import credentials",
          "favorite": false,
          "scope": {
            "urls": [
              "https://www.sample-url.io/"
            ],
            "androidApps": []
          },
          "credentials": [
            {
              "type": "basic-auth",
              "username": {
                "fieldType": "string",
                "value": "MyUsername"
              }
            },
            {
              "type": "passkey",
              "credentialId": "xMA-5emp0WsQASnuNmuzQA",
              "rpId": "www.sample-url.io",
              "username": "user@email.com",
              "userDisplayName": "user@email.com",
              "userHandle": "lEn2KqNnS7SsUdVbrdoFiw",
              "key": "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgQx8Smx_KdvQ7nJXt2_62Xrn-im9ibCOtsphj_xZo_uWhRANCAARUDaIFJIUaRyUehAy_d1_a-DK63Ws_d-zkYj-uqHdrGZI0dnhazQGva4tJZQFN35iLoLzFFj_CSjqeYAEOX7Ck"
            }
          ]
        }
      ]
    }
"""
    .trimIndent()
