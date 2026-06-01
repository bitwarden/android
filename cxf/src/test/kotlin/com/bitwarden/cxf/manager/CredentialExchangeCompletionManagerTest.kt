package com.bitwarden.cxf.manager

import android.app.Activity
import android.content.pm.ApplicationInfo
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CredentialExchangeCompletionManagerTest {

    private val mockActivity = mockk<Activity>(relaxed = true, relaxUnitFun = true)
    private val mockApplicationInfo = mockk<ApplicationInfo>(relaxed = true)

    private val completionManager = CredentialExchangeCompletionManagerImpl(
        activity = mockActivity,
        clock = FIXED_CLOCK,
    )

    @BeforeEach
    fun setUp() {
        mockkObject(IntentHandler)
        every { mockActivity.packageName } returns "mockPackageName-1"
        every { mockActivity.applicationInfo } returns mockApplicationInfo
        every {
            IntentHandler.setImportCredentialsResponse(
                context = any(),
                uri = any(),
                response = any(),
                intent = any(),
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
                intent = any(),
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

private val FIXED_CLOCK = Clock.fixed(
    Instant.parse("2024-01-25T10:15:30.00Z"),
    ZoneOffset.UTC,
)
