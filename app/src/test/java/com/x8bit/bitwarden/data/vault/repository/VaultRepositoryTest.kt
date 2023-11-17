package com.x8bit.bitwarden.data.vault.repository

import app.cash.turbine.test
import com.bitwarden.core.InitCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

@Suppress("LargeClass")
class VaultRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val syncService: SyncService = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val vaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `sync with syncService Success should update AuthDiskSource and DataStateFlows`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sync()

            fakeAuthDiskSource.assertUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.assertPrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            assertEquals(
                DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(createMockCipherView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                    ),
                ),
                vaultRepository.vaultDataStateFlow.value,
            )
            assertEquals(
                DataState.Loaded(
                    data = SendData(
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
                vaultRepository.sendDataStateFlow.value,
            )
        }

    @Test
    fun `sync with data should update vaultDataStateFlow to Pending before service sync`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.vaultDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Pending(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `sync with data should update sendDataStateFlow to Pending before service sync`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sendDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Pending(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `sync with decryptCipherList Failure should update vaultDataStateFlow with Error`() =
        runTest {
            val mockException = IllegalStateException()
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockException.asFailure()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sync()

            assertEquals(
                DataState.Error<VaultData>(error = mockException),
                vaultRepository.vaultDataStateFlow.value,
            )
        }

    @Test
    fun `sync with decryptFolderList Failure should update vaultDataStateFlow with Error`() =
        runTest {
            val mockException = IllegalStateException()
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockException.asFailure()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sync()

            assertEquals(
                DataState.Error<VaultData>(error = mockException),
                vaultRepository.vaultDataStateFlow.value,
            )
        }

    @Test
    fun `sync with decryptSendList Failure should update sendDataStateFlow with Error`() =
        runTest {
            val mockException = IllegalStateException()
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns mockException.asFailure()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sync()

            assertEquals(
                DataState.Error<SendData>(error = mockException),
                vaultRepository.sendDataStateFlow.value,
            )
        }

    @Test
    fun `sync with syncService Failure should update vault and send DataStateFlow with an Error`() =
        runTest {
            val mockException = IllegalStateException(
                "sad",
            )
            coEvery {
                syncService.sync()
            } returns mockException.asFailure()

            vaultRepository.sync()

            assertEquals(
                DataState.Error(
                    error = mockException,
                    data = null,
                ),
                vaultRepository.vaultDataStateFlow.value,
            )
            assertEquals(
                DataState.Error(
                    error = mockException,
                    data = null,
                ),
                vaultRepository.sendDataStateFlow.value,
            )
        }

    @Test
    fun `sync with NoNetwork should update vault and send DataStateFlow to NoNetwork`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns UnknownHostException().asFailure()

            vaultRepository.sync()

            assertEquals(
                DataState.NoNetwork(
                    data = null,
                ),
                vaultRepository.vaultDataStateFlow.value,
            )
            assertEquals(
                DataState.NoNetwork(
                    data = null,
                ),
                vaultRepository.sendDataStateFlow.value,
            )
        }

    @Test
    fun `sync with NoNetwork data should update vaultDataStateFlow to NoNetwork with data`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.vaultDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                coEvery {
                    syncService.sync()
                } returns UnknownHostException().asFailure()
                vaultRepository.sync()
                assertEquals(
                    DataState.Pending(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.NoNetwork(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `sync with NoNetwork data should update sendDataStateFlow to NoNetwork with data`() =
        runTest {
            coEvery {
                syncService.sync()
            } returnsMany listOf(
                Result.success(createMockSyncResponse(number = 1)),
                UnknownHostException().asFailure(),
            )
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sendDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Pending(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.NoNetwork(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `unlockVaultAndSync with initializeCrypto Success should sync and return Success`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.Success)

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")

            assertEquals(
                VaultUnlockResult.Success,
                result,
            )
            coVerify { syncService.sync() }
        }

    @Test
    fun `sync should be able to be called after unlockVaultAndSync is canceled`() = runTest {
        coEvery {
            syncService.sync()
        } returns Result.success(createMockSyncResponse(number = 1))
        coEvery {
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
        } returns listOf(createMockCipherView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
        } returns listOf(createMockFolderView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
        } returns listOf(createMockSendView(number = 1)).asSuccess()
        fakeAuthDiskSource.storePrivateKey(
            userId = "mockUserId",
            privateKey = "mockPrivateKey-1",
        )
        fakeAuthDiskSource.storeUserKey(
            userId = "mockUserId",
            userKey = "mockKey-1",
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.initializeCrypto(
                request = InitCryptoRequest(
                    kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                    email = "email",
                    password = "mockPassword-1",
                    userKey = "mockKey-1",
                    privateKey = "mockPrivateKey-1",
                    organizationKeys = mapOf(),
                ),
            )
        } just awaits

        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch {
            vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")
        }
        coVerify(exactly = 0) { syncService.sync() }
        scope.cancel()
        vaultRepository.sync()

        coVerify(exactly = 1) { syncService.sync() }
    }

    @Test
    fun `sync should not be able to be called while unlockVaultAndSync is called`() = runTest {
        coEvery {
            syncService.sync()
        } returns Result.success(createMockSyncResponse(number = 1))
        coEvery {
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
        } returns listOf(createMockCipherView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
        } returns listOf(createMockFolderView(number = 1)).asSuccess()
        fakeAuthDiskSource.storePrivateKey(
            userId = "mockUserId",
            privateKey = "mockPrivateKey-1",
        )
        fakeAuthDiskSource.storeUserKey(
            userId = "mockUserId",
            userKey = "mockKey-1",
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.initializeCrypto(
                request = InitCryptoRequest(
                    kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                    email = "email",
                    password = "mockPassword-1",
                    userKey = "mockKey-1",
                    privateKey = "mockPrivateKey-1",
                    organizationKeys = mapOf(),
                ),
            )
        } just awaits

        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch {
            vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")
        }
        // We call sync here but the call to the SyncService should be blocked
        // by the active call to unlockVaultAndSync
        vaultRepository.sync()

        scope.cancel()

        coVerify(exactly = 0) { syncService.sync() }
    }

    @Test
    fun `unlockVaultAndSync with initializeCrypto failure should return GenericError`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.failure(IllegalStateException())

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")

            assertEquals(
                VaultUnlockResult.GenericError,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSync with initializeCrypto AuthenticationError should return AuthenticationError`() =
        runTest {
            coEvery { syncService.sync() } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.AuthenticationError)

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            assertEquals(
                VaultUnlockResult.AuthenticationError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing user state should return InvalidStateError `() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")

            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing user key should return InvalidStateError `() =
        runTest {
            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = null,
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing private key should return InvalidStateError `() =
        runTest {
            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = null,
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }

    @Test
    fun `clearUnlockedData should update the vaultDataStateFlow to Loading`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.vaultDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )

                vaultRepository.clearUnlockedData()

                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `clearUnlockedData should update the sendDataStateFlow to Loading`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            vaultRepository.sendDataStateFlow.test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
                vaultRepository.sync()
                assertEquals(
                    DataState.Loaded(
                        data = SendData(
                            sendViewList = listOf(createMockSendView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )

                vaultRepository.clearUnlockedData()

                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )
            }
        }
}

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockUserId",
    accounts = mapOf(
        "mockUserId" to AccountJson(
            profile = AccountJson.Profile(
                userId = "activeUserId",
                email = "email",
                isEmailVerified = true,
                name = null,
                stamp = null,
                organizationId = null,
                avatarColorHex = null,
                hasPremium = true,
                forcePasswordResetReason = null,
                kdfType = null,
                kdfIterations = null,
                kdfMemory = null,
                kdfParallelism = null,
                userDecryptionOptions = null,
            ),
            tokens = AccountJson.Tokens(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
            settings = AccountJson.Settings(
                environmentUrlData = null,
            ),
        ),
    ),
)
