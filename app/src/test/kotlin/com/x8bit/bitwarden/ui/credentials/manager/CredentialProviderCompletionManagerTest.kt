package com.x8bit.bitwarden.ui.credentials.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPasswordCredentialAutofillCipherLogin
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult
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

class CredentialProviderCompletionManagerTest {

    private val mockActivity = mockk<Activity> {
        every { packageName } returns "packageName"
        every { resources } returns mockk(relaxed = true)
        every { setResult(Activity.RESULT_OK, any()) } just runs
        every { finish() } just runs
    }
    private lateinit var credentialProviderCompletionManager: CredentialProviderCompletionManager

    @Nested
    inner class NoOpImplementation {
        @BeforeEach
        fun setUp() {
            credentialProviderCompletionManager =
                CredentialProviderCompletionManagerUnsupportedApiImpl
        }

        @Test
        fun `completeCredentialRegistration should perform no operations`() {
            val mockRegistrationResult = mockk<CreateCredentialResult>()
            credentialProviderCompletionManager.completeCredentialRegistration(
                mockRegistrationResult,
            )
            verify {
                mockRegistrationResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeFido2Assertion should perform no operations`() {
            val mockAssertionResult = mockk<AssertFido2CredentialResult>()
            credentialProviderCompletionManager.completeFido2Assertion(mockAssertionResult)
            verify {
                mockAssertionResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completePasswordGet should perform no operations`() {
            val mockPasswordGetResult = mockk<GetPasswordCredentialResult>()
            credentialProviderCompletionManager.completePasswordGet(mockPasswordGetResult)
            verify {
                mockPasswordGetResult wasNot Called
                mockActivity wasNot Called
            }
        }

        @Test
        fun `completeProviderGetCredentials should perform no operations`() {
            val mockGetCredentialResult = mockk<GetCredentialsResult>()
            credentialProviderCompletionManager.completeProviderGetCredentialsRequest(
                mockGetCredentialResult,
            )
            verify {
                mockGetCredentialResult wasNot Called
                mockActivity wasNot Called
            }
        }
    }

    @Nested
    inner class DefaultImplementation {

        private val mockPendingIntentManager = mockk<CredentialManagerPendingIntentManager>()

        @BeforeEach
        fun setUp() {
            credentialProviderCompletionManager =
                CredentialProviderCompletionManagerImpl(mockActivity)
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
        fun `completeCredentialRegistration should set CreateCredentialResponse, set activity result, then finish activity when result is SuccessFido2`() {
            credentialProviderCompletionManager
                .completeCredentialRegistration(
                    CreateCredentialResult.Success.Fido2CredentialRegistered(
                        responseJson = "registrationResponse",
                    ),
                )

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setCreateCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeCredentialRegistration should set CreateCredentialResponse, set activity result, then finish activity when result is SuccessPassword`() {
            credentialProviderCompletionManager.completeCredentialRegistration(
                CreateCredentialResult.Success.PasswordCreated,
            )

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setCreateCredentialResponse(
                    intent = any(),
                    response = any<CreatePasswordResponse>(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeCredentialRegistration should set CreateCredentialException, set activity result, then finish activity when result is Error`() {
            credentialProviderCompletionManager
                .completeCredentialRegistration(CreateCredentialResult.Error("".asText()))

            verifyActivityResultIsSetAndFinishedAfter {
                mockActivity.resources
                PendingIntentHandler.setCreateCredentialException(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeCredentialRegistration should set CreateCredentialException, set activity result, then finish activity when result is Cancelled`() {
            credentialProviderCompletionManager
                .completeCredentialRegistration(CreateCredentialResult.Cancelled)

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setCreateCredentialException(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Assertion should set GetCredentialResponse, set activity result, then finish activity when result is Success`() {
            credentialProviderCompletionManager
                .completeFido2Assertion(AssertFido2CredentialResult.Success("responseJson"))

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeFido2Assertion should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            credentialProviderCompletionManager
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
            credentialProviderCompletionManager
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
        fun `completePasswordGet should set GetCredentialResponse, set activity result, then finish activity when result is Success`() {
            credentialProviderCompletionManager
                .completePasswordGet(GetPasswordCredentialResult.Success(createMockLoginView(1)))

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completePasswordGet should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            credentialProviderCompletionManager
                .completePasswordGet(GetPasswordCredentialResult.Error("".asText()))

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
        fun `completePasswordGet should set GetCredentialException, set activity result, then finish activity when result is Cancelled`() {
            credentialProviderCompletionManager
                .completePasswordGet(GetPasswordCredentialResult.Cancelled)
            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setGetCredentialException(
                    any(),
                    any<GetCredentialCancellationException>(),
                )
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeProviderGetCredentials should set BeginGetCredentialResponse, set activity result, then finish activity when result is Success`() {
            credentialProviderCompletionManager
                .completeProviderGetCredentialsRequest(
                    GetCredentialsResult.Success(
                        credentialEntries = emptyList(),
                        userId = "mockUserId",
                    ),
                )

            verifyActivityResultIsSetAndFinishedAfter {
                PendingIntentHandler.setBeginGetCredentialResponse(any(), any())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `completeProviderGetCredentials for FIDO 2 clear authentication actions when result is Success`() {
            mockkConstructor(PublicKeyCredentialEntry.Builder::class)
            mockkStatic(PendingIntent::class)

            val mockCredentialEntry = mockk<PublicKeyCredentialEntry>()
            val mockFido2AutofillView = createMockFido2CredentialAutofillView(number = 1)

            every {
                mockPendingIntentManager.createFido2GetCredentialPendingIntent(
                    userId = "mockUserId",
                    credentialId = mockFido2AutofillView.credentialId.toString(),
                    cipherId = mockFido2AutofillView.cipherId,
                    isUserVerified = false,
                )
            } returns mockk()
            every { mockActivity.getString(any()) } returns "No username"
            every { Icon.createWithResource(mockActivity, any()) } returns mockk<Icon>()

            credentialProviderCompletionManager
                .completeProviderGetCredentialsRequest(
                    GetCredentialsResult.Success(
                        credentialEntries = listOf(mockCredentialEntry),
                        userId = "mockUserId",
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
        fun `completeProviderGetCredentials for Password clear authentication actions when result is Success`() {
            mockkConstructor(PasswordCredentialEntry.Builder::class)
            mockkStatic(PendingIntent::class)

            val mockCredentialEntry = mockk<PasswordCredentialEntry>()
            val mockPasswordAutofillView = createMockPasswordCredentialAutofillCipherLogin()

            every {
                mockPendingIntentManager.createPasswordGetCredentialPendingIntent(
                    userId = "mockUserId",
                    cipherId = mockPasswordAutofillView.cipherId,
                    isUserVerified = false,
                )
            } returns mockk()
            every { mockActivity.getString(any()) } returns "No username"
            every { Icon.createWithResource(mockActivity, any()) } returns mockk<Icon>()

            credentialProviderCompletionManager
                .completeProviderGetCredentialsRequest(
                    GetCredentialsResult.Success(
                        credentialEntries = listOf(mockCredentialEntry),
                        userId = "mockUserId",
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
        fun `completeProviderGetCredentials should set GetCredentialException, set activity result, then finish activity when result is Error`() {
            credentialProviderCompletionManager
                .completeProviderGetCredentialsRequest(
                    GetCredentialsResult.Error("".asText()),
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
