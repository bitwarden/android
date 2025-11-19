package com.x8bit.bitwarden.data.credentials.processor

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
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
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

class CredentialProviderProcessorTest {

    private lateinit var credentialProviderProcessor: CredentialProviderProcessor

    private val context: Context = mockk()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns "mockActiveUserId"
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val credentialEntries = listOf(mockk<CredentialEntry>(relaxed = true))
    private val bitwardenCredentialManager: BitwardenCredentialManager = mockk {
        coEvery { getCredentialEntries(any()) } returns credentialEntries.asSuccess()
    }
    private val pendingIntentManager: CredentialManagerPendingIntentManager = mockk()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk()
    private val cancellationSignal: CancellationSignal = mockk()

    private val clock = FIXED_CLOCK

    @BeforeEach
    fun setUp() {
        credentialProviderProcessor = CredentialProviderProcessorImpl(
            context = context,
            authRepository = authRepository,
            bitwardenCredentialManager = bitwardenCredentialManager,
            pendingIntentManager = pendingIntentManager,
            clock = clock,
            biometricsEncryptionManager = biometricsEncryptionManager,
            dispatcherManager = dispatcherManager,
        )

        mockkStatic(::isBuildVersionAtLeast)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
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

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onError(any()) }

        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
        assertEquals("Active user is required.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should invoke callback with error on password create request when userState is null`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<CreateCredentialException>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is CreateCredentialUnknownException)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should invoke callback with result on password create request with valid userState`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        every { context.packageName } returns "com.x8bit.bitwarden"
        every { context.getString(any(), any()) } returns "mockDescription"
        every {
            pendingIntentManager.createPasswordCreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(userId = any())
        } returns mockk<Cipher>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onResult(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        assertEquals(DEFAULT_USER_STATE.accounts.size, captureSlot.captured.createEntries.size)
        val capturedEntry = captureSlot.captured.createEntries[0]
        assertEquals(DEFAULT_USER_STATE.accounts[0].email, capturedEntry.accountName)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should generate correct password entries based on state`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        every { context.packageName } returns "com.x8bit.bitwarden.dev"
        every { context.getString(any(), any()) } returns "mockDescription"
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs
        every {
            pendingIntentManager.createPasswordCreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(any())
        } returns mockk<Cipher>()
        every { isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) } returns true

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

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
            captureSlot.captured
                .createEntries
                .all {
                    it.biometricPromptData?.allowedAuthenticators ==
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                },
        ) { "Expected all entries to have BIOMETRIC_STRONG authenticators." }

        // Verify entries have no biometric prompt data when cipher is null
        every { biometricsEncryptionManager.getOrCreateCipher(any()) } returns null
        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )
        assertTrue(
            captureSlot.captured.createEntries.all { it.biometricPromptData == null },
        ) { "Expected all entries to have null biometric prompt data." }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should not add biometric data to password entries on pre-V devices`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        every { context.packageName } returns "com.x8bit.bitwarden"
        every { context.getString(any(), any()) } returns "mockDescription"
        every {
            pendingIntentManager.createPasswordCreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(userId = any())
        } returns mockk<Cipher>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs
        every { isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) } returns false

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onResult(any()) }

        // Verify entries have no biometric prompt data on older devices
        assertTrue(captureSlot.captured.createEntries.all { it.biometricPromptData == null }) {
            "Expected all entries to have null biometric prompt data on pre-V devices."
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processCreateCredentialRequest should not add biometric data to password entries when vault is locked`() {
        val request: BeginCreatePasswordCredentialRequest = mockk {
            every { callingAppInfo } returns mockk(relaxed = true)
            every { candidateQueryData } returns Bundle()
        }
        val callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException> =
            mockk()
        val captureSlot = slot<BeginCreateCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = DEFAULT_USER_STATE.accounts.map { it.copy(isVaultUnlocked = false) },
        )
        every { context.packageName } returns "com.x8bit.bitwarden"
        every { context.getString(any(), any()) } returns "mockDescription"
        every {
            pendingIntentManager.createPasswordCreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs
        every { isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) } returns true

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onResult(any()) }
        verify(exactly = 0) { biometricsEncryptionManager.getOrCreateCipher(any()) }

        // Verify entries have no biometric prompt data when vault is locked
        assertTrue(captureSlot.captured.createEntries.all { it.biometricPromptData == null }) {
            "Expected all entries to have null biometric prompt data when vault is locked."
        }
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

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

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

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

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
            pendingIntentManager.createFido2CreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(userId = any())
        } returns mockk<Cipher>()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { request.candidateQueryData } returns candidateQueryData
        every {
            candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")
        } returns "{\"mockJsonRequest\":1}"
        every { callback.onResult(capture(captureSlot)) } just runs

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 1) { callback.onResult(any()) }
        verify(exactly = 0) { callback.onError(any()) }

        assertEquals(DEFAULT_USER_STATE.accounts.size, captureSlot.captured.createEntries.size)
        val capturedEntry = captureSlot.captured.createEntries[0]
        assertEquals(DEFAULT_USER_STATE.accounts[0].email, capturedEntry.accountName)
    }

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
            pendingIntentManager.createFido2CreationPendingIntent(
                userId = any(),
            )
        } returns mockIntent
        every {
            biometricsEncryptionManager.getOrCreateCipher(any())
        } returns mockk<Cipher>()
        every { isBuildVersionAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM) } returns true

        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

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
            captureSlot.captured
                .createEntries
                .all {
                    it.biometricPromptData?.allowedAuthenticators ==
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                },
        ) { "Expected all entries to have BIOMETRIC_STRONG authenticators." }

        // Verify entries have no biometric prompt data when cipher is null
        every { biometricsEncryptionManager.getOrCreateCipher(any()) } returns null
        credentialProviderProcessor.processCreateCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )
        assertTrue(
            captureSlot.captured.createEntries.all { it.biometricPromptData == null },
        ) { "Expected all entries to have null biometric prompt data." }
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

        credentialProviderProcessor.processGetCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

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
            pendingIntentManager.createFido2UnlockPendingIntent(
                userId = "mockUserId-1",
            )
        } returns mockIntent

        val expected = AuthenticationAction(
            title = "mockTitle",
            pendingIntent = mockIntent,
        )

        credentialProviderProcessor.processGetCredentialRequest(
            request = request,
            cancellationSignal = cancellationSignal,
            callback = callback,
        )

        verify(exactly = 0) { callback.onError(any()) }
        verify(exactly = 1) {
            callback.onResult(any())
            pendingIntentManager.createFido2UnlockPendingIntent(
                userId = "mockUserId-1",
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
                bitwardenCredentialManager.getCredentialEntries(any())
            } returns Result.failure(Exception("Error decrypting credentials."))

            credentialProviderProcessor.processGetCredentialRequest(
                request = request,
                cancellationSignal = cancellationSignal,
                callback = callback,
            )

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

            credentialProviderProcessor.processGetCredentialRequest(
                request = request,
                cancellationSignal = cancellationSignal,
                callback = callback,
            )

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

@Suppress("SameParameterValue")
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
                isExportable = true,
            ),
        )
    }
    return accounts
}
