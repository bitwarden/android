package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

class Fido2CredentialStoreImplTest {

    private val mutableDecryptCipherListResultStateFlow =
        MutableStateFlow<DataState<DecryptCipherListResult>>(DataState.Loading)
    private val authRepository: AuthRepository = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val vaultRepository: VaultRepository = mockk {
        every { decryptCipherListResultStateFlow } returns mutableDecryptCipherListResultStateFlow
    }

    private val fido2CredentialStore = Fido2CredentialStoreImpl(
        authRepository = authRepository,
        vaultSdkSource = vaultSdkSource,
        vaultRepository = vaultRepository,
    )

    @BeforeEach
    fun setUp() {
        mutableDecryptCipherListResultStateFlow.value = DataState.Loaded(
            data = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createFido2CipherListView(number = 1, credentialId = UUID_CREDENTIAL_ID),
                    createFido2CipherListView(number = 2, credentialId = B64_CREDENTIAL_ID),
                ),
            ),
        )
        coEvery { vaultRepository.getCipher(cipherId = any()) } answers {
            GetCipherResult.Success(
                cipherView = createMockCipherView(number = 1).copy(id = firstArg()),
            )
        }
    }

    @Test
    fun `findCredentials should match UUID credential id against fresh raw bytes`() = runTest {
        val result = fido2CredentialStore.findCredentials(
            ids = listOf(UUID.fromString(UUID_CREDENTIAL_ID).toRawBytes()),
            ripId = RP_ID,
            userHandle = null,
        )

        assertEquals(listOf("mockId-1"), result.map { it.id })
    }

    @Test
    fun `findCredentials should exclude ciphers with non-matching credential ids`() = runTest {
        val result = fido2CredentialStore.findCredentials(
            ids = listOf(UUID.randomUUID().toRawBytes()),
            ripId = RP_ID,
            userHandle = null,
        )

        assertEquals(emptyList<String>(), result.map { it.id })
    }

    @Test
    fun `findCredentials should match b64 credential id against decoded bytes`() = runTest {
        val result = fido2CredentialStore.findCredentials(
            ids = listOf(B64_CREDENTIAL_BYTES.copyOf()),
            ripId = RP_ID,
            userHandle = null,
        )

        assertEquals(listOf("mockId-2"), result.map { it.id })
    }

    @Test
    fun `findCredentials should match excludeCredentials ids during registration`() = runTest {
        // Registration passes the exclude list with the new user's handle; a match must be found
        // so the authenticator reports the credential as already registered.
        val result = fido2CredentialStore.findCredentials(
            ids = listOf(UUID.fromString(UUID_CREDENTIAL_ID).toRawBytes()),
            ripId = RP_ID,
            userHandle = byteArrayOf(1, 2, 3),
        )

        assertEquals(listOf("mockId-1"), result.map { it.id })
    }

    @Test
    fun `findCredentials should skip credential id filtering when ids are null`() = runTest {
        val result = fido2CredentialStore.findCredentials(
            ids = null,
            ripId = RP_ID,
            userHandle = null,
        )

        assertEquals(listOf("mockId-1", "mockId-2"), result.map { it.id })
    }

    @Test
    fun `findCredentials should skip credential id filtering when ids are empty`() = runTest {
        val result = fido2CredentialStore.findCredentials(
            ids = emptyList(),
            ripId = RP_ID,
            userHandle = null,
        )

        assertEquals(listOf("mockId-1", "mockId-2"), result.map { it.id })
    }
}

private const val RP_ID = "mockRpId"
private const val UUID_CREDENTIAL_ID = "3c5d6a1e-8f2b-4c7d-9e0a-1b2c3d4e5f60"
private val B64_CREDENTIAL_BYTES = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
private val B64_CREDENTIAL_ID =
    "b64." + Base64.getUrlEncoder().withoutPadding().encodeToString(B64_CREDENTIAL_BYTES)

private fun UUID.toRawBytes(): ByteArray = ByteBuffer
    .allocate(16)
    .putLong(mostSignificantBits)
    .putLong(leastSignificantBits)
    .array()

private fun createFido2CipherListView(
    number: Int,
    credentialId: String,
) = createMockCipherListView(
    number = number,
    type = CipherListViewType.Login(
        createMockLoginListView(
            number = number,
            hasFido2 = true,
            fido2Credentials = listOf(
                createMockFido2CredentialListView(
                    number = number,
                    credentialId = credentialId,
                    rpId = RP_ID,
                ),
            ),
        ),
    ),
)
