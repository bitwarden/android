package com.x8bit.bitwarden.data.vault.repository

import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
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
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
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

            val updatedUserState = MOCK_USER_STATE
                .copy(
                    accounts = mapOf(
                        "mockId-1" to MOCK_ACCOUNT.copy(
                            profile = MOCK_PROFILE.copy(
                                avatarColorHex = "mockAvatarColor-1",
                                stamp = "mockSecurityStamp-1",
                            ),
                        ),
                    ),
                )
            fakeAuthDiskSource.assertUserState(
                userState = updatedUserState,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.assertPrivateKey(
                userId = "mockId-1",
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

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with unlockVault Success should sync and return Success`() =
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
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        privateKey = "mockPrivateKey-1",
                        method = InitUserCryptoMethod.Password(
                            password = "mockPassword-1",
                            userKey = "mockKey-1",
                        ),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.Success)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                VaultUnlockResult.Success,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf("mockId-1"),
                ),
                vaultRepository.vaultStateFlow.value,
            )
            coVerify { syncService.sync() }
        }

    @Test
    fun `sync should be able to be called after unlockVaultAndSyncForCurrentUser is canceled`() =
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
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        privateKey = "mockPrivateKey-1",
                        method = InitUserCryptoMethod.Password(
                            password = "mockPassword-1",
                            userKey = "mockKey-1",
                        ),
                    ),
                )
            } just awaits

            val scope = CoroutineScope(Dispatchers.Unconfined)
            scope.launch {
                vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "mockPassword-1")
            }
            coVerify(exactly = 0) { syncService.sync() }
            scope.cancel()
            vaultRepository.sync()

            coVerify(exactly = 1) { syncService.sync() }
        }

    @Test
    fun `sync should not be able to be called while unlockVaultAndSyncForCurrentUser is called`() =
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
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        privateKey = "mockPrivateKey-1",
                        method = InitUserCryptoMethod.Password(
                            password = "mockPassword-1",
                            userKey = "mockKey-1",
                        ),
                    ),
                )
            } just awaits

            val scope = CoroutineScope(Dispatchers.Unconfined)
            scope.launch {
                vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "mockPassword-1")
            }
            // We call sync here but the call to the SyncService should be blocked
            // by the active call to unlockVaultAndSync
            vaultRepository.sync()

            scope.cancel()

            coVerify(exactly = 0) { syncService.sync() }
        }

    @Test
    fun `unlockVaultAndSyncForCurrentUser with unlockVault failure should return GenericError`() =
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
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        privateKey = "mockPrivateKey-1",
                        method = InitUserCryptoMethod.Password(
                            password = "mockPassword-1",
                            userKey = "mockKey-1",
                        ),
                    ),
                )
            } returns Result.failure(IllegalStateException())
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                VaultUnlockResult.GenericError,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with unlockVault AuthenticationError should return AuthenticationError`() =
        runTest {
            coEvery { syncService.sync() } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        privateKey = "mockPrivateKey-1",
                        method = InitUserCryptoMethod.Password(
                            password = "",
                            userKey = "mockKey-1",
                        ),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.AuthenticationError)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "")
            assertEquals(
                VaultUnlockResult.AuthenticationError,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with missing user state should return InvalidStateError `() =
        runTest {
            fakeAuthDiskSource.userState = null
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "")

            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with missing user key should return InvalidStateError `() =
        runTest {
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = null,
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with missing private key should return InvalidStateError `() =
        runTest {
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = null,
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Test
    fun `unlockVault with initializeCrypto success should return Success`() = runTest {
        val userId = "userId"
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationalKeys = emptyMap<String, String>()
        coEvery {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        assertEquals(
            VaultState(
                unlockedVaultUserIds = emptySet(),
            ),
            vaultRepository.vaultStateFlow.value,
        )

        val result = vaultRepository.unlockVault(
            userId = userId,
            masterPassword = masterPassword,
            kdf = kdf,
            email = email,
            userKey = userKey,
            privateKey = privateKey,
            organizationalKeys = organizationalKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        assertEquals(
            VaultState(
                unlockedVaultUserIds = setOf(userId),
            ),
            vaultRepository.vaultStateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationalKeys = emptyMap<String, String>()
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            val result = vaultRepository.unlockVault(
                userId = userId,
                masterPassword = masterPassword,
                kdf = kdf,
                email = email,
                userKey = userKey,
                privateKey = privateKey,
                organizationalKeys = organizationalKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError, result)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            }
        }

    @Test
    fun `unlockVault with initializeCrypto failure should return GenericError`() = runTest {
        val userId = "userId"
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationalKeys = emptyMap<String, String>()
        coEvery {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } returns Throwable("Fail").asFailure()
        assertEquals(
            VaultState(
                unlockedVaultUserIds = emptySet(),
            ),
            vaultRepository.vaultStateFlow.value,
        )

        val result = vaultRepository.unlockVault(
            userId = userId,
            masterPassword = masterPassword,
            kdf = kdf,
            email = email,
            userKey = userKey,
            privateKey = privateKey,
            organizationalKeys = organizationalKeys,
        )

        assertEquals(VaultUnlockResult.GenericError, result)
        assertEquals(
            VaultState(
                unlockedVaultUserIds = emptySet(),
            ),
            vaultRepository.vaultStateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        }
    }

    @Test
    fun `unlockVault with initializeCrypto awaiting should block calls to sync`() = runTest {
        val userId = "userId"
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationalKeys = emptyMap<String, String>()
        coEvery {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } just awaits

        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch {
            vaultRepository.unlockVault(
                userId = userId,
                masterPassword = masterPassword,
                kdf = kdf,
                email = email,
                userKey = userKey,
                privateKey = privateKey,
                organizationalKeys = organizationalKeys,
            )
        }
        // Does nothing because we are blocking
        vaultRepository.sync()
        scope.cancel()

        coVerify(exactly = 0) { syncService.sync() }
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        }
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

    @Test
    fun `getVaultItemStateFlow should receive updates whenever a sync is called`() = runTest {
        val itemId = 1234
        val itemIdString = "mockId-$itemId"
        val item = createMockCipherView(itemId)
        coEvery {
            syncService.sync()
        } returns Result.success(createMockSyncResponse(itemId))
        coEvery {
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(itemId)))
        } returns listOf(item).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(itemId)))
        } returns listOf(createMockFolderView(1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(listOf(createMockSdkSend(itemId)))
        } returns listOf(createMockSendView(itemId)).asSuccess()

        vaultRepository.getVaultItemStateFlow(itemIdString).test {
            assertEquals(DataState.Loading, awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Loaded(item), awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Pending(item), awaitItem())
            assertEquals(DataState.Loaded(item), awaitItem())
        }

        coVerify(exactly = 2) {
            syncService.sync()
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(itemId)))
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(itemId)))
            vaultSdkSource.decryptSendList(listOf(createMockSdkSend(itemId)))
        }
    }

    @Test
    fun `getVaultItemStateFlow should update to Error when a sync fails generically`() = runTest {
        val folderId = 1234
        val folderIdString = "mockId-$folderId"
        val throwable = Throwable("Fail")
        coEvery {
            syncService.sync()
        } returns throwable.asFailure()

        vaultRepository.getVaultItemStateFlow(folderIdString).test {
            assertEquals(DataState.Loading, awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Error<CipherView>(throwable), awaitItem())
        }

        coVerify(exactly = 1) {
            syncService.sync()
        }
    }

    @Test
    fun `getVaultItemStateFlow should update to NoNetwork when a sync fails from no network`() =
        runTest {
            val itemId = 1234
            val itemIdString = "mockId-$itemId"
            coEvery {
                syncService.sync()
            } returns UnknownHostException().asFailure()

            vaultRepository.getVaultItemStateFlow(itemIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.NoNetwork<CipherView>(), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
            }
        }

    @Test
    fun `getVaultItemStateFlow should update to Loaded with null when a item cannot be found`() =
        runTest {
            val itemIdString = "mockId-1234"
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            } returns listOf(createMockSendView(1)).asSuccess()

            vaultRepository.getVaultItemStateFlow(itemIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.Loaded(null), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            }
        }

    @Test
    fun `getVaultFolderStateFlow should receive updates whenever a sync is called`() = runTest {
        val folderId = 1234
        val folderIdString = "mockId-$folderId"
        val folder = createMockFolderView(folderId)
        coEvery {
            syncService.sync()
        } returns Result.success(createMockSyncResponse(folderId))
        coEvery {
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(folderId)))
        } returns listOf(createMockCipherView(folderId)).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(folderId)))
        } returns listOf(createMockFolderView(folderId)).asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(listOf(createMockSdkSend(folderId)))
        } returns listOf(createMockSendView(folderId)).asSuccess()

        vaultRepository.getVaultFolderStateFlow(folderIdString).test {
            assertEquals(DataState.Loading, awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Loaded(folder), awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Pending(folder), awaitItem())
            assertEquals(DataState.Loaded(folder), awaitItem())
        }

        coVerify(exactly = 2) {
            syncService.sync()
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(folderId)))
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(folderId)))
            vaultSdkSource.decryptSendList(listOf(createMockSdkSend(folderId)))
        }
    }

    @Test
    fun `getVaultFolderStateFlow should update to NoNetwork when a sync fails from no network`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            coEvery {
                syncService.sync()
            } returns UnknownHostException().asFailure()

            vaultRepository.getVaultFolderStateFlow(folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.NoNetwork<FolderView>(), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
            }
        }

    @Test
    fun `getVaultFolderStateFlow should update to Error when a sync fails generically`() = runTest {
        val folderId = 1234
        val folderIdString = "mockId-$folderId"
        val throwable = Throwable("Fail")
        coEvery {
            syncService.sync()
        } returns throwable.asFailure()

        vaultRepository.getVaultFolderStateFlow(folderIdString).test {
            assertEquals(DataState.Loading, awaitItem())
            vaultRepository.sync()
            assertEquals(DataState.Error<FolderView>(throwable), awaitItem())
        }

        coVerify(exactly = 1) {
            syncService.sync()
        }
    }

    @Test
    fun `getVaultFolderStateFlow should update to Loaded with null when a item cannot be found`() =
        runTest {
            val folderIdString = "mockId-1234"
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            } returns listOf(createMockSendView(1)).asSuccess()

            vaultRepository.getVaultFolderStateFlow(folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.Loaded(null), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            }
        }
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
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
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountJson.Tokens(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to MOCK_ACCOUNT,
    ),
)
