package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.GetFido2CredentialsResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.RegisterFido2CredentialResult
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.Called
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class Fido2CompletionManagerTest {

    private val mockActivity = mockk<Activity> {
        every { packageName } returns "packageName"
        every { resources } returns mockk(relaxed = true)
        every { setResult(Activity.RESULT_OK, any()) } just runs
        every { finish() } just runs
    }
    private lateinit var fido2CompletionManager: Fido2CompletionManager

    @Nested
    inner class NoOpImplementation {
        @BeforeEach
        fun setUp() {
            fido2CompletionManager = Fido2CompletionManagerUnsupportedApiImpl
        }

        @Test
        fun `completeFido2Registration should perform no operations`() {
            val mockRegistrationResult = mockk<RegisterFido2CredentialResult>()
            fido2CompletionManager.completeFido2Registration(mockRegistrationResult)
            verify {
                mockRegistrationResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeFido2Assertion should perform no operations`() {
            val mockAssertionResult = mockk<AssertFido2CredentialResult>()
            fido2CompletionManager.completeFido2Assertion(mockAssertionResult)
            verify {
                mockAssertionResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeFido2GetCredentials should perform no operations`() {
            val mockGetCredentialResult = mockk<GetFido2CredentialsResult>()
            fido2CompletionManager.completeFido2GetCredentialsRequest(mockGetCredentialResult)
            verify {
                mockGetCredentialResult wasNot Called
                mockActivity wasNot Called
            }
        }
    }

    @Nested
    inner class DefaultImplementation {

        private val mockIntentManager = mockk<IntentManager>()

        @BeforeEach
        fun setUp() {
            fido2CompletionManager = Fido2CompletionManagerImpl(mockActivity)
            mockkConstructor(Intent::class)
            mockkObject(PendingIntentHandler.Companion)
            mockkStatic(Icon::class)
            every {
                PendingIntentHandler.setCreateCredentialException(any(), any())
            } just runs
            every {
                PendingIntentHandler.setBeginGetCredentialResponse(any(), any())
            } just runs
        }

        @AfterEach
        fun tearDown() {
            unmockkConstructor(Intent::class, PublicKeyCredentialEntry.Builder::class)
            unmockkObject(PendingIntentHandler.Companion)
            unmockkStatic(PendingIntent::class)
            unmockkStatic(Icon::class)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Registration should set CreateCredentialResponse, set activity result, then finish activity when result is Success`() {
            fido2CompletionManager
                .completeFido2Registration(
                    RegisterFido2CredentialResult.Success(
                        responseJson = "registrationResponse",
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
                .completeFido2Registration(RegisterFido2CredentialResult.Error("".asText()))

            verifyActivityResultIsSetAndFinishedAfter {
                mockActivity.resources
                PendingIntentHandler.setCreateCredentialException(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Registration should set CreateCredentialException, set activity result, then finish activity when result is Cancelled`() {
            fido2CompletionManager
                .completeFido2Registration(RegisterFido2CredentialResult.Cancelled)

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setCreateCredentialException(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Assertion should set GetCredentialResponse, set activity result, then finish activity when result is Success`() {
            fido2CompletionManager
                .completeFido2Assertion(AssertFido2CredentialResult.Success("responseJson"))

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Assertion should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            fido2CompletionManager
                .completeFido2Assertion(AssertFido2CredentialResult.Error("".asText()))

            verifyActivityResultIsSetAndFinishedAfter {
                mockActivity.resources
                PendingIntentHandler.setGetCredentialException(
                    any(),
                    any<GetCredentialUnknownException>(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Assertion should set GetCredentialException, set activity result, then finish activity when result is Cancelled`() {
            fido2CompletionManager
                .completeFido2Assertion(AssertFido2CredentialResult.Cancelled)
            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setGetCredentialException(
                    any(),
                    any<GetCredentialCancellationException>(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2GetCredentials should set BeginGetCredentialResponse, set activity result, then finish activity when result is Success`() {
            fido2CompletionManager
                .completeFido2GetCredentialsRequest(
                    GetFido2CredentialsResult.Success(
                        userId = "mockUserId",
                        credentialEntries = emptyList(),
                        option = mockk(),
                    ),
                )

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setBeginGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2GetCredentials clear authentication actions when result is Success`() {
            mockkConstructor(PublicKeyCredentialEntry.Builder::class)
            mockkStatic(PendingIntent::class)

            val mockCredentialEntry = mockk<PublicKeyCredentialEntry>()
            val mockFido2AutofillView = createMockFido2CredentialAutofillView(number = 1)

            every {
                mockIntentManager.createFido2GetCredentialPendingIntent(
                    action = GET_PASSKEY_INTENT,
                    userId = "mockUserId",
                    credentialId = mockFido2AutofillView.credentialId.toString(),
                    cipherId = mockFido2AutofillView.cipherId,
                    isUserVerified = false,
                    requestCode = any(),
                )
            } returns mockk()
            every { mockActivity.getString(any()) } returns "No username"
            every { Icon.createWithResource(mockActivity, any()) } returns mockk<Icon>()

            fido2CompletionManager
                .completeFido2GetCredentialsRequest(
                    GetFido2CredentialsResult.Success(
                        userId = "mockUserId",
                        credentialEntries = listOf(mockCredentialEntry),
                        option = mockk(),
                    ),
                )

            val responseSlot = slot<BeginGetCredentialResponse>()
            verify {
                PendingIntentHandler.setBeginGetCredentialResponse(
                    intent = any(),
                    response = capture(responseSlot),
                )
            }

            assertEquals(
                listOf(mockCredentialEntry),
                responseSlot.captured.credentialEntries,
            )

            assertTrue(responseSlot.captured.authenticationActions.isEmpty())
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2GetCredentials should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            fido2CompletionManager
                .completeFido2GetCredentialsRequest(
                    GetFido2CredentialsResult.Error("".asText()),
                )
            verifyActivityResultIsSetAndFinishedAfter {
                mockActivity.resources
                PendingIntentHandler.setGetCredentialException(any(), any())
            }
        }

        /**
         * Convenience function to ensure the given [calls] are performed before setting the
         * [mockActivity] result and calling finish. This sequence is expected to be performed for
         * all FIDO 2 operations triggered by [androidx.credentials.CredentialProvider] APIs.
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
}
