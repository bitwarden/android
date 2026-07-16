package com.bitwarden.cxf.importer

import android.app.Activity
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialExchangeImporterTest {

    private val importer = CredentialExchangeImporterImpl(
        activity = mockk<Activity>(relaxed = true),
    )

    @Test
    fun `importCredentials should return Failure`() = runTest {
        val result = importer.importCredentials(listOf("basic-auth"))

        assertTrue(result is ImportCredentialsSelectionResult.Failure)
    }
}
