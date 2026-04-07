package com.x8bit.bitwarden

import android.content.Intent
import androidx.core.os.bundleOf
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.CredentialProviderRequestManager
import com.x8bit.bitwarden.data.credentials.manager.model.CredentialProviderRequest
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.ProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockCreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.createMockGetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.createMockProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.credentials.util.getCreateCredentialRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getGetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getProviderGetPasswordRequestOrNull
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CredentialProviderViewModelTest : BaseViewModelTest() {

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true) {
        every { activeUserId } returns ACTIVE_USER_ID
        every { switchAccount(any()) } returns SwitchAccountResult.NoChange
    }
    private val credentialRequestSlot = slot<CredentialProviderRequest>()
    private val mockCredentialProviderRequestManager = mockk<CredentialProviderRequestManager> {
        every { setPendingCredentialRequest(capture(credentialRequestSlot)) } just runs
    }
    private val bitwardenCredentialManager = mockk<BitwardenCredentialManager> {
        every { isUserVerified } returns false
        every { isUserVerified = any() } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            Intent::getFido2AssertionRequestOrNull,
            Intent::getProviderGetPasswordRequestOrNull,
            Intent::getCreateCredentialRequestOrNull,
            Intent::getGetCredentialsRequestOrNull,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Intent::getFido2AssertionRequestOrNull,
            Intent::getProviderGetPasswordRequestOrNull,
            Intent::getCreateCredentialRequestOrNull,
            Intent::getGetCredentialsRequestOrNull,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with create credential request should set pending request to CreateCredential`() {
        val viewModel = createViewModel()
        val createCredentialRequest = createMockCreateCredentialRequest(
            number = 1,
            isUserPreVerified = false,
        )
        val mockIntent = createMockIntent(
            mockCreateCredentialRequest = createCredentialRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.CreateCredential)
        assertEquals(
            createCredentialRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.CreateCredential).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with create credential request should set user verification based on request`() {
        val viewModel = createViewModel()
        val createCredentialRequest = createMockCreateCredentialRequest(
            number = 1,
            isUserPreVerified = true,
        )
        val mockIntent = createMockIntent(
            mockCreateCredentialRequest = createCredentialRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { bitwardenCredentialManager.isUserVerified = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with create credential request should switch users if active user is not selected`() {
        val viewModel = createViewModel()
        val createCredentialRequest = CreateCredentialRequest(
            userId = "selectedUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )
        val mockIntent = createMockIntent(
            mockCreateCredentialRequest = createCredentialRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify(exactly = 1) {
            mockAuthRepository.switchAccount("selectedUserId")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with create credential request should not switch users if active user is selected`() {
        val viewModel = createViewModel()
        val createCredentialRequest = CreateCredentialRequest(
            userId = ACTIVE_USER_ID,
            isUserPreVerified = false,
            requestData = bundleOf(),
        )
        val mockIntent = createMockIntent(
            mockCreateCredentialRequest = createCredentialRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify(exactly = 0) { mockAuthRepository.switchAccount(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with FIDO2 assertion request should set pending request to Fido2Assertion`() {
        val viewModel = createViewModel()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
        val mockIntent = createMockIntent(
            mockFido2CredentialAssertionRequest = mockAssertionRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.Fido2Assertion)
        assertEquals(
            mockAssertionRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.Fido2Assertion).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with FIDO2 assertion request should set user verification based on request`() {
        val viewModel = createViewModel()
        val mockAssertionRequest = Fido2CredentialAssertionRequest(
            userId = "mockUserId",
            cipherId = "mockCipherId",
            credentialId = "mockCredentialId",
            isUserPreVerified = true,
            requestData = bundleOf(),
        )
        val mockIntent = createMockIntent(
            mockFido2CredentialAssertionRequest = mockAssertionRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { bitwardenCredentialManager.isUserVerified = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with password get request should set pending request to GetPassword`() {
        val viewModel = createViewModel()
        val mockPasswordGetRequest = createMockProviderGetPasswordCredentialRequest(number = 1)
        val mockIntent = createMockIntent(
            mockProviderGetPasswordRequest = mockPasswordGetRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.GetPassword)
        assertEquals(
            mockPasswordGetRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.GetPassword).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with password get request should set user verification based on request`() {
        val viewModel = createViewModel()
        val mockPasswordGetRequest = ProviderGetPasswordCredentialRequest(
            userId = "mockUserId",
            cipherId = "mockCipherId",
            isUserPreVerified = true,
            requestData = bundleOf(),
        )
        val mockIntent = createMockIntent(
            mockProviderGetPasswordRequest = mockPasswordGetRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { bitwardenCredentialManager.isUserVerified = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with get credentials request should set pending request to GetCredentials`() {
        val viewModel = createViewModel()
        val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
        val mockIntent = createMockIntent(
            mockGetCredentialsRequest = mockGetCredentialsRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.GetCredentials)
        assertEquals(
            mockGetCredentialsRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.GetCredentials).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with create credential request should set pending request to CreateCredential`() {
        val viewModel = createViewModel()
        val createCredentialRequest = createMockCreateCredentialRequest(number = 1)
        val mockIntent = createMockIntent(
            mockCreateCredentialRequest = createCredentialRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.CreateCredential)
        assertEquals(
            createCredentialRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.CreateCredential).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with FIDO2 assertion request should set pending request to Fido2Assertion`() {
        val viewModel = createViewModel()
        val mockAssertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
        val mockIntent = createMockIntent(
            mockFido2CredentialAssertionRequest = mockAssertionRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.Fido2Assertion)
        assertEquals(
            mockAssertionRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.Fido2Assertion).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with password get request should set pending request to GetPassword`() {
        val viewModel = createViewModel()
        val mockPasswordGetRequest = createMockProviderGetPasswordCredentialRequest(number = 1)
        val mockIntent = createMockIntent(
            mockProviderGetPasswordRequest = mockPasswordGetRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.GetPassword)
        assertEquals(
            mockPasswordGetRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.GetPassword).request,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with get credentials request should set pending request to GetCredentials`() {
        val viewModel = createViewModel()
        val mockGetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
        val mockIntent = createMockIntent(
            mockGetCredentialsRequest = mockGetCredentialsRequest,
        )

        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(intent = mockIntent))

        verify { mockCredentialProviderRequestManager.setPendingCredentialRequest(any()) }
        assertTrue(credentialRequestSlot.captured is CredentialProviderRequest.GetCredentials)
        assertEquals(
            mockGetCredentialsRequest,
            (credentialRequestSlot.captured as CredentialProviderRequest.GetCredentials).request,
        )
    }

    @Test
    fun `on ReceiveFirstIntent with no credential data should not set pending request`() {
        val viewModel = createViewModel()
        val mockIntent = createMockIntent()

        viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent = mockIntent))

        verify(exactly = 0) {
            mockCredentialProviderRequestManager.setPendingCredentialRequest(any())
        }
    }

    private fun createViewModel() = CredentialProviderViewModel(
        credentialProviderRequestManager = mockCredentialProviderRequestManager,
        authRepository = mockAuthRepository,
        bitwardenCredentialManager = bitwardenCredentialManager,
    )

    @Suppress("LongParameterList")
    private fun createMockIntent(
        mockFido2CredentialAssertionRequest: Fido2CredentialAssertionRequest? = null,
        mockProviderGetPasswordRequest: ProviderGetPasswordCredentialRequest? = null,
        mockCreateCredentialRequest: CreateCredentialRequest? = null,
        mockGetCredentialsRequest: GetCredentialsRequest? = null,
    ): Intent = mockk {
        every { getFido2AssertionRequestOrNull() } returns mockFido2CredentialAssertionRequest
        every { getProviderGetPasswordRequestOrNull() } returns mockProviderGetPasswordRequest
        every { getCreateCredentialRequestOrNull() } returns mockCreateCredentialRequest
        every { getGetCredentialsRequestOrNull() } returns mockGetCredentialsRequest
    }
}

private const val ACTIVE_USER_ID = "activeUserId"
