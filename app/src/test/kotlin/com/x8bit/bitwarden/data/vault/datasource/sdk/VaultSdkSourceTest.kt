package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.DeriveKeyConnectorException
import com.bitwarden.core.DeriveKeyConnectorRequest
import com.bitwarden.core.EnrollPinResponse
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.MasterPasswordAuthenticationData
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.UpdatePasswordResponse
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.CryptoException
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.TrustDeviceResponse
import com.bitwarden.exporters.Account
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Fido2ClientException
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.fido.Origin
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.bitwarden.fido.SilentlyDiscoverCredentialsException
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.AuthClient
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.CiphersClient
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.ClientFido2
import com.bitwarden.sdk.ClientFido2Authenticator
import com.bitwarden.sdk.ClientFido2Client
import com.bitwarden.sdk.CryptoClient
import com.bitwarden.sdk.ExporterClient
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.sdk.PasswordHistoryClient
import com.bitwarden.sdk.PlatformClient
import com.bitwarden.sdk.SendClient
import com.bitwarden.sdk.VaultClient
import com.bitwarden.send.Send
import com.bitwarden.send.SendView
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.DecryptException
import com.bitwarden.vault.EncryptionContext
import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.PasswordHistory
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.DeriveKeyConnectorResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.Fido2CredentialSearchUserInterfaceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

