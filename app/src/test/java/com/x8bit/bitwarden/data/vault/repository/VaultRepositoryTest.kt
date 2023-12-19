package com.x8bit.bitwarden.data.vault.repository

import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
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
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCollection
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganizationKeys
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
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
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val ciphersService: CiphersService = mockk()
    private val vaultDiskSource: VaultDiskSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val vaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        ciphersService = ciphersService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `ciphersStateFlow should emit decrypted list of ciphers when decryptCipherList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockCipherList = listOf(createMockCipher(number = 1))
            val mockEncryptedCipherList = mockCipherList.toEncryptedSdkCipherList()
            val mockCipherViewList = listOf(createMockCipherView(number = 1))
            val mutableCiphersStateFlow = MutableSharedFlow<List<SyncResponseJson.Cipher>>(
                replay = 1,
                extraBufferCapacity = Int.MAX_VALUE,
            )
            every {
                vaultDiskSource.getCiphers(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableCiphersStateFlow
            coEvery {
                vaultSdkSource.decryptCipherList(mockEncryptedCipherList)
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
        val throwable = Throwable("Fail")
        val mockCipherList = listOf(createMockCipher(number = 1))
        val mockEncryptedCipherList = mockCipherList.toEncryptedSdkCipherList()
        val mutableCiphersStateFlow = MutableSharedFlow<List<SyncResponseJson.Cipher>>(
            replay = 1,
            extraBufferCapacity = Int.MAX_VALUE,
        )
        every {
            vaultDiskSource.getCiphers(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableCiphersStateFlow
        coEvery {
            vaultSdkSource.decryptCipherList(mockEncryptedCipherList)
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
            val mockCollectionList = listOf(createMockCollection(number = 1))
            val mockEncryptedCollectionList = mockCollectionList.toEncryptedSdkCollectionList()
            val mockCollectionViewList = listOf(createMockCollectionView(number = 1))
            val mutableCollectionsStateFlow = MutableSharedFlow<List<SyncResponseJson.Collection>>(
                replay = 1,
                extraBufferCapacity = Int.MAX_VALUE,
            )
            every {
                vaultDiskSource.getCollections(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableCollectionsStateFlow
            coEvery {
                vaultSdkSource.decryptCollectionList(mockEncryptedCollectionList)
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
        val throwable = Throwable("Fail")
        val mockCollectionList = listOf(createMockCollection(number = 1))
        val mockEncryptedCollectionList = mockCollectionList.toEncryptedSdkCollectionList()
        val mutableCollectionStateFlow = MutableSharedFlow<List<SyncResponseJson.Collection>>(
            replay = 1,
            extraBufferCapacity = Int.MAX_VALUE,
        )
        every {
            vaultDiskSource.getCollections(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableCollectionStateFlow
        coEvery {
            vaultSdkSource.decryptCollectionList(mockEncryptedCollectionList)
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
            val mockFolderList = listOf(createMockFolder(number = 1))
            val mockEncryptedFolderList = mockFolderList.toEncryptedSdkFolderList()
            val mockFolderViewList = listOf(createMockFolderView(number = 1))
            val mutableFoldersStateFlow = MutableSharedFlow<List<SyncResponseJson.Folder>>(
                replay = 1,
                extraBufferCapacity = Int.MAX_VALUE,
            )
            every {
                vaultDiskSource.getFolders(userId = MOCK_USER_STATE.activeUserId)
            } returns mutableFoldersStateFlow
            coEvery {
                vaultSdkSource.decryptFolderList(mockEncryptedFolderList)
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
        val throwable = Throwable("Fail")
        val mockFolderList = listOf(createMockFolder(number = 1))
        val mockEncryptedFolderList = mockFolderList.toEncryptedSdkFolderList()
        val mutableFoldersStateFlow = MutableSharedFlow<List<SyncResponseJson.Folder>>(
            replay = 1,
            extraBufferCapacity = Int.MAX_VALUE,
        )
        every {
            vaultDiskSource.getFolders(userId = MOCK_USER_STATE.activeUserId)
        } returns mutableFoldersStateFlow
        coEvery {
            vaultSdkSource.decryptFolderList(mockEncryptedFolderList)
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
    fun `sync with syncService Success should unlock the vault for orgs if necessary and update AuthDiskSource and DataStateFlows`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
            fakeAuthDiskSource.assertOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = mapOf("mockId-1" to "mockKey-1"),
            )
            assertEquals(
                DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(createMockCipherView(number = 1)),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
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
            coVerify {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            }
        }

    @Test
    fun `sync with data should update vaultDataStateFlow to Pending before service sync`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()

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
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
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
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
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
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockException = IllegalStateException()
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockException.asFailure()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
            val mockSyncResponse = createMockSyncResponse(number = 1)
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockException.asFailure()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
    fun `sync with decryptCollectionList Failure should update vaultDataStateFlow with Error`() =
        runTest {
            val mockException = IllegalStateException()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns mockException.asFailure()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()

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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
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
    fun `sync with NoNetwork should update vault and send DataStateFlow to NoNetwork`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns UnknownHostException().asFailure()

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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()

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
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
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
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
                            folderViewList = listOf(createMockFolderView(number = 1)),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DataState.NoNetwork(
                        data = VaultData(
                            cipherViewList = listOf(createMockCipherView(number = 1)),
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery {
                syncService.sync()
            } returnsMany listOf(
                mockSyncResponse.asSuccess(),
                UnknownHostException().asFailure(),
            )
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(number = 1)))
            } returns listOf(createMockSendView(number = 1)).asSuccess()

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
    fun `lockVaultForCurrentUser should lock the vault for the current user if it is currently unlocked`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            verifyUnlockedVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            vaultRepository.lockVaultForCurrentUser()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Test
    fun `lockVaultIfNecessary should lock the given account if it is currently unlocked`() =
        runTest {
            val userId = "userId"
            verifyUnlockedVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                ),
                vaultRepository.vaultStateFlow.value,
            )

            vaultRepository.lockVaultIfNecessary(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                ),
                vaultRepository.vaultStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with unlockVault Success should sync and return Success`() =
        runTest {
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
            fakeAuthDiskSource.storeOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = createMockOrganizationKeys(number = 1),
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
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSyncForCurrentUser with unlockVault failure for users should return GenericError`() =
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
    fun `unlockVaultAndSyncForCurrentUser with unlockVault failure for orgs should return GenericError`() =
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
            fakeAuthDiskSource.storeOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = createMockOrganizationKeys(number = 1),
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
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns IllegalStateException().asFailure()

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
    fun `unlockVaultAndSyncForCurrentUser with unlockVault AuthenticationError for users should return AuthenticationError`() =
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
    fun `unlockVaultAndSyncForCurrentUser with unlockVault AuthenticationError for orgs should return AuthenticationError`() =
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
            fakeAuthDiskSource.storeOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = createMockOrganizationKeys(number = 1),
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
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()
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
        val organizationKeys = mapOf("orgId1" to "orgKey1")
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
        coEvery {
            vaultSdkSource.initializeOrganizationCrypto(
                request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
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
            organizationKeys = organizationKeys,
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
        coVerify(exactly = 1) {
            vaultSdkSource.initializeOrganizationCrypto(
                request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for users should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
                organizationKeys = organizationKeys,
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

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for orgs should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
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
                organizationKeys = organizationKeys,
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Test
    fun `unlockVault with initializeCrypto failure for users should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
                organizationKeys = organizationKeys,
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
    fun `unlockVault with initializeCrypto failure for orgs should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
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
                organizationKeys = organizationKeys,
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
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
        val organizationKeys = null
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
                organizationKeys = organizationKeys,
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
                            collectionViewList = listOf(createMockCollectionView(number = 1)),
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
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val mockSyncResponse = createMockSyncResponse(number = itemId)
        coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
        coEvery {
            vaultSdkSource.initializeOrganizationCrypto(
                request = InitOrgCryptoRequest(
                    organizationKeys = createMockOrganizationKeys(itemId),
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
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(itemId)))
        } returns listOf(item).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(itemId)))
        } returns listOf(createMockFolderView(1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(itemId)))
        } returns listOf(createMockCollectionView(itemId)).asSuccess()
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
        fakeAuthDiskSource.userState = MOCK_USER_STATE
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val itemIdString = "mockId-1234"
            val mockSyncResponse = createMockSyncResponse(1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val folderId = 1234
        val folderIdString = "mockId-$folderId"
        val folder = createMockFolderView(folderId)
        val mockSyncResponse = createMockSyncResponse(folderId)
        coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
        coEvery {
            vaultSdkSource.initializeOrganizationCrypto(
                request = InitOrgCryptoRequest(
                    organizationKeys = createMockOrganizationKeys(folderId),
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
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(folderId)))
        } returns listOf(createMockCipherView(folderId)).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(folderId)))
        } returns listOf(createMockFolderView(folderId)).asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(folderId)))
        } returns listOf(createMockCollectionView(folderId)).asSuccess()
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
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
        fakeAuthDiskSource.userState = MOCK_USER_STATE
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
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderIdString = "mockId-1234"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
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
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
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

    @Test
    fun `createCipher with encryptCipher failure should return CreateCipherResult failure`() =
        runTest {
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
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
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
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
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
            } returns createMockSdkCipher(number = 1).asSuccess()
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns createMockCipher(number = 1).asSuccess()
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(1))
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            } returns listOf(createMockSendView(1)).asSuccess()

            val result = vaultRepository.createCipher(cipherView = mockCipherView)

            assertEquals(
                CreateCipherResult.Success,
                result,
            )
        }

    @Test
    fun `updateCipher with encryptCipher failure should return UpdateCipherResult failure`() =
        runTest {
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher failure should return UpdateCipherResult failure`() =
        runTest {
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
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

            assertEquals(UpdateCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher success should return UpdateCipherResult success`() =
        runTest {
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(cipherView = mockCipherView)
            } returns createMockSdkCipher(number = 1).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1),
                )
            } returns createMockCipher(number = 1).asSuccess()
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(1))
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns listOf(createMockCipherView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns listOf(createMockFolderView(1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptCollectionList(listOf(createMockSdkCollection(1)))
            } returns listOf(createMockCollectionView(number = 1)).asSuccess()
            coEvery {
                vaultSdkSource.decryptSendList(listOf(createMockSdkSend(1)))
            } returns listOf(createMockSendView(1)).asSuccess()

            val result = vaultRepository.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Success, result)
        }

    //region Helper functions

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
