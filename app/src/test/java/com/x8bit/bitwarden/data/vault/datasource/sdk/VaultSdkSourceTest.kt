package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitCryptoRequest
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.ClientCrypto
import com.bitwarden.sdk.ClientVault
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.IllegalStateException

class VaultSdkSourceTest {
    private val clientVault = mockk<ClientVault>()
    private val clientCrypto = mockk<ClientCrypto>()
    private val vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        clientVault = clientVault,
        clientCrypto = clientCrypto,
    )

    @Test
    fun `initializeCrypto with sdk success should return InitializeCryptoResult Success`() =
        runBlocking {
            val mockInitCryptoRequest = mockk<InitCryptoRequest>()
            coEvery {
                clientCrypto.initializeCrypto(
                    req = mockInitCryptoRequest,
                )
            } returns Unit
            val result = vaultSdkSource.initializeCrypto(
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult.Success.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeCrypto(
                    req = mockInitCryptoRequest,
                )
            }
        }

    @Test
    fun `initializeCrypto with sdk failure should return failure`() = runBlocking {
        val mockInitCryptoRequest = mockk<InitCryptoRequest>()
        val expectedException = IllegalStateException("mock")
        coEvery {
            clientCrypto.initializeCrypto(
                req = mockInitCryptoRequest,
            )
        } throws expectedException
        val result = vaultSdkSource.initializeCrypto(
            request = mockInitCryptoRequest,
        )
        assertEquals(
            expectedException.asFailure(),
            result,
        )
        coVerify {
            clientCrypto.initializeCrypto(
                req = mockInitCryptoRequest,
            )
        }
    }

    @Test
    fun `initializeCrypto with BitwardenException failure should return AuthenticationError`() =
        runBlocking {
            val mockInitCryptoRequest = mockk<InitCryptoRequest>()
            val expectedException = BitwardenException.E(message = "")
            coEvery {
                clientCrypto.initializeCrypto(
                    req = mockInitCryptoRequest,
                )
            } throws expectedException
            val result = vaultSdkSource.initializeCrypto(
                request = mockInitCryptoRequest,
            )
            assertEquals(
                InitializeCryptoResult.AuthenticationError.asSuccess(),
                result,
            )
            coVerify {
                clientCrypto.initializeCrypto(
                    req = mockInitCryptoRequest,
                )
            }
        }

    @Test
    fun `Cipher decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val mockCipher = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            clientVault.ciphers().decrypt(
                cipher = mockCipher,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipher(
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
    }

    @Test
    fun `Cipher decryptListCollection should call SDK and return a Result with correct data`() =
        runBlocking {
            val mockCiphers = mockk<List<Cipher>>()
            val expectedResult = mockk<List<CipherListView>>()
            coEvery {
                clientVault.ciphers().decryptList(
                    ciphers = mockCiphers,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptCipherListCollection(
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
        }

    @Test
    fun `Cipher decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val mockCiphers = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            clientVault.ciphers().decrypt(
                cipher = mockCiphers,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipherList(
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
    }

    @Test
    fun `decryptSendList should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val mockSend = mockk<Send>()
            val expectedResult = mockk<SendView>()
            coEvery {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptSendList(
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
        }

    @Test
    fun `decryptSend should call SDK and return correct data wrapped in a Result`() =
        runBlocking {
            val mockSend = mockk<Send>()
            val expectedResult = mockk<SendView>()
            coEvery {
                clientVault.sends().decrypt(
                    send = mockSend,
                )
            } returns expectedResult
            val result = vaultSdkSource.decryptSend(
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
        }

    @Test
    fun `Folder decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val mockFolder = mockk<Folder>()
        val expectedResult = mockk<FolderView>()
        coEvery {
            clientVault.folders().decrypt(
                folder = mockFolder,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolder(
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
    }

    @Test
    fun `Folder decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val mockFolders = mockk<List<Folder>>()
        val expectedResult = mockk<List<FolderView>>()
        coEvery {
            clientVault.folders().decryptList(
                folders = mockFolders,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolderList(
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
    }
}
