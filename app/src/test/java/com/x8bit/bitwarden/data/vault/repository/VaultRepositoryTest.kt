package com.x8bit.bitwarden.data.vault.repository

import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Kdf
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCollection
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganizationKeys
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSend
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCollection
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

@Suppress("LargeClass")
class VaultRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val syncService: SyncService = mockk()
    private val sendsService: SendsService = mockk()
    private val ciphersService: CiphersService = mockk()
    private val vaultDiskSource: VaultDiskSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val mutableVaultStateFlow = MutableStateFlow(
        VaultState(
            unlockedVaultUserIds = emptySet(),
            unlockingVaultUserIds = emptySet(),
        ),
    )
    private val vaultLockManager: VaultLockManager = mockk {
        every { vaultStateFlow } returns mutableVaultStateFlow
        every { isVaultUnlocked(any()) } returns false
        every { isVaultUnlocking(any()) } returns false
        every { lockVault(any()) } just runs
        every { lockVaultIfNecessary(any()) } just runs
        every { lockVaultForCurrentUser() } just runs
    }

    private val vaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        sendsService = sendsService,
        ciphersService = ciphersService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `ciphersStateFlow should emit decrypted list of ciphers when decryptCipherList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherList = listOf(createMockCipher(number = 1))
            val mockEncryptedCipherList = mockCipherList.toEncryptedSdkCipherList()
            val mockCipherViewList = listOf(createMockCipherView(number = 1))
            val mutableCiphersStateFlow =
                bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>(replay = 1)
            every {
                vaultDiskSource.getCiphers(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableCiphersStateFlow
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = userId,
                    cipherList = mockEncryptedCipherList,
                )
            } returns mockCipherViewList.asSuccess()

            vaultRepository
                .ciphersStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableCiphersStateFlow.tryEmit(mockCipherList)
                    assertEquals(DataState.Loaded(mockCipherViewList), awaitItem())
                }
        }

    @Test
    fun `ciphersStateFlow should emit an error when decryptCipherList fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val throwable = Throwable("Fail")
        val mockCipherList = listOf(createMockCipher(number = 1))
        val mockEncryptedCipherList = mockCipherList.toEncryptedSdkCipherList()
        val mutableCiphersStateFlow =
            bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>(replay = 1)
        every {
            vaultDiskSource.getCiphers(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableCiphersStateFlow
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = userId,
                cipherList = mockEncryptedCipherList,
            )
        } returns throwable.asFailure()

        vaultRepository
            .ciphersStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableCiphersStateFlow.tryEmit(mockCipherList)
                assertEquals(DataState.Error<List<CipherView>>(throwable), awaitItem())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `collectionsStateFlow should emit decrypted list of collections when decryptCollectionList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCollectionList = listOf(createMockCollection(number = 1))
            val mockEncryptedCollectionList = mockCollectionList.toEncryptedSdkCollectionList()
            val mockCollectionViewList = listOf(createMockCollectionView(number = 1))
            val mutableCollectionsStateFlow =
                bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>(replay = 1)
            every {
                vaultDiskSource.getCollections(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableCollectionsStateFlow
            coEvery {
                vaultSdkSource.decryptCollectionList(
                    userId = userId,
                    collectionList = mockEncryptedCollectionList,
                )
            } returns mockCollectionViewList.asSuccess()

            vaultRepository
                .collectionsStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableCollectionsStateFlow.tryEmit(mockCollectionList)
                    assertEquals(DataState.Loaded(mockCollectionViewList), awaitItem())
                }
        }

    @Test
    fun `collectionsStateFlow should emit an error when decryptCollectionList fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val throwable = Throwable("Fail")
        val mockCollectionList = listOf(createMockCollection(number = 1))
        val mockEncryptedCollectionList = mockCollectionList.toEncryptedSdkCollectionList()
        val mutableCollectionStateFlow =
            bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>(replay = 1)
        every {
            vaultDiskSource.getCollections(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableCollectionStateFlow
        coEvery {
            vaultSdkSource.decryptCollectionList(
                userId = userId,
                collectionList = mockEncryptedCollectionList,
            )
        } returns throwable.asFailure()

        vaultRepository
            .collectionsStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableCollectionStateFlow.tryEmit(mockCollectionList)
                assertEquals(DataState.Error<List<CollectionView>>(throwable), awaitItem())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `foldersStateFlow should emit decrypted list of folders when decryptFolderList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockFolderList = listOf(createMockFolder(number = 1))
            val mockEncryptedFolderList = mockFolderList.toEncryptedSdkFolderList()
            val mockFolderViewList = listOf(createMockFolderView(number = 1))
            val mutableFoldersStateFlow =
                bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>(replay = 1)
            every {
                vaultDiskSource.getFolders(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableFoldersStateFlow
            coEvery {
                vaultSdkSource.decryptFolderList(
                    userId = userId,
                    folderList = mockEncryptedFolderList,
                )
            } returns mockFolderViewList.asSuccess()

            vaultRepository
                .foldersStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableFoldersStateFlow.tryEmit(mockFolderList)
                    assertEquals(DataState.Loaded(mockFolderViewList), awaitItem())
                }
        }

    @Test
    fun `foldersStateFlow should emit an error when decryptFolderList fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val throwable = Throwable("Fail")
        val mockFolderList = listOf(createMockFolder(number = 1))
        val mockEncryptedFolderList = mockFolderList.toEncryptedSdkFolderList()
        val mutableFoldersStateFlow =
            bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>(replay = 1)
        every {
            vaultDiskSource.getFolders(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableFoldersStateFlow
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = userId,
                folderList = mockEncryptedFolderList,
            )
        } returns throwable.asFailure()

        vaultRepository
            .foldersStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableFoldersStateFlow.tryEmit(mockFolderList)
                assertEquals(DataState.Error<List<FolderView>>(throwable), awaitItem())
            }
    }

    @Test
    fun `sendDataStateFlow should emit decrypted list of sends when decryptSendsList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendList = listOf(createMockSend(number = 1))
            val mockEncryptedSendList = mockSendList.toEncryptedSdkSendList()
            val mockSendViewList = listOf(createMockSendView(number = 1))
            val mutableSendsStateFlow =
                bufferedMutableSharedFlow<List<SyncResponseJson.Send>>(replay = 1)
            every {
                vaultDiskSource.getSends(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableSendsStateFlow
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = mockEncryptedSendList,
                )
            } returns mockSendViewList.asSuccess()

            vaultRepository
                .sendDataStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableSendsStateFlow.tryEmit(mockSendList)
                    assertEquals(DataState.Loaded(SendData(mockSendViewList)), awaitItem())
                }
        }

    @Test
    fun `sendDataStateFlow should emit an error when decryptSendsList fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val throwable = Throwable("Fail")
        val mockSendList = listOf(createMockSend(number = 1))
        val mockEncryptedSendList = mockSendList.toEncryptedSdkSendList()
        val mutableSendsStateFlow =
            bufferedMutableSharedFlow<List<SyncResponseJson.Send>>(replay = 1)
        every {
            vaultDiskSource.getSends(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableSendsStateFlow
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = mockEncryptedSendList,
            )
        } returns throwable.asFailure()

        vaultRepository
            .sendDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendsStateFlow.tryEmit(mockSendList)
                assertEquals(DataState.Error<SendData>(throwable), awaitItem())
            }
    }

    @Test
    fun `deleteVaultData should call deleteVaultData on VaultDiskSource`() {
        val userId = "userId-1234"
        coEvery { vaultDiskSource.deleteVaultData(userId) } just runs

        vaultRepository.deleteVaultData(userId = userId)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteVaultData(userId)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with syncService Success should unlock the vault for orgs if necessary and update AuthDiskSource and VaultDiskSource`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
            } just runs

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
            fakeAuthDiskSource.assertOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = mapOf("mockId-1" to "mockKey-1"),
            )
            fakeAuthDiskSource.assertOrganizations(
                userId = "mockId-1",
                organizations = listOf(createMockOrganization(number = 1)),
            )
            coVerify {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            }
        }

    @Test
    fun `sync with syncService Failure should update DataStateFlow with an Error`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val mockException = IllegalStateException("sad")
        coEvery { syncService.sync() } returns mockException.asFailure()

        vaultRepository.sync()

        assertEquals(
            DataState.Error<List<CipherView>>(mockException),
            vaultRepository.ciphersStateFlow.value,
        )
        assertEquals(
            DataState.Error<List<CollectionView>>(mockException),
            vaultRepository.collectionsStateFlow.value,
        )
        assertEquals(
            DataState.Error<List<FolderView>>(mockException),
            vaultRepository.foldersStateFlow.value,
        )
        assertEquals(
            DataState.Error<SendData>(mockException),
            vaultRepository.sendDataStateFlow.value,
        )
    }

    @Test
    fun `sync with syncService Failure should update vaultDataStateFlow with an Error`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val mockException = IllegalStateException("sad")
        coEvery { syncService.sync() } returns mockException.asFailure()
        setupVaultDiskSourceFlows()

        vaultRepository
            .vaultDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.Error<VaultData>(mockException), awaitItem())
            }
    }

    @Test
    fun `sync with NoNetwork should update DataStateFlows to NoNetwork`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns UnknownHostException().asFailure()

        vaultRepository.sync()

        assertEquals(
            DataState.NoNetwork(data = null),
            vaultRepository.ciphersStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultRepository.collectionsStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultRepository.foldersStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultRepository.sendDataStateFlow.value,
        )
    }

    @Test
    fun `sync with NoNetwork should update vaultDataStateFlow to NoNetwork`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns UnknownHostException().asFailure()
        setupVaultDiskSourceFlows()

        vaultRepository
            .vaultDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.NoNetwork(data = null), awaitItem())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with NoNetwork data should update sendDataStateFlow to Pending and NoNetwork with data`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            coEvery { syncService.sync() } returns UnknownHostException().asFailure()
            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = listOf(createMockSdkSend(1)),
                )
            } returns listOf(createMockSendView(1)).asSuccess()

            vaultRepository
                .sendDataStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    sendsFlow.tryEmit(listOf(createMockSend(1)))
                    assertEquals(
                        DataState.Loaded(data = SendData(listOf(createMockSendView(1)))),
                        awaitItem(),
                    )
                    vaultRepository.sync()
                    assertEquals(
                        DataState.Pending(data = SendData(listOf(createMockSendView(1)))),
                        awaitItem(),
                    )
                    assertEquals(
                        DataState.NoNetwork(data = SendData(listOf(createMockSendView(1)))),
                        awaitItem(),
                    )
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVaultForCurrentUser should delegate to the VaultLockManager`() {
        vaultRepository.lockVaultForCurrentUser()
        verify { vaultLockManager.lockVaultForCurrentUser() }
    }

    @Test
    fun `lockVaultIfNecessary should delete to the VaultLockManager`() {
        val userId = "userId"
        vaultRepository.lockVaultIfNecessary(userId = userId)
        verify { vaultLockManager.lockVaultIfNecessary(userId = userId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with VaultLockManager Success should unlock for the current user, sync, and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = listOf(createMockSdkSend(number = 1)),
                )
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storeOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockVaultUnlockResult = VaultUnlockResult.Success
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Password(
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                    ),

                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            } returns mockVaultUnlockResult

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify { syncService.sync() }
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Password(
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                    ),

                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with VaultLockManager non-Success should unlock for the current user and return the error`() =
        runTest {
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = listOf(createMockSdkSend(number = 1)),
                )
            } returns listOf(createMockSendView(number = 1)).asSuccess()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storeOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockVaultUnlockResult = VaultUnlockResult.InvalidStateError
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Password(
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                    ),

                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            } returns mockVaultUnlockResult

            val result = vaultRepository.unlockVaultAndSyncForCurrentUser(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify(exactly = 0) { syncService.sync() }
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Password(
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                    ),

                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Test
    fun `unlockVault should delegate to the VaultLockManager`() = runTest {
        val userId = "userId"
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = mapOf("orgId1" to "orgKey1")
        val mockVaultUnlockResult = mockk<VaultUnlockResult>()
        coEvery {
            vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                privateKey = privateKey,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),

                organizationKeys = organizationKeys,
            )
        } returns mockVaultUnlockResult

        val result = vaultRepository.unlockVault(
            userId = userId,
            masterPassword = masterPassword,
            kdf = kdf,
            email = email,
            userKey = userKey,
            privateKey = privateKey,
            organizationKeys = organizationKeys,
        )

        assertEquals(mockVaultUnlockResult, result)
        coVerify(exactly = 1) {
            vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                privateKey = privateKey,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),

                organizationKeys = organizationKeys,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sync should not be able to be called while isVaultUnlocking is true for the current user`() =
        runTest {
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = listOf(createMockSdkSend(number = 1)),
                )
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
                    userId = userId,
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

            every {
                vaultLockManager.isVaultUnlocking(userId)
            } returns true

            // We call sync here but the call to the SyncService should be blocked
            // by unlocking vault.
            vaultRepository.sync()
            coVerify(exactly = 0) { syncService.sync() }

            every {
                vaultLockManager.isVaultUnlocking(userId)
            } returns false

            vaultRepository.sync()
            coVerify(exactly = 1) { syncService.sync() }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with missing user state should return InvalidStateError `() =
        runTest {
            fakeAuthDiskSource.userState = null
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
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
                    unlockingVaultUserIds = emptySet(),
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
                    unlockingVaultUserIds = emptySet(),
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
                    unlockingVaultUserIds = emptySet(),
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
                    unlockingVaultUserIds = emptySet(),
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
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Test
    fun `clearUnlockedData should update the vaultDataStateFlow to Loading`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = userId,
                cipherList = listOf(createMockSdkCipher(1)),
            )
        } returns listOf(createMockCipherView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = userId,
                folderList = listOf(createMockSdkFolder(1)),
            )
        } returns listOf(createMockFolderView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(
                userId = userId,
                collectionList = listOf(createMockSdkCollection(1)),
            )
        } returns listOf(createMockCollectionView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = listOf(createMockSdkSend(number = 1)),
            )
        } returns listOf(createMockSendView(number = 1)).asSuccess()
        val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
        val collectionsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>()
        val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(
            ciphersFlow = ciphersFlow,
            collectionsFlow = collectionsFlow,
            foldersFlow = foldersFlow,
            sendsFlow = sendsFlow,
        )

        vaultRepository.vaultDataStateFlow.test {
            assertEquals(DataState.Loading, awaitItem())

            ciphersFlow.tryEmit(listOf(createMockCipher(number = 1)))
            collectionsFlow.tryEmit(listOf(createMockCollection(number = 1)))
            foldersFlow.tryEmit(listOf(createMockFolder(number = 1)))
            sendsFlow.tryEmit(listOf(createMockSend(number = 1)))

            assertEquals(
                DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(createMockCipherView(number = 1)),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
                awaitItem(),
            )

            vaultRepository.clearUnlockedData()

            assertEquals(DataState.Loading, awaitItem())
        }
    }

    @Test
    fun `clearUnlockedData should update the sendDataStateFlow to Loading`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = userId,
                sendList = listOf(createMockSdkSend(number = 1)),
            )
        } returns listOf(createMockSendView(number = 1)).asSuccess()
        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

        vaultRepository.sendDataStateFlow.test {
            assertEquals(DataState.Loading, awaitItem())

            sendsFlow.tryEmit(listOf(createMockSend(number = 1)))

            assertEquals(
                DataState.Loaded(
                    data = SendData(sendViewList = listOf(createMockSendView(number = 1))),
                ),
                awaitItem(),
            )

            vaultRepository.clearUnlockedData()

            assertEquals(DataState.Loading, awaitItem())
        }
    }

    @Test
    fun `getVaultItemStateFlow should update to Error when a sync fails generically`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns throwable.asFailure()
            setupVaultDiskSourceFlows()

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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns UnknownHostException().asFailure()
            setupVaultDiskSourceFlows()

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
    fun `getVaultFolderStateFlow should update to NoNetwork when a sync fails from no network`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns UnknownHostException().asFailure()
            setupVaultDiskSourceFlows()

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
    fun `getVaultFolderStateFlow should update to Error when a sync fails generically`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns throwable.asFailure()
            setupVaultDiskSourceFlows()

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
    fun `getSendStateFlow should update emit SendView when present`() = runTest {
        val sendId = 1
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val sendView = createMockSendView(number = sendId)
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = emptyList(),
            )
        } returns emptyList<SendView>().asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = listOf(createMockSdkSend(number = sendId)),
            )
        } returns listOf(sendView).asSuccess()

        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

        vaultRepository.getSendStateFlow("mockId-$sendId").test {
            assertEquals(DataState.Loading, awaitItem())
            sendsFlow.tryEmit(emptyList())
            assertEquals(DataState.Loaded<SendView?>(null), awaitItem())
            sendsFlow.tryEmit(listOf(createMockSend(number = sendId)))
            assertEquals(DataState.Loaded<SendView?>(sendView), awaitItem())
        }
    }

    @Test
    fun `getSendStateFlow should update to NoNetwork when a sync fails from no network`() =
        runTest {
            val sendId = 1234
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns UnknownHostException().asFailure()
            setupVaultDiskSourceFlows()

            vaultRepository.getSendStateFlow("mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.NoNetwork<SendView?>(), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
            }
        }

    @Test
    fun `getSendStateFlow should update to Error when a sync fails generically`() =
        runTest {
            val sendId = 1234
            val throwable = Throwable("Fail")
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns throwable.asFailure()
            setupVaultDiskSourceFlows()

            vaultRepository.getSendStateFlow("mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                vaultRepository.sync()
                assertEquals(DataState.Error<SendView?>(throwable), awaitItem())
            }

            coVerify(exactly = 1) {
                syncService.sync()
            }
        }

    @Test
    fun `createCipher with encryptCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createCipher(cipherView = mockCipherView)

            assertEquals(
                CreateCipherResult.Error,
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipher with ciphersService createCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1).asSuccess()
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createCipher(cipherView = mockCipherView)

            assertEquals(
                CreateCipherResult.Error,
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipher with ciphersService createCipher success should return CreateCipherResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns mockCipher.asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, mockCipher) } just runs

            val result = vaultRepository.createCipher(cipherView = mockCipherView)

            assertEquals(
                CreateCipherResult.Success,
                result,
            )
        }

    @Test
    fun `updateCipher with encryptCipher failure should return UpdateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher failure should return UpdateCipherResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher Invalid response should return UpdateCipherResult Error with a non-null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns UpdateCipherResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(
                UpdateCipherResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher Success response should return UpdateCipherResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns UpdateCipherResponseJson
                .Success(cipher = mockCipher)
                .asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher,
                )
            } just runs

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Success, result)
        }

    @Test
    fun `createSend with encryptSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView)

            assertEquals(CreateSendResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with sendsService createSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1).asSuccess()
            coEvery {
                sendsService.createSend(body = createMockSendJsonRequest(number = 1))
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView)

            assertEquals(CreateSendResult.Error, result)
        }

    @Test
    fun `createSend with sendsService createSend success should return CreateSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val mockSend = createMockSend(number = 1)
            val mockSendViewResult = createMockSendView(number = 2)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery {
                sendsService.createSend(body = createMockSendJsonRequest(number = 1))
            } returns mockSend.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId, mockSend) } just runs
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendViewResult.asSuccess()

            val result = vaultRepository.createSend(sendView = mockSendView)

            assertEquals(CreateSendResult.Success(mockSendViewResult), result)
        }

    @Test
    fun `updateSend with encryptSend failure should return UpdateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend failure should return UpdateSendResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1).asSuccess()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend Invalid response should return UpdateSendResult Error with a non-null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1).asSuccess()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1),
                )
            } returns UpdateSendResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(
                UpdateSendResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend success and decryption error should return UpdateSendResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1).asSuccess()
            val mockSend = createMockSend(number = 1)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns Throwable("Fail").asFailure()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend Success response should return UpdateSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1).asSuccess()
            val mockSend = createMockSend(number = 1)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            val mockSendViewResult = createMockSendView(number = 2)
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns mockSendViewResult.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Success(mockSendViewResult), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Error should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns Throwable("Fail").asFailure()

            val result = vaultRepository.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Success and vaultSdkSource decryptSend Failure should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSend = createMockSend(number = 1)
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns Throwable("Fail").asFailure()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = vaultRepository.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Success should return RemovePasswordSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            val mockSend = createMockSend(number = 1)
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns mockSendView.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = vaultRepository.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Success(mockSendView), result)
        }

    //region Helper functions

    /**
     * Helper setup all flows required to properly subscribe to the
     * [VaultRepository.vaultDataStateFlow].
     */
    private fun setupVaultDiskSourceFlows(
        ciphersFlow: Flow<List<SyncResponseJson.Cipher>> = bufferedMutableSharedFlow(),
        collectionsFlow: Flow<List<SyncResponseJson.Collection>> = bufferedMutableSharedFlow(),
        foldersFlow: Flow<List<SyncResponseJson.Folder>> = bufferedMutableSharedFlow(),
        sendsFlow: Flow<List<SyncResponseJson.Send>> = bufferedMutableSharedFlow(),
    ) {
        coEvery { vaultDiskSource.getCiphers(MOCK_USER_STATE.activeUserId) } returns ciphersFlow
        coEvery {
            vaultDiskSource.getCollections(MOCK_USER_STATE.activeUserId)
        } returns collectionsFlow
        coEvery { vaultDiskSource.getFolders(MOCK_USER_STATE.activeUserId) } returns foldersFlow
        coEvery { vaultDiskSource.getSends(MOCK_USER_STATE.activeUserId) } returns sendsFlow
    }

    /**
     * Helper to ensures that the vault for the user with the given [userId] is unlocked.
     */
    private suspend fun verifyUnlockedVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = null
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
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

        val result = vaultRepository.unlockVault(
            userId = userId,
            masterPassword = masterPassword,
            kdf = kdf,
            email = email,
            userKey = userKey,
            privateKey = privateKey,
            organizationKeys = organizationKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                userId = userId,
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

    //endregion Helper functions
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
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
