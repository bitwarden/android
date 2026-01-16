package com.bitwarden.cxf.importer

import android.app.Activity
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun setUp() {
        mockkConstructor(
            JSONObject::class,
            JSONArray::class,
        )

        every {
            anyConstructed<JSONObject>().put("credentialTypes", any<JSONArray>())
        } returns mockk()

        every {
            anyConstructed<JSONObject>().put("knownExtensions", any<JSONArray>())
        } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(
            JSONObject::class,
            JSONArray::class,
        )
    }

    @Test
    fun `importCredentials should return Success when provider returns valid response`() =
        runTest {
            val mockCallingAppInfo = mockk<CallingAppInfo>()
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = any(),
                    request = any(),
                )
            } returns ProviderImportCredentialsResponse(
                response = ImportCredentialsResponse(
                    responseJson = "mockResponse",
                ),
                callingAppInfo = mockCallingAppInfo,
            )

            val result = importer.importCredentials(listOf("basic-auth"))

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
    fun `importCredentials should return Cancelled when ImportCredentialsCancellationException is thrown`() =
        runTest {
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = any(),
                    request = any(),
                )
            } throws ImportCredentialsCancellationException()

            val result = importer.importCredentials(listOf("basic-auth"))

            assertEquals(ImportCredentialsSelectionResult.Cancelled, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `importCredentials should return Failure with UnknownErrorException when generic Exception is thrown`() =
        runTest {
            coEvery {
                mockProviderEventsManager.importCredentials(
                    context = any(),
                    request = any(),
                )
            } throws RuntimeException("Test exception")

            val result = importer.importCredentials(listOf("basic-auth"))

            assertTrue(result is ImportCredentialsSelectionResult.Failure)
            val failure = result as ImportCredentialsSelectionResult.Failure
            assertTrue(failure.error is ImportCredentialsUnknownErrorException)
        }
}
