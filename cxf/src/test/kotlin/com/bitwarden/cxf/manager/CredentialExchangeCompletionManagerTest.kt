package com.bitwarden.cxf.manager

import android.app.Activity
import android.net.Uri
import androidx.credentials.providerevents.IntentHandler
import androidx.credentials.providerevents.exception.ImportCredentialsException
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CredentialExchangeCompletionManagerTest {

    private val mockActivity = mockk<Activity>()
    private val completionManager = CredentialExchangeCompletionManagerImpl(mockActivity)

    @BeforeEach
    fun setUp() {
        mockkObject(IntentHandler)
        every {
            IntentHandler.setImportCredentialsResponse(
                context = any(),
                uri = any(),
                response = any(),
            )
        } just runs

        every {
            IntentHandler.setImportCredentialsException(
                intent = any(),
                exception = any(),
            )
        } just runs
    }

    @Test
    fun `completeCredentialExport sets Success result and finishes the activity`() {
        val mockUri = mockk<Uri>()
        val exportResult = ExportCredentialsResult.Success("payload", mockUri)

        every { mockActivity.setResult(Activity.RESULT_OK, any()) } just runs
        every { mockActivity.finish() } just runs

        completionManager.completeCredentialExport(exportResult)

        verify(ordering = Ordering.ORDERED) {
            IntentHandler.setImportCredentialsResponse(
                context = mockActivity,
                uri = mockUri,
                response = any(),
            )
            mockActivity.setResult(Activity.RESULT_OK, any())
            mockActivity.finish()
        }
    }

    @Test
    fun `completeCredentialExport sets Failure result and finishes the activity`() {
        val importException = mockk<ImportCredentialsException>()
        val exportResult = ExportCredentialsResult.Failure(error = importException)

        every { mockActivity.setResult(Activity.RESULT_OK, any()) } just runs
        every { mockActivity.finish() } just runs

        completionManager.completeCredentialExport(exportResult)

        verify(ordering = Ordering.ORDERED) {
            IntentHandler.setImportCredentialsException(
                intent = any(),
                exception = importException,
            )
            mockActivity.setResult(Activity.RESULT_OK, any())
            mockActivity.finish()
        }
    }
}