@Suppress("LargeClass")
class VaultSdkSourceTest {
    private val clientAuth = mockk<AuthClient>()
    private val clientCrypto = mockk<CryptoClient>()
    private val fido2 = mockk<ClientFido2Client> {
        coEvery { register(any(), any(), any()) }
    }
    private val clientFido2 = mockk<ClientFido2> {
        every { client(any(), any()) } returns fido2
    }
    private val clientPlatform = mockk<PlatformClient> {
        every { fido2() } returns clientFido2
    }
    private val passwordHistoryClient = mockk<PasswordHistoryClient>()
    private val clientSends = mockk<SendClient>()
    private val ciphersClient = mockk<CiphersClient>()
    private val clientVault = mockk<VaultClient> {
        every { ciphers() } returns ciphersClient
        every { passwordHistory() } returns passwordHistoryClient
    }
    private val clientExporters = mockk<ExporterClient> {
        coEvery { exportVault(any(), any(), any()) }
    }
    private val client = mockk<Client> {
        every { auth() } returns clientAuth
        every { sends() } returns clientSends
        every { vault() } returns clientVault
        every { platform() } returns clientPlatform
        every { crypto() } returns clientCrypto
        every { exporters() } returns clientExporters
    }
    private val sdkClientManager = mockk<SdkClientManager> {
        coEvery { getOrCreateClient(any()) } returns client
        every { destroyClient(any()) } just runs
    }
    private val mockFido2CredentialStore: Fido2CredentialStore = mockk()
    private val fakeDispatcherManager = FakeDispatcherManager()
    private val vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        sdkClientManager = sdkClientManager,
        dispatcherManager = fakeDispatcherManager,
    )

    @Test
    fun `clearCrypto should destroy the associated client via the SDK Manager`() {
        val userId = "userId"

        vaultSdkSource.clearCrypto(userId = userId)

        verify { sdkClientManager.destroyClient(userId = userId) }
    }

    @Test
    fun `getTrustDevice with trustDevice success should return success with correct data`() =
        runBlocking {
            val userId = "userId"
            val expectedResult = mockk<TrustDeviceResponse>()
            coEvery { clientAuth.trustDevice() } returns expectedResult

            val result = vaultSdkSource.getTrustDevice(userId = userId)

            assertEquals(expectedResult.asSuccess(), result)
            coVerify(exactly = 1) {
                clientAuth.trustDevice()
            }
        }

    @Test
    fun `getTrustDevice with trustDevice exception should return a failure`() = runBlocking {
        val userId = "userId"
        val error = Throwable("Fail")
        coEvery { clientAuth.trustDevice() } throws error

        val result = vaultSdkSource.getTrustDevice(userId = userId)

        assertEquals(error.asFailure(), result)
        coVerify(exactly = 1) {
            clientAuth.trustDevice()
        }
    }

    @Test
    fun `deriveKeyConnector should call SDK and return a Result with the correct data`() =
        runBlocking {
            val userId = "userId"
            val userKeyEncrypted = "userKeyEncrypted"
            val email = "email"
            val password = "password"
            val expectedResult = "expectedResult"
            val kdf = mockk<Kdf>()
            coEvery {
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            } returns expectedResult
            val result = vaultSdkSource.deriveKeyConnector(
                userId = userId,
                userKeyEncrypted = userKeyEncrypted,
                email = email,
                password = password,
                kdf = kdf,
            )
            assertEquals(
                DeriveKeyConnectorResult.Success(derivedKey = expectedResult),
                result.getOrNull(),
            )
            coVerify(exactly = 1) {
                sdkClientManager.getOrCreateClient(userId = userId)
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `deriveKeyConnector should call SDK with WrongPassword exception and return a Result with wrong password`() =
        runBlocking {
            val userId = "userId"
            val userKeyEncrypted = "userKeyEncrypted"
            val email = "email"
            val password = "password"
            val kdf = mockk<Kdf>()
            coEvery {
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            } throws BitwardenException.DeriveKeyConnector(
                v1 = DeriveKeyConnectorException.WrongPassword(message = "mock message"),
            )
            val result = vaultSdkSource.deriveKeyConnector(
                userId = userId,
                userKeyEncrypted = userKeyEncrypted,
                email = email,
                password = password,
                kdf = kdf,
            )
            assertEquals(
                DeriveKeyConnectorResult.WrongPasswordError,
                result.getOrNull(),
            )
            coVerify(exactly = 1) {
                sdkClientManager.getOrCreateClient(userId = userId)
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `deriveKeyConnector should call SDK with Crypto exception return and return a Result with error`() =
        runBlocking {
            val userId = "userId"
            val userKeyEncrypted = "userKeyEncrypted"
            val email = "email"
            val password = "password"
            val error = BitwardenException.DeriveKeyConnector(
                v1 = DeriveKeyConnectorException.Crypto(message = "mock message"),
            )
            val kdf = mockk<Kdf>()
            coEvery {
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            } throws error
            val result = vaultSdkSource.deriveKeyConnector(
                userId = userId,
                userKeyEncrypted = userKeyEncrypted,
                email = email,
                password = password,
                kdf = kdf,
            )
            assertEquals(
                DeriveKeyConnectorResult.Error(error = error),
                result.getOrNull(),
            )
            coVerify(exactly = 1) {
                sdkClientManager.getOrCreateClient(userId = userId)
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            }
        }

    @Test
    fun `deriveKeyConnector should call SDK and return a Result with error`() =
        runBlocking {
            val userId = "userId"
            val userKeyEncrypted = "userKeyEncrypted"
            val email = "email"
            val password = "password"
            val error = mockk<BitwardenException> {
                every { message } returns "Other error"
            }
            val kdf = mockk<Kdf>()
            coEvery {
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            } throws error
            val result = vaultSdkSource.deriveKeyConnector(
                userId = userId,
                userKeyEncrypted = userKeyEncrypted,
                email = email,
                password = password,
                kdf = kdf,
            )
            assertEquals(
                DeriveKeyConnectorResult.Error(error = error),
                result.getOrNull(),
            )
            coVerify(exactly = 1) {
                sdkClientManager.getOrCreateClient(userId = userId)
                clientCrypto.deriveKeyConnector(
                    request = DeriveKeyConnectorRequest(
                        userKeyEncrypted = userKeyEncrypted,
                        email = email,
                        password = password,
                        kdf = kdf,
                    ),
                )
            }
        }

    @Test
    fun `enrollPin should call SDK and return a Result with the correct data`() = runBlocking {
        val userId = "userId"
        val pin = "pin"
        val expectedResult = mockk<EnrollPinResponse>()
        coEvery {
            clientCrypto.enrollPin(pin = pin)
        } returns expectedResult
        val result = vaultSdkSource.enrollPin(
            userId = userId,
            pin = pin,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientCrypto.enrollPin(pin)
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `enrollPinWithEncryptedPin should call SDK and return a Result with the correct data`() =
        runBlocking {
            val userId = "userId"
            val encryptedPin = "encryptedPin"
            val expectedResult = mockk<EnrollPinResponse>()
            coEvery {
                clientCrypto.enrollPinWithEncryptedPin(encryptedPin = encryptedPin)
            } returns expectedResult
            val result = vaultSdkSource.enrollPinWithEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.enrollPinWithEncryptedPin(encryptedPin = encryptedPin)
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `validatePin should call SDK and return a Result with the correct data`() =
        runBlocking {
            val userId = "userId"
            val pin = "pin"
            val pinProtectedUserKey = "pinProtectedUserKey"
            val expectedResult = true
            coEvery {
                clientAuth.validatePin(pin = pin, pinProtectedUserKey = pinProtectedUserKey)
            } returns expectedResult

            val result = vaultSdkSource.validatePin(
                userId = userId,
                pin = pin,
                pinProtectedUserKey = pinProtectedUserKey,
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify(exactly = 1) {
                clientAuth.validatePin(pin = pin, pinProtectedUserKey = pinProtectedUserKey)
                sdkClientManager.getOrCreateClient(userId = userId)
            }
        }

    @Test
    fun `getAuthRequestKey should call SDK and return a Result with correct data`() =
        runBlocking {
            val publicKey = "key"
            val userId = "userId"
            val expectedResult = "authRequestKey"
            coEvery {
                clientAuth.approveAuthRequest(publicKey)
            } returns expectedResult
            val result = vaultSdkSource.getAuthRequestKey(
                publicKey = publicKey,
                userId = userId,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientAuth.approveAuthRequest(publicKey)
                sdkClientManager.getOrCreateClient(userId = userId)
            }
        }

    @Test
    fun `getResetPasswordKey should call SDK and return a Result with correct data`() =
        runBlocking {
            val orgPublicKey = "key"
            val userId = "userId"
            val expectedResult = "resetPasswordKey"
            coEvery {
                clientCrypto.enrollAdminPasswordReset(orgPublicKey)
            } returns expectedResult
            val result = vaultSdkSource.getResetPasswordKey(
                orgPublicKey = orgPublicKey,
                userId = userId,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.enrollAdminPasswordReset(orgPublicKey)
                sdkClientManager.getOrCreateClient(userId = userId)
            }
        }

    @Test
    fun `getUserEncryptionKey should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val expectedResult = "userEncryptionKey"
            coEvery {
                clientCrypto.getUserEncryptionKey()
            } returns expectedResult
            val result = vaultSdkSource.getUserEncryptionKey(userId = userId)
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.getUserEncryptionKey()
                sdkClientManager.getOrCreateClient(userId = userId)
            }
        }

    @Test
    fun `getUserFingerprint should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val expectedResult = "fingerprint"
        coEvery {
            clientPlatform.userFingerprint(
                fingerprintMaterial = userId,
            )
        } returns expectedResult

        val result = vaultSdkSource.getUserFingerprint(userId)
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientPlatform.userFingerprint(
                fingerprintMaterial = userId,
            )
        }
    }

    @Test
    fun `initializeUserCrypto with sdk success should return InitializeCryptoResult Success`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitUserCryptoRequest>()
            coEvery {
                clientCrypto.initializeUserCrypto(
                    req = mockInitCryptoRequest,
                )
            } just runs
            val result = vaultSdkSource.initializeCrypto(
                userId = userId,
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult.Success.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeUserCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `initializeUserCrypto with sdk failure should return failure`() = runBlocking {
        val userId = "userId"
        val mockInitCryptoRequest = mockk<InitUserCryptoRequest>()
        val expectedException = IllegalStateException("mock")
        coEvery {
            clientCrypto.initializeUserCrypto(
                req = mockInitCryptoRequest,
            )
        } throws expectedException
        val result = vaultSdkSource.initializeCrypto(
            userId = userId,
            request = mockInitCryptoRequest,
        )
        assertEquals(
            expectedException.asFailure(),
            result,
        )
        coVerify {
            clientCrypto.initializeUserCrypto(
                req = mockInitCryptoRequest,
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `initializeUserCrypto with BitwardenException failure should return AuthenticationError with message`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitUserCryptoRequest>()
            val expectedErrorMessage = "Whoopsy"
            val expectedException = BitwardenException.Crypto(
                CryptoException.InvalidKey(expectedErrorMessage),
            )
            coEvery {
                clientCrypto.initializeUserCrypto(
                    req = mockInitCryptoRequest,
                )
            } throws expectedException
            val result = vaultSdkSource.initializeCrypto(
                userId = userId,
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult
                    .AuthenticationError(
                        message = expectedException.message,
                        error = expectedException,
                    )
                    .asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeUserCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `initializeOrgCrypto with sdk success should return InitializeCryptoResult Success`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitOrgCryptoRequest>()
            coEvery {
                clientCrypto.initializeOrgCrypto(
                    req = mockInitCryptoRequest,
                )
            } just runs
            val result = vaultSdkSource.initializeOrganizationCrypto(
                userId = userId,
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult.Success.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeOrgCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `initializeOrgCrypto with sdk failure should return failure`() = runBlocking {
        val userId = "userId"
        val mockInitCryptoRequest = mockk<InitOrgCryptoRequest>()
        val expectedException = IllegalStateException("mock")
        coEvery {
            clientCrypto.initializeOrgCrypto(
                req = mockInitCryptoRequest,
            )
        } throws expectedException
        val result = vaultSdkSource.initializeOrganizationCrypto(
            userId = userId,
            request = mockInitCryptoRequest,
        )
        assertEquals(
            expectedException.asFailure(),
            result,
        )
        coVerify {
            clientCrypto.initializeOrgCrypto(
                req = mockInitCryptoRequest,
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `initializeOrgCrypto with BitwardenException failure should return AuthenticationError with correct message`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitOrgCryptoRequest>()
            val expectedErrorMessage = "Whoopsy2"
            val expectedException = BitwardenException.Crypto(
                CryptoException.InvalidKey(expectedErrorMessage),
            )
            coEvery {
                clientCrypto.initializeOrgCrypto(
                    req = mockInitCryptoRequest,
                )
            } throws expectedException
            val result = vaultSdkSource.initializeOrganizationCrypto(
                userId = userId,
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult
                    .AuthenticationError(
                        message = expectedException.message,
                        error = expectedException,
                    )
                    .asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeOrgCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptCipher should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCipher = mockk<CipherView>()
        val expectedResult = mockk<EncryptionContext> {
            every { cipher } returns createMockSdkCipher(number = 1)
        }
        coEvery { ciphersClient.encrypt(cipherView = mockCipher) } returns expectedResult
        val result = vaultSdkSource.encryptCipher(
            userId = userId,
            cipherView = mockCipher,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            ciphersClient.encrypt(cipherView = mockCipher)
            sdkClientManager.getOrCreateClient(userId = userId)
        }
    }

    @Test
    fun `Cipher decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCipher = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery { ciphersClient.decrypt(cipher = mockCipher) } returns expectedResult
        val result = vaultSdkSource.decryptCipher(
            userId = userId,
            cipher = mockCipher,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            ciphersClient.decrypt(cipher = mockCipher)
            sdkClientManager.getOrCreateClient(userId = userId)
        }
    }

    @Test
    fun `decryptCollection should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockCollection = mockk<Collection>()
            val expectedResult = mockk<CollectionView>()
            coEvery {
                clientVault.collections().decrypt(
                    collection = mockCollection,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptCollection(
                userId = userId,
                collection = mockCollection,
            )
            assertEquals(
                expectedResult.asSuccess(), result,
            )
            coVerify {
                clientVault.collections().decrypt(
                    collection = mockCollection,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptCollectionList should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockCollectionsList = mockk<List<Collection>>()
            val expectedResult = mockk<List<CollectionView>>()
            coEvery {
                clientVault.collections().decryptList(
                    collections = mockCollectionsList,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptCollectionList(
                userId = userId,
                collectionList = mockCollectionsList,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientVault.collections().decryptList(
                    collections = mockCollectionsList,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptSendList should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockSend1 = mockk<Send>()
            val mockSend2 = mockk<Send>()
            val mockSendView1 = mockk<SendView>()
            val mockSendView2 = mockk<SendView>()
            coEvery { clientSends.decrypt(send = mockSend1) } returns mockSendView1
            coEvery { clientSends.decrypt(send = mockSend2) } returns mockSendView2
            val result = vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = listOf(mockSend1, mockSend2),
            )
            assertEquals(
                listOf(mockSendView1, mockSendView2).asSuccess(),
                result,
            )
            coVerify(exactly = 1) {
                clientSends.decrypt(send = mockSend1)
                clientSends.decrypt(send = mockSend2)
                // It's important that we only fetch the client once
                sdkClientManager.getOrCreateClient(userId = userId)
                client.sends()
            }
        }

    @Test
    fun `encryptAttachment should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val expectedResult = mockk<Attachment>()
            val mockCipher = mockk<Cipher>()
            val mockAttachmentView = mockk<AttachmentView>()
            coEvery {
                clientVault.attachments().encryptFile(
                    cipher = mockCipher,
                    attachment = mockAttachmentView,
                    decryptedFilePath = "",
                    encryptedFilePath = "",
                )
            } returns expectedResult

            val result = vaultSdkSource.encryptAttachment(
                userId = userId,
                cipher = mockCipher,
                attachmentView = mockAttachmentView,
                decryptedFilePath = "",
                encryptedFilePath = "",
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify {
                clientVault.attachments().encryptFile(
                    cipher = mockCipher,
                    attachment = mockAttachmentView,
                    decryptedFilePath = "",
                    encryptedFilePath = "",
                )
            }
        }

    @Test
    fun `encryptSend should call SDK and return correct data wrapped in a Result`() = runBlocking {
        val userId = "userId"
        val expectedResult = mockk<Send>()
        val mockSendView = mockk<SendView>()
        coEvery { clientSends.encrypt(send = mockSendView) } returns expectedResult

        val result = vaultSdkSource.encryptSend(
            userId = userId,
            sendView = mockSendView,
        )

        assertEquals(expectedResult.asSuccess(), result)
        coVerify {
            clientSends.encrypt(send = mockSendView)
        }
    }

    @Test
    fun `decryptSend should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockSend = mockk<Send>()
            val expectedResult = mockk<SendView>()
            coEvery { clientSends.decrypt(send = mockSend) } returns expectedResult
            val result = vaultSdkSource.decryptSend(
                userId = userId,
                send = mockSend,
            )
            assertEquals(
                expectedResult.asSuccess(), result,
            )
            coVerify {
                clientSends.decrypt(send = mockSend)
                sdkClientManager.getOrCreateClient(userId = userId)
            }
        }

    @Test
    fun `encryptFolder should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val expectedResult = mockk<Folder>()
        val mockFolder = mockk<FolderView>()
        coEvery {
            clientVault.folders().encrypt(
                folder = mockFolder,
            )
        } returns expectedResult

        val result = vaultSdkSource.encryptFolder(
            userId = userId,
            folder = mockFolder,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )

        coVerify {
            clientVault.folders().encrypt(
                folder = mockFolder,
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `Folder decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockFolder = mockk<Folder>()
        val expectedResult = mockk<FolderView>()
        coEvery {
            clientVault.folders().decrypt(
                folder = mockFolder,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolder(
            userId = userId,
            folder = mockFolder,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.folders().decrypt(
                folder = mockFolder,
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `Folder decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockFolders = mockk<List<Folder>>()
        val expectedResult = mockk<List<FolderView>>()
        coEvery {
            clientVault.folders().decryptList(
                folders = mockFolders,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolderList(
            userId = userId,
            folderList = mockFolders,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.folders().decryptList(
                folders = mockFolders,
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `File decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCipher = mockk<Cipher>()
        val mockAttachmentView = mockk<AttachmentView>()
        val expectedResult = Unit
        coEvery {
            clientVault.attachments().decryptFile(
                cipher = mockCipher,
                attachment = mockAttachmentView,
                encryptedFilePath = "encrypted_path",
                decryptedFilePath = "decrypted_path",
            )
        } just runs
        val result = vaultSdkSource.decryptFile(
            userId = userId,
            cipher = mockCipher,
            attachmentView = mockAttachmentView,
            encryptedFilePath = "encrypted_path",
            decryptedFilePath = "decrypted_path",
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.attachments().decryptFile(
                cipher = mockCipher,
                attachment = mockAttachmentView,
                encryptedFilePath = "encrypted_path",
                decryptedFilePath = "decrypted_path",
            )
        }
        coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `encryptPasswordHistory should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val mockPasswordHistoryView = mockk<PasswordHistoryView>()
            val expectedResult = mockk<PasswordHistory>()
            coEvery {
                passwordHistoryClient.encrypt(
                    passwordHistory = mockPasswordHistoryView,
                )
            } returns expectedResult

            val result = vaultSdkSource.encryptPasswordHistory(
                userId = userId,
                passwordHistory = mockPasswordHistoryView,
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify {
                passwordHistoryClient.encrypt(
                    passwordHistory = mockPasswordHistoryView,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptPasswordHistoryList should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val mockPasswordHistoryList = mockk<List<PasswordHistory>>()
            val expectedResult = mockk<List<PasswordHistoryView>>()
            coEvery {
                passwordHistoryClient.decryptList(
                    list = mockPasswordHistoryList,
                )
            } returns expectedResult

            val result = vaultSdkSource.decryptPasswordHistoryList(
                userId = userId,
                passwordHistoryList = mockPasswordHistoryList,
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify {
                passwordHistoryClient.decryptList(
                    list = mockPasswordHistoryList,
                )
            }
            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `generateTotpForCipherListView should call SDK and return a Result with correct data`() =
        runTest {
            val userId = "userId"
            val totpResponse = TotpResponse("TestCode", 30u)

            coEvery {
                clientVault.generateTotpCipherView(
                    view = any(),
                    time = any(),
                )
            } returns totpResponse

            val result = vaultSdkSource.generateTotpForCipherListView(
                userId = userId,
                cipherListView = mockk(),
                time = mockk(),
            )

            assertEquals(totpResponse.asSuccess(), result)
            coVerify {
                clientVault.generateTotpCipherView(
                    view = any(),
                    time = any(),
                )
            }

            coVerify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `moveToOrganization should call SDK and a Result with correct data`() = runTest {
        val userId = "userId"
        val organizationId = "organizationId"
        val mockCipher = mockk<CipherView>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            ciphersClient.moveToOrganization(cipher = mockCipher, organizationId = organizationId)
        } returns expectedResult

        val result = vaultSdkSource.moveToOrganization(
            userId = userId,
            organizationId = organizationId,
            cipherView = mockCipher,
        )

        assertEquals(expectedResult.asSuccess(), result)
    }

    @Test
    fun `validatePassword should call SDK and a Result with correct data`() = runTest {
        val userId = "userId"
        val password = "password"
        val passwordHash = "passwordHash"
        coEvery {
            clientAuth.validatePassword(
                password = password,
                passwordHash = passwordHash,
            )
        } returns true

        val result = vaultSdkSource.validatePassword(
            userId = userId,
            password = password,
            passwordHash = passwordHash,
        )
        assertEquals(
            true.asSuccess(),
            result,
        )
    }

    @Test
    fun `validatePasswordUserKey should call SDK and a Result with correct data`() = runTest {
        val userId = "userId"
        val password = "password"
        val encryptedUserKey = "encryptedUserKey"
        val masterPasswordHash = "masterPasswordHash"
        coEvery {
            clientAuth.validatePasswordUserKey(
                password = password,
                encryptedUserKey = encryptedUserKey,
            )
        } returns masterPasswordHash

        val result = vaultSdkSource.validatePasswordUserKey(
            userId = userId,
            password = password,
            encryptedUserKey = encryptedUserKey,
        )
        assertEquals(masterPasswordHash.asSuccess(), result)
    }

    @Test
    fun `updatePassword should call SDK and a Result with correct data`() = runTest {
        val userId = "userId"
        val newPassword = "newPassword"
        val passwordHash = "passwordHash"
        val newKey = "newKey"
        val updatePasswordResponse = UpdatePasswordResponse(
            passwordHash = passwordHash,
            newKey = newKey,
        )
        coEvery {
            clientCrypto.makeUpdatePassword(
                newPassword = newPassword,
            )
        } returns updatePasswordResponse

        val result = vaultSdkSource.updatePassword(
            userId = userId,
            newPassword = newPassword,
        )
        assertEquals(
            updatePasswordResponse.asSuccess(),
            result,
        )
    }

    @Test
    fun `exportVaultDataToString should call SDK and return a Result with the correct data`() =
        runTest {
            val userId = "userId"
            val expected = "TestResult"

            val format = ExportFormat.Json
            val ciphers = listOf(createMockSdkCipher(1))
            val folders = listOf(createMockSdkFolder(1))

            coEvery {
                clientExporters.exportVault(
                    folders = folders,
                    ciphers = ciphers,
                    format = format,
                )
            } returns expected

            val result = vaultSdkSource.exportVaultDataToString(
                userId = userId,
                folders = folders,
                ciphers = ciphers,
                format = ExportFormat.Json,
            )

            coVerify {
                clientExporters.exportVault(
                    folders = folders,
                    ciphers = ciphers,
                    format = format,
                )
            }

            assertEquals(
                expected.asSuccess(),
                result,
            )
        }

    @Test
    fun `exportVaultDataToCxf should call SDK and return a Result with the correct data`() =
        runTest {
            val userId = "userId"
            val account = mockk<Account>()
            val ciphers = listOf(createMockSdkCipher(1))
            val expected = "TestResult"

            coEvery {
                clientExporters.exportCxf(
                    account = account,
                    ciphers = ciphers,
                )
            } returns expected

            val result = vaultSdkSource.exportVaultDataToCxf(
                userId = userId,
                account = account,
                ciphers = ciphers,
            )

            coVerify {
                clientExporters.exportCxf(
                    account = account,
                    ciphers = ciphers,
                )
            }

            assertEquals(
                expected.asSuccess(),
                result,
            )
        }

    @Test
    fun `importCxf should call SDK and return a Result with the correct data`() = runTest {
        val userId = "userId"
        val expected = listOf(createMockSdkCipher(number = 1))
        val cxf = "cxf"

        coEvery {
            clientExporters.importCxf(
                payload = cxf,
            )
        } returns expected

        val result = vaultSdkSource.importCxf(
            userId = userId,
            payload = cxf,
        )

        coVerify {
            clientExporters.importCxf(
                payload = cxf,
            )
        }

        assertEquals(
            expected.asSuccess(),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should return attestation response when registration completes`() =
        runTest {
            mockkStatic(MessageDigest::class) {
                every { MessageDigest.getInstance(any()) } returns mockk<MessageDigest> {
                    every { digest(any()) } returns DEFAULT_SIGNATURE.toByteArray()
                }

                val mockAttestation = mockk<PublicKeyCredentialAuthenticatorAttestationResponse>()

                coEvery { fido2.register(any(), any(), any()) } returns mockAttestation

                val result = vaultSdkSource.registerFido2Credential(
                    DEFAULT_FIDO_2_REGISTER_CREDENTIAL_REQUEST,
                    fido2CredentialStore = mockFido2CredentialStore,
                )

                assertEquals(
                    mockAttestation.asSuccess(),
                    result,
                )
            }
        }

    @Test
    fun `registerFido2Credential should return Failure when BitwardenException is thrown`() =
        runTest {
            coEvery {
                fido2.register(
                    any(),
                    any(),
                    any(),
                )
            } throws BitwardenException.Fido2Client(mockk<Fido2ClientException>())

            val result = vaultSdkSource.registerFido2Credential(
                DEFAULT_FIDO_2_REGISTER_CREDENTIAL_REQUEST,
                fido2CredentialStore = mockFido2CredentialStore,
            )

            assertTrue(result.isFailure)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should return assertion response when registration completes`() =
        runTest {
            mockkStatic(MessageDigest::class) {
                every { MessageDigest.getInstance(any()) } returns mockk<MessageDigest> {
                    every { digest(any()) } returns DEFAULT_SIGNATURE.toByteArray()
                }

                val mockAssertion = mockk<PublicKeyCredentialAuthenticatorAssertionResponse>()

                coEvery { fido2.authenticate(any(), any(), any()) } returns mockAssertion

                val result = vaultSdkSource
                    .authenticateFido2Credential(
                        DEFAULT_FIDO_2_AUTH_REQUEST,
                        fido2CredentialStore = mockFido2CredentialStore,
                    )

                assertEquals(
                    mockAssertion.asSuccess(),
                    result,
                )
            }
        }

    @Test
    fun `authenticateFido2Credential should return Failure when BitwardenException is thrown`() =
        runTest {
            coEvery {
                fido2.authenticate(
                    any(),
                    any(),
                    any(),
                )
            } throws BitwardenException.Fido2Client(mockk<Fido2ClientException>("mockException"))

            val result = vaultSdkSource
                .authenticateFido2Credential(
                    DEFAULT_FIDO_2_AUTH_REQUEST,
                    fido2CredentialStore = mockFido2CredentialStore,
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun `decryptFido2CredentialAutofillViews should return results when successful`() = runTest {
        val mockCipherView = createMockCipherView(number = 1)
        val mockAutofillView = Fido2CredentialAutofillView(
            credentialId = byteArrayOf(0),
            cipherId = "mockCipherId",
            hasCounter = false,
            rpId = "mockRpId",
            userNameForUi = "mockUserNameForUi",
            userHandle = "mockUserHandle".toByteArray(),
        )
        val autofillViews = listOf(mockAutofillView)

        coEvery {
            clientFido2.decryptFido2AutofillCredentials(mockCipherView)
        } returns autofillViews

        val result = vaultSdkSource.decryptFido2CredentialAutofillViews(
            userId = "mockUserId",
            cipherViews = arrayOf(mockCipherView),
        )

        assertEquals(
            autofillViews.asSuccess(),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `decryptFido2CredentialAutofillViews should return Failure when Bitwarden exception is thrown`() =
        runTest {
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                clientFido2.decryptFido2AutofillCredentials(mockCipherView)
            } throws BitwardenException.Fido2Client(mockk<Fido2ClientException>("mockException"))

            val result = vaultSdkSource.decryptFido2CredentialAutofillViews(
                userId = "mockUserId",
                cipherViews = arrayOf(mockCipherView),
            )

            assertTrue(result.isFailure)
        }

    @Test
    fun `silentlyDiscoverCredentials should return results when successful`() = runTest {
        val userId = "userId"
        val fido2CredentialStore: Fido2CredentialStore = mockk()
        val relyingPartyId = "relyingPartyId"
        val userHandle = "mockUserHandle"
        val mockAutofillView = Fido2CredentialAutofillView(
            credentialId = byteArrayOf(0),
            cipherId = "mockCipherId",
            rpId = "mockRpId",
            userNameForUi = "mockUserNameForUi",
            userHandle = userHandle.toByteArray(),
            hasCounter = false,
        )
        val autofillViews = listOf(mockAutofillView)

        val authenticator: ClientFido2Authenticator = mockk {
            coEvery {
                silentlyDiscoverCredentials(
                    relyingPartyId,
                    userHandle.toByteArray(),
                )
            } returns autofillViews
        }
        every {
            clientFido2.authenticator(
                userInterface = any(),
                credentialStore = fido2CredentialStore,
            )
        } returns authenticator

        val result = vaultSdkSource.silentlyDiscoverCredentials(
            userId = userId,
            fido2CredentialStore = fido2CredentialStore,
            relyingPartyId = relyingPartyId,
            userHandle = userHandle,
        )

        assertEquals(
            autofillViews.asSuccess(),
            result,
        )
    }

    @Test
    fun `silentlyDiscoverCredentials should return Failure when Bitwarden exception is thrown`() =
        runTest {
            val userId = "userId"
            val fido2CredentialStore: Fido2CredentialStore = mockk()
            val relyingPartyId = "relyingPartyId"
            val userHandle = "mockUserHandle"

            coEvery {
                clientFido2
                    .authenticator(
                        userInterface = Fido2CredentialSearchUserInterfaceImpl(),
                        credentialStore = fido2CredentialStore,
                    )
                    .silentlyDiscoverCredentials(
                        relyingPartyId,
                        userHandle.toByteArray(),
                    )
            } throws BitwardenException.SilentlyDiscoverCredentials(
                mockk<SilentlyDiscoverCredentialsException>("mockException"),
            )

            val result = vaultSdkSource.silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
                userHandle = userHandle,
            )

            assertTrue(result.isFailure)
        }

    @Test
    fun `decryptCipherListWithFailures should return Success when successful`() = runTest {
        val userId = "userId"
        val mockCipherList = mockk<List<Cipher>>()
        val expectedResult = mockk<DecryptCipherListResult>()

        coEvery {
            clientVault.ciphers().decryptListWithFailures(
                ciphers = mockCipherList,
            )
        } returns expectedResult

        val result = vaultSdkSource.decryptCipherListWithFailures(
            userId = userId,
            cipherList = mockCipherList,
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `decryptCipherListWithFailures should return Failure when Bitwarden exception is thrown`() =
        runTest {
            val userId = "userId"
            val mockCipherList = mockk<List<Cipher>>()
            coEvery {
                clientVault.ciphers().decryptListWithFailures(
                    ciphers = mockCipherList,
                )
            } throws BitwardenException.Decrypt(mockk<DecryptException>("mockException"))
            val result = vaultSdkSource.decryptCipherListWithFailures(
                userId = userId,
                cipherList = mockCipherList,
            )
            assertTrue(result.isFailure)
        }

    @Test
    fun `makeUpdateKdf should return results when successful`() = runTest {
        val kdf = mockk<Kdf>()
        val updateKdfResponse = UpdateKdfResponse(
            masterPasswordAuthenticationData = MasterPasswordAuthenticationData(
                kdf = kdf,
                salt = "mockSalt",
                masterPasswordAuthenticationHash = "mockHash",
            ),
            masterPasswordUnlockData = MasterPasswordUnlockData(
                kdf = kdf,
                masterKeyWrappedUserKey = "mockKey",
                salt = "mockSalt",
            ),
            oldMasterPasswordAuthenticationData = MasterPasswordAuthenticationData(
                kdf = kdf,
                salt = "mockSalt",
                masterPasswordAuthenticationHash = "mockHash",
            ),
        )
        coEvery {
            clientCrypto.makeUpdateKdf(
                password = "mockPassword",
                kdf = kdf,
            )
        } returns updateKdfResponse

        val result = vaultSdkSource.makeUpdateKdf(
            userId = "mockUserId",
            password = "mockPassword",
            kdf = kdf,
        )

        assertEquals(
            updateKdfResponse.asSuccess(),
            result,
        )
    }

    @Test
    fun `makeUpdateKdf should return Failure when Bitwarden exception is thrown`() =
        runTest {
            val kdf = mockk<Kdf>()
            val error = BitwardenException.Crypto(CryptoException.MissingKey("mockException"))
            coEvery {
                clientCrypto.makeUpdateKdf(
                    password = "mockPassword",
                    kdf = kdf,
                )
            } throws error
            val result = vaultSdkSource.makeUpdateKdf(
                userId = "mockUserId",
                password = "mockPassword",
                kdf = kdf,
            )
            assertEquals(error.asFailure(), result)
        }
}

private const val DEFAULT_SIGNATURE = "0987654321ABCDEF"

private val DEFAULT_ORIGIN = Origin.Android(
    UnverifiedAssetLink(
        packageName = "com.x8bit.bitwarden",
        sha256CertFingerprint = "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46",
        host = "bitwarden.com",
        assetLinkUrl = "www.bitwarden.com",
    ),
)
private val DEFAULT_FIDO_2_REGISTER_CREDENTIAL_REQUEST = RegisterFido2CredentialRequest(
    userId = "mockUserId",
    origin = DEFAULT_ORIGIN,
    requestJson = "requestJson",
    clientData = ClientData.DefaultWithCustomHash(
        DEFAULT_SIGNATURE.toByteArray(),
    ),
    isUserVerificationSupported = true,
    selectedCipherView = createMockCipherView(number = 1),
)
private val DEFAULT_FIDO_2_AUTH_REQUEST = AuthenticateFido2CredentialRequest(
    userId = "mockUserId",
    origin = DEFAULT_ORIGIN,
    requestJson = "requestJson",
    clientData = ClientData.DefaultWithCustomHash(
        DEFAULT_SIGNATURE.toByteArray(),
    ),
    isUserVerificationSupported = true,
    selectedCipherView = createMockCipherView(number = 1),
)
