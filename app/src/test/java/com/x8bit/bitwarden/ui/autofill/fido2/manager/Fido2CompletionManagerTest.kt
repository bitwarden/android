package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.content.Intent
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Fido2CompletionManagerTest {

    private val mockActivity = mockk<Activity> {
        every { setResult(Activity.RESULT_OK, any()) } just runs
        every { finish() } just runs
    }
    private val fido2CompletionManager = Fido2CompletionManagerImpl(mockActivity)

    @BeforeEach
    fun setUp() {
        mockkConstructor(Intent::class)
        mockkObject(PendingIntentHandler.Companion)
        every {
            PendingIntentHandler.setCreateCredentialException(any(), any())
        } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(Intent::class)
        unmockkObject(PendingIntentHandler.Companion)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Registration should set CreateCredentialResponse, set activity result, then finish activity when result is Success`() {
        fido2CompletionManager
            .completeFido2Registration(
                Fido2RegisterCredentialResult.Success(
                    registrationResponse = "registrationResponse",
                ),
            )

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setCreateCredentialResponse(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Registration should set CreateCredentialException, set activity result, then finish activity when result is Error`() {
        fido2CompletionManager
            .completeFido2Registration(Fido2RegisterCredentialResult.Error)

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setCreateCredentialException(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Registration should set CreateCredentialException, set activity result, then finish activity when result is Canclled`() {
        fido2CompletionManager
            .completeFido2Registration(Fido2RegisterCredentialResult.Cancelled)

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setCreateCredentialException(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Assertion should set GetCredentialResponse, set activity result, then finish activity when result is Success`() {
        fido2CompletionManager
            .completeFido2Assertion(Fido2CredentialAssertionResult.Success("responseJson"))

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setGetCredentialResponse(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Assertion should set GetCredentialException, set activity result, then finish activity when result is Error`() {
        fido2CompletionManager
            .completeFido2Assertion(Fido2CredentialAssertionResult.Error)

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setGetCredentialException(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeFido2Assertion should set cancellation exception, set activity result, then finish activity when result is Cancelled`() {
        fido2CompletionManager
            .completeFido2Assertion(Fido2CredentialAssertionResult.Cancelled)

        verifyActivityResultIsSetAndFinishedAfter {
            PendingIntentHandler.setGetCredentialException(any(), any())
        }
    }

    /**
     * Convenience function to ensure the given [calls] are performed before setting the
     * [mockActivity] result and calling finish. This sequence is expected to be performed for all
     * FIDO 2 operations triggered by [androidx.credentials.CredentialProvider] APIs.
     */
    private fun verifyActivityResultIsSetAndFinishedAfter(
        calls: MockKVerificationScope.() -> Unit,
    ) {
        verify(Ordering.SEQUENCE) {
            calls()
            mockActivity.setResult(Activity.RESULT_OK, any())
            mockActivity.finish()
        }
    }
}
