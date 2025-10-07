package com.bitwarden.cxf.importer

import android.app.Activity
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CredentialExchangeImporterTest {

    private val mockActivity = mockk<Activity>(relaxed = true) {
        every { packageName } returns "mockPackageName"
    }
    private val mockProviderEventsManager = mockk<ProviderEventsManager>()
    private val importer = CredentialExchangeImporterImpl(
        activity = mockActivity,
        providerEventsManager = mockProviderEventsManager,
    )

    @Test
    fun `importCredentials should construct request correctly and return a success result`() =
        runTest {
            val mockCallingAppInfo = mockk<CallingAppInfo>()
            val capturedRequestJson = mutableListOf<ImportCredentialsRequest>()
            val expectedRequestJson = """
        {
          "version": {
            "major":0,
            "minor":0
          },
          "mode": ["direct"],
          "importerRpId": "mockPackageName",
          "importerDisplayName": "null",
          "credentialTypes": [
            "basic-auth"
          ]
        }
        """
                .trimIndent()
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = mockActivity,
                    request = capture(capturedRequestJson),
                )
            } returns ProviderImportCredentialsResponse(
                response = ImportCredentialsResponse(
                    responseJson = "mockResponse",
                ),
                callingAppInfo = mockCallingAppInfo,
            )

            val result = importer.importCredentials(listOf("basic-auth"))
            assertEquals(
                expectedRequestJson,
                capturedRequestJson.firstOrNull()?.requestJson,
            )
            assertEquals(
                ImportCredentialsSelectionResult.Success(
                    response = "mockResponse",
                    callingAppInfo = mockCallingAppInfo,
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `importCredentials should return ImportCredentialsSelectionResult Cancelled when ImportCredentialsCancellationException is thrown`() =
        runTest {
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = mockActivity,
                    request = any(),
                )
            } throws ImportCredentialsCancellationException()

            assertEquals(
                ImportCredentialsSelectionResult.Cancelled,
                importer.importCredentials(listOf("basic-auth")),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `importCredentials should return ImportCredentialsSelectionResult Failure when ImportCredentialsException is thrown`() =
        runTest {
            val importException = mockk<ImportCredentialsException>()
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = mockActivity,
                    request = any(),
                )
            } throws importException

            val result = importer.importCredentials(listOf("basic-auth"))

            assertEquals(
                ImportCredentialsSelectionResult.Failure(error = importException),
                result,
            )
        }
}
