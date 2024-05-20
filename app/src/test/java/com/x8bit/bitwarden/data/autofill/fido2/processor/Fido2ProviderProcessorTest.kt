package com.x8bit.bitwarden.data.autofill.fido2.processor

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Fido2ProviderProcessorTest {

    private lateinit var fido2Processor: Fido2ProviderProcessor

    private val context: Context = mockk()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns "mockActiveUserId"
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val intentManager: IntentManager = mockk()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val cancellationSignal: CancellationSignal = mockk()

    @BeforeEach
    fun setUp() {
        fido2Processor = Fido2ProviderProcessorImpl(
            context,
            authRepository,
            intentManager,
            dispatcherManager,
        )
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
        DEFAULT_USER_STATE.accounts.forEachIndexed { index, mockAccount ->
            assertEquals(mockAccount.email, captureSlot.captured.createEntries[index].accountName)
        }
    }
}

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "mockUserId-1",
    accounts = createMockAccounts(2),
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
            ),
        )
    }
    return accounts
}
