package com.x8bit.bitwarden.ui.autofill.fido2.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
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
            val mockRegistrationResult = mockk<Fido2RegisterCredentialResult>()
            fido2CompletionManager.completeFido2Registration(mockRegistrationResult)
            verify {
                mockRegistrationResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeFido2Assertion should perform no operations`() {
            val mockAssertionResult = mockk<Fido2CredentialAssertionResult>()
            fido2CompletionManager.completeFido2Assertion(mockAssertionResult)
            verify {
                mockAssertionResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeFido2GetCredentials should perform no operations`() {
            val mockGetCredentialResult = mockk<Fido2GetCredentialsResult>()
            fido2CompletionManager.completeFido2GetCredentialRequest(mockGetCredentialResult)
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
            fido2CompletionManager = Fido2CompletionManagerImpl(mockActivity, mockIntentManager)
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
        fun `completeFido2Registration should set CreateCredentialException, set activity result, then finish activity when result is Cancelled`() {
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
        fun `completeFido2GetCredentials should set BeginGetCredentialResponse, set activity result, then finish activity when result is Success`() {
            fido2CompletionManager
                .completeFido2GetCredentialRequest(
                    Fido2GetCredentialsResult.Success(
                        userId = "mockUserId",
                        options = mockk(),
                        credentials = emptyList(),
                    ),
                )

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setBeginGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2GetCredentials should create a PublicKeyCredentialEntry and clear authentication actions when result is Success`() {
            mockkConstructor(PublicKeyCredentialEntry.Builder::class)
            mockkStatic(PendingIntent::class)

            val mockCredentialEntry = mockk<PublicKeyCredentialEntry>()
            val mockFido2AutofillView = createMockFido2CredentialAutofillView(number = 1)
            val mockFido2AutofillViewList = listOf(mockFido2AutofillView)

            every {
                anyConstructed<PublicKeyCredentialEntry.Builder>().build()
            } returns mockCredentialEntry
            every {
                mockIntentManager.createFido2GetCredentialPendingIntent(
                    action = GET_PASSKEY_INTENT,
                    userId = "mockUserId",
                    credentialId = mockFido2AutofillView.credentialId.toString(),
                    cipherId = mockFido2AutofillView.cipherId,
                    requestCode = any(),
                )
            } returns mockk()
            every { mockActivity.getString(any()) } returns "No username"
            every { Icon.createWithResource(mockActivity, any()) } returns mockk<Icon>()

            fido2CompletionManager
                .completeFido2GetCredentialRequest(
                    Fido2GetCredentialsResult.Success(
                        userId = "mockUserId",
                        options = mockk(),
                        credentials = mockFido2AutofillViewList,
                    ),
                )

            val responseSlot = slot<BeginGetCredentialResponse>()
            verify {
                anyConstructed<PublicKeyCredentialEntry.Builder>().build()
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
        fun `completeFido2GetCredentials should set username to default value when userNameForUi is null`() {
            mockkConstructor(PublicKeyCredentialEntry.Builder::class)
            mockkStatic(PendingIntent::class)
            val mockCredentialEntry = mockk<PublicKeyCredentialEntry>()
            val mockFido2AutofillView = createMockFido2CredentialAutofillView(number = 1)
                .copy(userNameForUi = null)
            val mockFido2AutofillViewList = listOf(mockFido2AutofillView)

            every {
                anyConstructed<PublicKeyCredentialEntry.Builder>().build()
            } returns mockCredentialEntry
            every {
                mockIntentManager.createFido2GetCredentialPendingIntent(
                    action = GET_PASSKEY_INTENT,
                    userId = "mockUserId",
                    credentialId = mockFido2AutofillView.credentialId.toString(),
                    cipherId = mockFido2AutofillView.cipherId,
                    requestCode = any(),
                )
            } returns mockk()
            every { mockActivity.getString(any()) } returns "No Username"
            every { Icon.createWithResource(mockActivity, any()) } returns mockk<Icon>()

            fido2CompletionManager
                .completeFido2GetCredentialRequest(
                    Fido2GetCredentialsResult.Success(
                        userId = "mockUserId",
                        options = mockk(),
                        credentials = mockFido2AutofillViewList,
                    ),
                )

            val responseSlot = slot<BeginGetCredentialResponse>()
            verify {
                mockActivity.getString(R.string.no_username)
                anyConstructed<PublicKeyCredentialEntry.Builder>().build()
                PendingIntentHandler.setBeginGetCredentialResponse(
                    intent = any(),
                    response = capture(responseSlot),
                )
            }

            assertEquals(
                listOf(mockCredentialEntry),
                responseSlot.captured.credentialEntries,
            )
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2GetCredentials should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            fido2CompletionManager
                .completeFido2GetCredentialRequest(Fido2GetCredentialsResult.Error)
            verifyActivityResultIsSetAndFinishedAfter {
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
