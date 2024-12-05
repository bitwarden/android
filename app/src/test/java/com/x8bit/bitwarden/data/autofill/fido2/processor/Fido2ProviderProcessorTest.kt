package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class Fido2ProviderProcessorTest {

    private lateinit var fido2Processor: Fido2ProviderProcessor

    private val context: Context = mockk()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val mutableCiphersStateFlow = MutableStateFlow<DataState<List<CipherView>>>(
        DataState.Loaded(emptyList()),
    )
    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns "mockActiveUserId"
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepository: VaultRepository = mockk {
        every { ciphersStateFlow } returns mutableCiphersStateFlow
    }
    private val passkeyAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
    private val fido2CredentialManager: Fido2CredentialManager = mockk {
        every { getPasskeyAssertionOptionsOrNull(any()) } returns passkeyAssertionOptions
    }
    private val fido2CredentialStore: Fido2CredentialStore = mockk()
    private val intentManager: IntentManager = mockk()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val cancellationSignal: CancellationSignal = mockk()

    private val json = PlatformNetworkModule.providesJson()
    private val clock = FIXED_CLOCK

    @BeforeEach
    fun setUp() {
        fido2Processor = Fido2ProviderProcessorImpl(
            context,
            authRepository,
            vaultRepository,
            fido2CredentialStore,
            fido2CredentialManager,
            intentManager,
            clock,
            dispatcherManager,
        )

        mockkStatic(Icon::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Icon::class)
    }

    @Test
    fun `processCreateCredentialRequest should invoke callback with error when user id is null`() {
        val request: BeginCreateCredentialRequest = mockk()
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
        val request: BeginCreatePasswordCredentialRequest = mockk()
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
        val request: BeginCreatePublicKeyCredentialRequest = mockk()
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
        val request: BeginCreatePublicKeyCredentialRequest = mockk()
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
        val request: BeginCreatePublicKeyCredentialRequest = mockk()
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

    @Test
    fun `processCreateCredentialRequest should generate result entries for each user account`() {
        val request: BeginCreatePublicKeyCredentialRequest = mockk()
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
    }

    @Test
    fun `processGetCredentialRequest should invoke callback with error when user state is null`() {
        val request: BeginGetCredentialRequest = mockk()
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
        val request: BeginGetCredentialRequest = mockk()
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<BeginGetCredentialResponse>()
        val mockIntent: PendingIntent = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
            .copy(
                accounts = listOf(
                    DEFAULT_USER_STATE
                        .accounts
                        .first { it.userId == "mockUserId-1" }
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
    fun `processGetCredentialRequest should invoke callback with error when option is not BeginGetPublicKeyCredentialOption`() {
        val request: BeginGetCredentialRequest = mockk {
            every { beginGetCredentialOptions } returns listOf(mockk<BeginGetPasswordOption>())
        }
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<GetCredentialException>()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is GetCredentialUnsupportedException)
        assertEquals("Unsupported option.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with error when option does not contain valid request json`() {
        val mockOption = BeginGetPublicKeyCredentialOption(
            candidateQueryData = Bundle(),
            id = "",
            requestJson = json.encodeToString(passkeyAssertionOptions),
        )
        val request: BeginGetCredentialRequest = mockk {
            every { beginGetCredentialOptions } returns listOf(mockOption)
        }
        every {
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(any())
        } returns null
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<GetCredentialException>()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs

        fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 1) { callback.onError(any()) }
        verify(exactly = 0) { callback.onResult(any()) }

        assert(captureSlot.captured is GetCredentialUnknownException)
        assertEquals("Invalid data.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with error when discovering passkey fails`() {
        val mockOption = BeginGetPublicKeyCredentialOption(
            candidateQueryData = Bundle(),
            id = "",
            requestJson = json.encodeToString(passkeyAssertionOptions),
        )
        val request: BeginGetCredentialRequest = mockk {
            every { beginGetCredentialOptions } returns listOf(mockOption)
        }
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<GetCredentialException>()
        val mockCipherViews = listOf(createMockCipherView(number = 1))
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        mutableCiphersStateFlow.value = DataState.Loaded(mockCipherViews)
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onError(capture(captureSlot)) } just runs
        coEvery {
            vaultRepository.getDecryptedFido2CredentialAutofillViews(any())
        } returns DecryptFido2CredentialAutofillViewResult.Error
        coEvery {
            vaultRepository.silentlyDiscoverCredentials(
                userId = DEFAULT_USER_STATE.activeUserId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = "mockRelyingPartyId-1",
            )
        } returns Throwable().asFailure()

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

        assert(captureSlot.captured is GetCredentialUnknownException)
        assertEquals("Error decrypting credentials.", captureSlot.captured.errorMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processGetCredentialRequest should invoke callback with filtered and discovered passkeys`() {
        val mockOption = BeginGetPublicKeyCredentialOption(
            candidateQueryData = Bundle(),
            id = "",
            requestJson = json.encodeToString(passkeyAssertionOptions),
        )
        val request: BeginGetCredentialRequest = mockk {
            every { beginGetCredentialOptions } returns listOf(mockOption)
        }
        val callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException> = mockk()
        val captureSlot = slot<BeginGetCredentialResponse>()
        val mockCipherViews = listOf(createMockCipherView(number = 1))
        val mockFido2CredentialAutofillViews = listOf(
            createMockFido2CredentialAutofillView(number = 1),
        )
        val mockIntent: PendingIntent = mockk()
        val mockPublicKeyCredentialEntry: PublicKeyCredentialEntry = mockk()
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        mutableCiphersStateFlow.value = DataState.Loaded(mockCipherViews)
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every { callback.onResult(capture(captureSlot)) } just runs
        coEvery {
            vaultRepository.silentlyDiscoverCredentials(
                userId = DEFAULT_USER_STATE.activeUserId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = "mockRelyingPartyId-1",
            )
        } returns mockFido2CredentialAutofillViews.asSuccess()
        coEvery {
            vaultRepository.getDecryptedFido2CredentialAutofillViews(any())
        } returns DecryptFido2CredentialAutofillViewResult.Success(mockFido2CredentialAutofillViews)
        every {
            intentManager.createFido2GetCredentialPendingIntent(
                action = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY",
                userId = DEFAULT_USER_STATE.activeUserId,
                credentialId = mockFido2CredentialAutofillViews.first().credentialId.toString(),
                cipherId = mockFido2CredentialAutofillViews.first().cipherId,
                requestCode = any(),
            )
        } returns mockIntent

        mockkConstructor(PublicKeyCredentialEntry.Builder::class)
        every {
            anyConstructed<PublicKeyCredentialEntry.Builder>().build()
        } returns mockPublicKeyCredentialEntry
        every { Icon.createWithResource(context, any()) } returns mockk<Icon>()

        fido2Processor.processGetCredentialRequest(request, cancellationSignal, callback)

        verify(exactly = 0) { callback.onError(any()) }
        // TODO: [PM-9515] Uncomment when SDK bug is fixed.
        // verify(exactly = 1) {
        //    callback.onResult(any())
        //    intentManager.createFido2GetCredentialPendingIntent(
        //        action = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY",
        //        credentialId = mockFido2CredentialAutofillViews.first().credentialId.toString(),
        //        cipherId = mockFido2CredentialAutofillViews.first().cipherId,
        //        requestCode = any(),
        //    )
        // }
        // coVerify(exactly = 1) {
        //    vaultRepository.silentlyDiscoverCredentials(
        //        userId = DEFAULT_USER_STATE.activeUserId,
        //        fido2CredentialStore = fido2CredentialStore,
        //        relyingPartyId = "mockRelyingPartyId-1",
        //    )
        // }

        assertEquals(1, captureSlot.captured.credentialEntries.size)
        assertEquals(mockPublicKeyCredentialEntry, captureSlot.captured.credentialEntries.first())

        unmockkConstructor(PublicKeyCredentialEntry.Builder::class)
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
