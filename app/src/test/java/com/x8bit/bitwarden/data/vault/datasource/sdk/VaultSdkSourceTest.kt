package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Collection
import com.bitwarden.core.CollectionView
import com.bitwarden.core.DateTime
import com.bitwarden.core.DerivePinKeyResponse
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.bitwarden.core.TotpResponse
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.ClientCrypto
import com.bitwarden.sdk.ClientPasswordHistory
import com.bitwarden.sdk.ClientVault
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultSdkSourceTest {
    private val clientCrypto = mockk<ClientCrypto>()
    private val clientPasswordHistory = mockk<ClientPasswordHistory>()
    private val clientVault = mockk<ClientVault>() {
        every { passwordHistory() } returns clientPasswordHistory
    }
    private val client = mockk<Client>() {
        every { vault() } returns clientVault
        every { crypto() } returns clientCrypto
    }
    private val sdkClientManager = mockk<SdkClientManager> {
        every { getOrCreateClient(any()) } returns client
        every { destroyClient(any()) } just runs
    }
    private val vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        sdkClientManager = sdkClientManager,
    )

    @Test
    fun `clearCrypto should destroy the associated client via the SDK Manager`() {
        val userId = "userId"

        vaultSdkSource.clearCrypto(userId = userId)

        verify { sdkClientManager.destroyClient(userId = userId) }
    }

    @Test
    fun `derivePinKey should call SDK and return a Result with the correct data`() = runBlocking {
        val userId = "userId"
        val pin = "pin"
        val expectedResult = mockk<DerivePinKeyResponse>()
        coEvery {
            clientCrypto.derivePinKey(pin = pin)
        } returns expectedResult
        val result = vaultSdkSource.derivePinKey(
            userId = userId,
            pin = pin,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientCrypto.derivePinKey(pin)
        }
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `derivePinProtectedUserKey should call SDK and return a Result with the correct data`() =
        runBlocking {
            val userId = "userId"
            val encryptedPin = "encryptedPin"
            val expectedResult = "pinProtectedUserKey"
            coEvery {
                clientCrypto.derivePinUserKey(encryptedPin = encryptedPin)
            } returns expectedResult
            val result = vaultSdkSource.derivePinProtectedUserKey(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.derivePinUserKey(encryptedPin = encryptedPin)
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
            } returns Unit
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
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `initializeUserCrypto with BitwardenException failure should return AuthenticationError`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitUserCryptoRequest>()
            val expectedException = BitwardenException.E(message = "")
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
                InitializeCryptoResult.AuthenticationError.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeUserCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
            } returns Unit
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
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `initializeOrgCrypto with BitwardenException failure should return AuthenticationError`() =
        runBlocking {
            val userId = "userId"
            val mockInitCryptoRequest = mockk<InitOrgCryptoRequest>()
            val expectedException = BitwardenException.E(message = "")
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
                InitializeCryptoResult.AuthenticationError.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeOrgCrypto(
                    req = mockInitCryptoRequest,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptCipher should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCipher = mockk<CipherView>()
        val expectedResult = mockk<Cipher>()
        coEvery {
            clientVault.ciphers().encrypt(
                cipherView = mockCipher,
            )
        } returns expectedResult
        val result = vaultSdkSource.encryptCipher(
            userId = userId,
            cipherView = mockCipher,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.ciphers().encrypt(
                cipherView = mockCipher,
            )
        }
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `Cipher decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCipher = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            clientVault.ciphers().decrypt(
                cipher = mockCipher,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipher(
            userId = userId,
            cipher = mockCipher,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.ciphers().decrypt(
                cipher = mockCipher,
            )
        }
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `Cipher decryptListCollection should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val mockCiphers = mockk<List<Cipher>>()
            val expectedResult = mockk<List<CipherListView>>()
            coEvery {
                clientVault.ciphers().decryptList(
                    ciphers = mockCiphers,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptCipherListCollection(
                userId = userId,
                cipherList = mockCiphers,
            )
            assertEquals(
                expectedResult.asSuccess(),
                result,
            )
            coVerify {
                clientVault.ciphers().decryptList(
                    ciphers = mockCiphers,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `Cipher decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val userId = "userId"
        val mockCiphers = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            clientVault.ciphers().decrypt(
                cipher = mockCiphers,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipherList(
            userId = userId,
            cipherList = listOf(mockCiphers),
        )
        assertEquals(
            listOf(expectedResult).asSuccess(),
            result,
        )
        coVerify {
            clientVault.ciphers().decrypt(
                cipher = mockCiphers,
            )
        }
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptSendList should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockSend = mockk<Send>()
            val expectedResult = mockk<SendView>()
            coEvery {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = listOf(mockSend),
            )
            assertEquals(
                listOf(expectedResult).asSuccess(),
                result,
            )
            coVerify {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `encryptSend should call SDK and return correct data wrapped in a Result`() = runBlocking {
        val userId = "userId"
        val expectedResult = mockk<Send>()
        val mockSendView = mockk<SendView>()
        coEvery { clientVault.sends().encrypt(send = mockSendView) } returns expectedResult

        val result = vaultSdkSource.encryptSend(
            userId = userId,
            sendView = mockSendView,
        )

        assertEquals(expectedResult.asSuccess(), result)
        coVerify {
            clientVault.sends().encrypt(send = mockSendView)
        }
    }

    @Test
    fun `decryptSend should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val userId = "userId"
            val mockSend = mockk<Send>()
            val expectedResult = mockk<SendView>()
            coEvery {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptSend(
                userId = userId,
                send = mockSend,
            )
            assertEquals(
                expectedResult.asSuccess(), result,
            )
            coVerify {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
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
        verify { sdkClientManager.getOrCreateClient(userId = userId) }
    }

    @Test
    fun `encryptPasswordHistory should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val mockPasswordHistoryView = mockk<PasswordHistoryView>()
            val expectedResult = mockk<PasswordHistory>()
            coEvery {
                clientPasswordHistory.encrypt(
                    passwordHistory = mockPasswordHistoryView,
                )
            } returns expectedResult

            val result = vaultSdkSource.encryptPasswordHistory(
                userId = userId,
                passwordHistory = mockPasswordHistoryView,
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify {
                clientPasswordHistory.encrypt(
                    passwordHistory = mockPasswordHistoryView,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `decryptPasswordHistoryList should call SDK and return a Result with correct data`() =
        runBlocking {
            val userId = "userId"
            val mockPasswordHistoryList = mockk<List<PasswordHistory>>()
            val expectedResult = mockk<List<PasswordHistoryView>>()
            coEvery {
                clientPasswordHistory.decryptList(
                    list = mockPasswordHistoryList,
                )
            } returns expectedResult

            val result = vaultSdkSource.decryptPasswordHistoryList(
                userId = userId,
                passwordHistoryList = mockPasswordHistoryList,
            )

            assertEquals(expectedResult.asSuccess(), result)
            coVerify {
                clientPasswordHistory.decryptList(
                    list = mockPasswordHistoryList,
                )
            }
            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }

    @Test
    fun `generateTotp should call SDK and return a Result with correct data`() =
        runTest {
            val userId = "userId"
            val totpResponse = TotpResponse("TestCode", 30u)
            coEvery { clientVault.generateTotp(any(), any()) } returns totpResponse

            val time = DateTime.now()
            val result = vaultSdkSource.generateTotp(
                userId = userId,
                totp = "Totp",
                time = time,
            )

            assertEquals(
                Result.success(totpResponse),
                result,
            )
            coVerify {
                clientVault.generateTotp(
                    key = "Totp",
                    time = time,
                )
            }

            verify { sdkClientManager.getOrCreateClient(userId = userId) }
        }
}
