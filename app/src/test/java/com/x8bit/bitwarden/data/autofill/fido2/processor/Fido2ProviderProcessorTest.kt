package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.biometric.BiometricManager
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CredentialEntry
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.base.FakeDispatcherManager
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.Cipher

class Fido2ProviderProcessorTest {

    private lateinit var fido2Processor: Fido2ProviderProcessor

    private val context: Context = mockk()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns "mockActiveUserId"
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val passkeyAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
    private val credentialEntries = listOf(mockk<CredentialEntry>(relaxed = true))
    private val fido2CredentialManager: Fido2CredentialManager = mockk {
        every { getPasskeyAssertionOptionsOrNull(any()) } returns passkeyAssertionOptions
        coEvery {
            getPublicKeyCredentialEntries(any(), any())
        } returns credentialEntries.asSuccess()
    }
    private val intentManager: IntentManager = mockk()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk()
    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(FlagKey.SingleTapPasskeyCreation) } returns false
        every { getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication) } returns false
    }
    private val cancellationSignal: CancellationSignal = mockk()

    private val clock = FIXED_CLOCK

    @BeforeEach
    fun setUp() {
        fido2Processor = Fido2ProviderProcessorImpl(
            context,
            authRepository,
            fido2CredentialManager,
            intentManager,
            clock,
            biometricsEncryptionManager,
            featureFlagManager,
            dispatcherManager,
        )

        mockkStatic(::isBuildVersionBelow)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `processCreateCredentialRequest should invoke callback with error when user id is null`() {
        val request: BeginCreateCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<CreateCredentialException>()
        every { authRepository.activeUserId } returns null
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }

        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
        assertEquals("Active user is required.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should invoke callback with error on password create request`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<CreateCredentialException>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should invoke callback with error when json is null or empty`() {
        val request: BeginCreatePublicKeyCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
        }
        val candidateQueryData: Bundle = mockk()
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<CreateCredentialException>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { request.candidateQueryData } returns candidateQueryData
        every {
            candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        } returns null
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
    }

    @Test
    fun `processCreateCredentialRequest should invoke callback with error when user state null`() {
        val request: BeginCreatePublicKeyCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
        }
        val candidateQueryData: Bundle = mockk()
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<CreateCredentialException>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { request.candidateQueryData } returns candidateQueryData
        every {
            candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        } returns "{\"mockJsonRequest\":1}"
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should invoke callback with result when user state is valid`() {
        val request: BeginCreatePublicKeyCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
        }
        val candidateQueryData: Bundle = mockk()
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        every { context.packageName } returns "com.x8bit.bitwarden"
        every { context.getString(any(), any()) } returns "mockDescription"
        every {
            intentManager.createFido2CreationPendingIntent(
                any(),
                any(),
                any(),
            )
        } returns mockIntent
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { request.candidateQueryData } returns candidateQueryData
        every {
            candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        } returns "{\"mockJsonRequest\":1}"
        every { callback.onResult(capture(captureSlot)) } just runs

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onResult(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        assertEquals(DEFAULT_USER_STATE.accounts.size, captureSlot.captured.createEntries.size)
        val capturedEntry = captureSlot.captured.createEntries[0]
        assertEquals(DEFAULT_USER_STATE.accounts[0].email, capturedEntry.accountName)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should generate correct entries based on state`() {
        val request: BeginCreatePublicKeyCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
        }
        val candidateQueryData: Bundle = mockk()
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        every { context.packageName } returns "com.x8bit.bitwarden.dev"
        every { context.getString(any(), any()) } returns "mockDescription"
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { request.candidateQueryData } returns candidateQueryData
        every {
            candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        } returns "{\"mockJsonRequest\":1}"
        every { callback.onResult(capture(captureSlot)) } just runs
        every {
            intentManager.createFido2CreationPendingIntent(
                any(),
                any(),
                any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(any())
        } returns mockk<Cipher>()
        every { featureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyCreation) } returns true
        every { isBuildVersionBelow(Build.VERSION_CODES.VANILLA_ICE_CREAM) } returns false

        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onResult(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        assertEquals(DEFAULT_USER_STATE.accounts.size, captureSlot.captured.createEntries.size)

        // Verify only the active account entry has a lastUsedTime
        assertEquals(
            1,
            captureSlot.captured.createEntries.filter { it.lastUsedTime != null }.size,
        )
        DEFAULT_USER_STATE.accounts.forEachIndexed { index, mockAccount ->
            assertEquals(mockAccount.email, captureSlot.captured.createEntries[index].accountName)
        }

        // Verify all entries have biometric prompt data when feature flag is enabled
        assertTrue(captureSlot.captured.createEntries.all { it.biometricPromptData != null }) {
            "Expected all entries to have biometric prompt data."
        }

        // Verify entries have the correct authenticators when cipher is not null
        assertTrue(
            captureSlot.captured.createEntries.all {
                it.biometricPromptData?.allowedAuthenticators == BiometricManager.Authenticators.BIOMETRIC_STRONG
            },
        ) { "Expected all entries to have BIOMETRIC_STRONG authenticators." }

        // Verify entries have no biometric prompt data when cipher is null
        every { biometricsEncryptionManager.getOrCreateCipher(any()) } returns null
        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)
        assertTrue(
            captureSlot.captured.createEntries.all { it.biometricPromptData == null },
        ) { "Expected all entries to have null biometric prompt data." }

        // Disable single tap feature flag to verify all entries do not have biometric prompt data
        every { featureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyCreation) } returns false
        fido2Processor.processCreateCredentialRequest(request, cancellationSignal, callback)
        assertTrue(
            captureSlot.captured.createEntries.all { it.biometricPromptData == null },
        ) { "Expected all entries to not have biometric prompt data." }
    }

    @Test
    fun `processGetCredentialRequest should invoke callback with error when user state is null`() {
        val request: BeginGetCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every {
                beginGetCredentialOptions
            } returns listOf(
                mockk<BeginGetPasswordOption> {
                    every { candidateQueryData } returns Bundle()
                },
            )
        }
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<GetCredentialException>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }

        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is GetCredentialUnknownException)
        assertEquals("Active user is required.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with authentication action when vault is locked`() {
        val request: BeginGetCredentialRequest = mockk {
            every { beginGetCredentialOptions } returns listOf(
                mockk<BeginGetPasswordOption> {
                    every { candidateQueryData } returns Bundle()
                },
            )
            every { callingAppInfo } returns mockk(relaxed = true)
        }
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<BeginGetCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_USER_STATE.accounts.first { it.userId == "mockUserId-1" }
                        .copy(isVaultUnlocked = false),
                ),
            )
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs
        every { context.getString(any()) } returns "mockTitle"
        every {
            intentManager.createFido2UnlockPendingIntent(
                action = "com.x8bit.bitwarden.fido2.ACTION_UNLOCK_ACCOUNT",
                userId = "mockUserId-1",
                requestCode = any(),
            )
        } returns mockIntent

        val expected = AuthenticationAction(
            title = "mockTitle",
            pendingIntent = mockIntent,
        )

        fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 1) {
            callback.onResult(any())
            intentManager.createFido2UnlockPendingIntent(
                action = "com.x8bit.bitwarden.fido2.ACTION_UNLOCK_ACCOUNT",
                userId = "mockUserId-1",
                requestCode = any(),
            )
        }

        assertEquals(
            expected.title,
            captureSlot.captured.authenticationActions.first().title,
        )
        assertEquals(
            expected.pendingIntent,
            captureSlot.captured.authenticationActions.first().pendingIntent,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with error when option is not BeginGetPublicKeyCredentialOption`() =
        runTest {
            val request: BeginGetCredentialRequest = mockk {
                every { beginGetCredentialOptions } returns listOf(
                    mockk<BeginGetPasswordOption> {
                        every { candidateQueryData } returns Bundle()
                    },
                )
                every { callingAppInfo } returns mockk(relaxed = true)
            }
            val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> =
                mockk()
            val captureSlot = slot<GetCredentialException>()
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            every { cancellationSignal.setOnCancelListener(any()) } just runs
            every { callback.onError(capture(captureSlot)) } just runs

            fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

            verify(exactly = 1) { callback.onError(any()) }
            verify(exactly = 0) { callback.onResult(any()) }

            assertTrue(captureSlot.captured is GetCredentialUnknownException)
            assertEquals("Unsupported option.", captureSlot.captured.errorMessage)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with error when discovering passkey fails`() =
        runTest {
            val mockOption = BeginGetPublicKeyCredentialOption(
                candidateQueryData = Bundle(),
                id = "",
                requestJson = "{}",
            )
            val request: BeginGetCredentialRequest = mockk {
                every { beginGetCredentialOptions } returns listOf(mockOption)
                every { callingAppInfo } returns mockk(relaxed = true)
            }
            val callback =
                mockk<OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>>()
            val captureSlot = slot<GetCredentialException>()
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            every { cancellationSignal.setOnCancelListener(any()) } just runs
            every { callback.onError(capture(captureSlot)) } just runs
            coEvery {
                fido2CredentialManager.getPublicKeyCredentialEntries(
                userId = DEFAULT_USER_STATE.activeUserId,
                    option = mockOption,
                )
            } returns Result.failure(Exception("Error decrypting credentials."))

            fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

            verify(exactly = 1) { callback.onError(any()) }
            verify(exactly = 0) { callback.onResult(any()) }
            // TODO: [PM-9515] Uncomment when SDK bug is fixed.
            // coVerify(exactly = 1) {
            //    vaultRepository.silentlyDiscoverCredentials(
            //        userId = DEFAULT_USER_STATE.activeUserId,
            //        fido2CredentialStore = fido2CredentialStore,
            //        relyingPartyId = "mockRelyingPartyId-1",
            //    )
            // }

            assertTrue(captureSlot.captured is GetCredentialUnknownException)
            assertEquals("Error decrypting credentials.", captureSlot.captured.errorMessage)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with filtered and discovered passkeys`() =
        runTest {
            val mockOption = BeginGetPublicKeyCredentialOption(
                candidateQueryData = Bundle(),
                id = "",
                requestJson = "{}",
            )
            val request: BeginGetCredentialRequest = mockk {
                every { beginGetCredentialOptions } returns listOf(mockOption)
                every { callingAppInfo } returns mockk(relaxed = true)
            }
            val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> =
                mockk()
            val captureSlot = slot<BeginGetCredentialResponse>()
            mutableUserStateFlow.value = DEFAULT_USER_STATE

            every { cancellationSignal.setOnCancelListener(any()) } just runs
            every { callback.onResult(capture(captureSlot)) } just runs

            fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

            assertEquals(1, captureSlot.captured.credentialEntries.size)
            assertEquals(
                credentialEntries,
                captureSlot.captured.credentialEntries,
            )
        }
}

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "mockUserId-1",
    accounts = createMockAccounts(2),
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private fun createMockAccounts(number: Int): List<UserState.Account> {
    val accounts = mutableListOf<UserState.Account>()
    repeat(number) {
        accounts.add(
            UserState.Account(
                userId = "mockUserId-$it",
                name = null,
                email = "mockEmail-$it",
                avatarColorHex = "$it",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                needsPasswordReset = false,
                needsMasterPassword = false,
                trustedDevice = null,
                organizations = emptyList(),
                isBiometricsEnabled = false,
                vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                hasMasterPassword = true,
                isUsingKeyConnector = false,
                onboardingStatus = OnboardingStatus.COMPLETE,
                firstTimeState = FirstTimeState(showImportLoginsCard = true),
            ),
        )
    }
    return accounts
}
