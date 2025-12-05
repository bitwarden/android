package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockCollection
import com.bitwarden.network.model.createMockDomains
import com.bitwarden.network.model.createMockFolder
import com.bitwarden.network.model.createMockOrganization
import com.bitwarden.network.model.createMockOrganizationKeys
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.model.createMockProfile
import com.bitwarden.network.model.createMockSend
import com.bitwarden.network.model.createMockSyncResponse
import com.bitwarden.network.service.SyncService
import com.bitwarden.send.SendView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCollection
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.createMockDomainsData
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
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Suppress("LargeClass")
class VaultSyncManagerTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val syncService: SyncService = mockk {
        coEvery {
            getAccountRevisionDateMillis()
        } returns clock.instant().plus(1, ChronoUnit.MINUTES).toEpochMilli().asSuccess()
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val settingsDiskSource = mockk<SettingsDiskSource> {
        every { getLastSyncTime(userId = any()) } returns clock.instant()
        every { storeLastSyncTime(userId = any(), lastSyncTime = any()) } just runs
    }
    private val mutableGetCiphersFlow: MutableStateFlow<List<SyncResponseJson.Cipher>> =
        MutableStateFlow(listOf(createMockCipher(1)))
    private val vaultDiskSource: VaultDiskSource = mockk {
        coEvery { resyncVaultData(any()) } just runs
        every { getCiphersFlow(any()) } returns mutableGetCiphersFlow
    }
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val mutableVaultStateFlow = MutableStateFlow<List<VaultUnlockData>>(emptyList())
    private val mutableUnlockedUserIdsStateFlow = MutableStateFlow<Set<String>>(emptySet())
    private val vaultLockManager: VaultLockManager = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultStateFlow
        every { isVaultUnlocked(userId = any()) } answers { call ->
            call.invocation.args.first() in mutableUnlockedUserIdsStateFlow.value
        }
        every { isVaultUnlocking(userId = any()) } returns false
        every { lockVault(userId = any(), isUserInitiated = any()) } just runs
        every { lockVaultForCurrentUser(isUserInitiated = any()) } just runs
        coEvery { waitUntilUnlocked(userId = any()) } coAnswers { call ->
            mutableUnlockedUserIdsStateFlow.first { call.invocation.args.first() in it }
        }
    }
    private val userLogoutManager: UserLogoutManager = mockk {
        every { softLogout(any(), any()) } just runs
    }
    private val userStateManager: UserStateManager = mockk {
        val blockSlot = slot<suspend () -> SyncVaultDataResult>()
        coEvery { userStateTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured() }
    }
    private val mutableFullSyncFlow = bufferedMutableSharedFlow<String>()
    private val pushManager: PushManager = mockk {
        every { fullSyncFlow } returns mutableFullSyncFlow
    }
    private val mutableDatabaseSchemeChangeFlow = bufferedMutableSharedFlow<Unit>()
    private val databaseSchemeManager: DatabaseSchemeManager = mockk {
        every { databaseSchemeChangeFlow } returns mutableDatabaseSchemeChangeFlow
    }

    private val vaultSyncManager: VaultSyncManager = VaultSyncManagerImpl(
        syncService = syncService,
        settingsDiskSource = settingsDiskSource,
        authDiskSource = fakeAuthDiskSource,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        userLogoutManager = userLogoutManager,
        userStateManager = userStateManager,
        vaultLockManager = vaultLockManager,
        clock = clock,
        databaseSchemeManager = databaseSchemeManager,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class, MissingPropertyException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class, MissingPropertyException::class)
    }

    @Test
    fun `userSwitchingChangesFlow should cancel any pending sync call`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } just awaits

        vaultSyncManager.sync()
        vaultSyncManager.sync()
        coVerify(exactly = 1) {
            // Despite being called twice, we only allow 1 sync
            syncService.sync()
        }

        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = "mockId-2",
            accounts = mapOf("mockId-2" to mockk()),
        )
        vaultSyncManager.sync()
        coVerify(exactly = 2) {
            // A second sync should have happened now since it was cancelled by the userState change
            syncService.getAccountRevisionDateMillis()
            syncService.sync()
        }
    }

    @Test
    fun `userSwitchingChangesFlow should clear unlocked data`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        coEvery {
            vaultSdkSource.decryptCipherListWithFailures(
                userId = userId,
                cipherList = listOf(createMockSdkCipher(number = 1, clock = clock)),
            )
        } returns createMockDecryptCipherListResult(number = 1).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = userId,
                folderList = listOf(createMockSdkFolder(number = 1)),
            )
        } returns listOf(createMockFolderView(number = 1)).asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(
                userId = userId,
                collectionList = listOf(createMockSdkCollection(number = 1)),
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
        val domainsFlow = bufferedMutableSharedFlow<SyncResponseJson.Domains>()
        setupVaultDiskSourceFlows(
            ciphersFlow = ciphersFlow,
            collectionsFlow = collectionsFlow,
            foldersFlow = foldersFlow,
            sendsFlow = sendsFlow,
            domainsFlow = domainsFlow,
        )

        turbineScope {
            val ciphersStateFlow = vaultSyncManager
                .decryptCipherListResultStateFlow
                .testIn(backgroundScope)
            val collectionsStateFlow = vaultSyncManager.collectionsStateFlow.testIn(backgroundScope)
            val foldersStateFlow = vaultSyncManager.foldersStateFlow.testIn(backgroundScope)
            val sendsStateFlow = vaultSyncManager.sendDataStateFlow.testIn(backgroundScope)
            val domainsStateFlow = vaultSyncManager.domainsStateFlow.testIn(backgroundScope)

            assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
            assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
            assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
            assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
            assertEquals(DataState.Loading, domainsStateFlow.awaitItem())

            ciphersFlow.tryEmit(listOf(createMockCipher(number = 1)))
            collectionsFlow.tryEmit(listOf(createMockCollection(number = 1)))
            foldersFlow.tryEmit(listOf(createMockFolder(number = 1)))
            sendsFlow.tryEmit(listOf(createMockSend(number = 1)))
            domainsFlow.tryEmit(createMockDomains(number = 1))

            // No events received until unlocked
            ciphersStateFlow.expectNoEvents()
            collectionsStateFlow.expectNoEvents()
            foldersStateFlow.expectNoEvents()
            sendsStateFlow.expectNoEvents()
            // Domains does not care about being unlocked
            assertEquals(
                DataState.Loaded(createMockDomainsData(number = 1)),
                domainsStateFlow.awaitItem(),
            )

            setVaultToUnlocked(userId = userId)

            ciphersFlow.tryEmit(listOf(createMockCipher(number = 1)))
            collectionsFlow.tryEmit(listOf(createMockCollection(number = 1)))
            foldersFlow.tryEmit(listOf(createMockFolder(number = 1)))
            sendsFlow.tryEmit(listOf(createMockSend(number = 1)))
            domainsFlow.tryEmit(createMockDomains(number = 1))

            assertEquals(
                DataState.Loaded(createMockDecryptCipherListResult(number = 1)),
                ciphersStateFlow.awaitItem(),
            )
            assertEquals(
                DataState.Loaded(listOf(createMockCollectionView(number = 1))),
                collectionsStateFlow.awaitItem(),
            )
            assertEquals(
                DataState.Loaded(listOf(createMockFolderView(number = 1))),
                foldersStateFlow.awaitItem(),
            )
            assertEquals(
                DataState.Loaded(SendData(listOf(createMockSendView(number = 1)))),
                sendsStateFlow.awaitItem(),
            )
            // Domain data has not changed
            domainsStateFlow.expectNoEvents()

            fakeAuthDiskSource.userState = null

            assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
            assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
            assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
            assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
            assertEquals(DataState.Loading, domainsStateFlow.awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutableVaultStateFlow should clear unlocked data when it does not contain the active user`() =
        runTest {
            fakeAuthDiskSource.userState = UserStateJson(
                activeUserId = "mockId-1",
                accounts = mapOf(
                    "mockId-1" to MOCK_ACCOUNT,
                    "mockId-2" to mockk(),
                ),
            )
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(userId = "mockId-1", status = VaultUnlockData.Status.UNLOCKING),
                VaultUnlockData(userId = "mockId-2", status = VaultUnlockData.Status.UNLOCKED),
            )
            val userId = "mockId-1"
            coEvery {
                vaultSdkSource.decryptCipherListWithFailures(
                    userId = userId,
                    cipherList = listOf(createMockSdkCipher(number = 1, clock = clock)),
                )
            } returns createMockDecryptCipherListResult(number = 1).asSuccess()
            coEvery {
                vaultSdkSource.decryptFolderList(
                    userId = userId,
                    folderList = listOf(createMockSdkFolder(number = 1)),
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
            val domainsFlow = bufferedMutableSharedFlow<SyncResponseJson.Domains>()
            setupVaultDiskSourceFlows(
                ciphersFlow = ciphersFlow,
                collectionsFlow = collectionsFlow,
                foldersFlow = foldersFlow,
                sendsFlow = sendsFlow,
                domainsFlow = domainsFlow,
            )

            turbineScope {
                val ciphersStateFlow = vaultSyncManager
                    .decryptCipherListResultStateFlow
                    .testIn(backgroundScope)
                val collectionsStateFlow = vaultSyncManager
                    .collectionsStateFlow
                    .testIn(backgroundScope)
                val foldersStateFlow = vaultSyncManager.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultSyncManager.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultSyncManager.domainsStateFlow.testIn(backgroundScope)

                assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
                assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
                assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
                assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
                assertEquals(DataState.Loading, domainsStateFlow.awaitItem())

                ciphersFlow.tryEmit(listOf(createMockCipher(number = 1)))
                collectionsFlow.tryEmit(listOf(createMockCollection(number = 1)))
                foldersFlow.tryEmit(listOf(createMockFolder(number = 1)))
                sendsFlow.tryEmit(listOf(createMockSend(number = 1)))
                domainsFlow.tryEmit(createMockDomains(number = 1))

                // No events received until unlocked
                ciphersStateFlow.expectNoEvents()
                collectionsStateFlow.expectNoEvents()
                foldersStateFlow.expectNoEvents()
                sendsStateFlow.expectNoEvents()
                // Domains does not care about being unlocked
                assertEquals(
                    DataState.Loaded(createMockDomainsData(number = 1)),
                    domainsStateFlow.awaitItem(),
                )
                setVaultToUnlocked(userId = userId)

                assertEquals(
                    DataState.Loaded(createMockDecryptCipherListResult(number = 1)),
                    ciphersStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(listOf(createMockCollectionView(number = 1))),
                    collectionsStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(listOf(createMockFolderView(number = 1))),
                    foldersStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(SendData(listOf(createMockSendView(number = 1)))),
                    sendsStateFlow.awaitItem(),
                )
                // Domain data has not changed
                domainsStateFlow.expectNoEvents()

                mutableVaultStateFlow.value = listOf(
                    VaultUnlockData(userId = "mockId-2", status = VaultUnlockData.Status.UNLOCKED),
                )

                assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
                assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
                assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
                assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
                domainsStateFlow.expectNoEvents()
            }
        }

    @Test
    fun `ciphersStateFlow should emit decrypted list of ciphers when decryptCipherList succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherList = listOf(createMockCipher(number = 1))
            val mockEncryptedCipherList = mockCipherList.toEncryptedSdkCipherList()
            val mockDecryptCipherListResult = createMockDecryptCipherListResult(number = 1)
            val mutableCiphersStateFlow =
                bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>(replay = 1)
            setupVaultDiskSourceFlows(ciphersFlow = mutableCiphersStateFlow)
            coEvery {
                vaultSdkSource.decryptCipherListWithFailures(
                    userId = userId,
                    cipherList = mockEncryptedCipherList,
                )
            } returns mockDecryptCipherListResult.asSuccess()

            vaultSyncManager
                .decryptCipherListResultStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableCiphersStateFlow.tryEmit(mockCipherList)

                    // No additional emissions until vault is unlocked
                    expectNoEvents()
                    setVaultToUnlocked(userId = userId)

                    assertEquals(DataState.Loaded(mockDecryptCipherListResult), awaitItem())
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
        setupVaultDiskSourceFlows(ciphersFlow = mutableCiphersStateFlow)
        coEvery {
            vaultSdkSource.decryptCipherListWithFailures(
                userId = userId,
                cipherList = mockEncryptedCipherList,
            )
        } returns throwable.asFailure()

        vaultSyncManager
            .decryptCipherListResultStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableCiphersStateFlow.tryEmit(mockCipherList)

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

                assertEquals(DataState.Error<DecryptCipherListResult>(throwable), awaitItem())
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
            setupVaultDiskSourceFlows(collectionsFlow = mutableCollectionsStateFlow)
            coEvery {
                vaultSdkSource.decryptCollectionList(
                    userId = userId,
                    collectionList = mockEncryptedCollectionList,
                )
            } returns mockCollectionViewList.asSuccess()

            vaultSyncManager
                .collectionsStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableCollectionsStateFlow.tryEmit(mockCollectionList)

                    // No additional emissions until vault is unlocked
                    expectNoEvents()
                    setVaultToUnlocked(userId = userId)

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
        setupVaultDiskSourceFlows(collectionsFlow = mutableCollectionStateFlow)
        coEvery {
            vaultSdkSource.decryptCollectionList(
                userId = userId,
                collectionList = mockEncryptedCollectionList,
            )
        } returns throwable.asFailure()

        vaultSyncManager
            .collectionsStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableCollectionStateFlow.tryEmit(mockCollectionList)

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

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
            setupVaultDiskSourceFlows(foldersFlow = mutableFoldersStateFlow)
            coEvery {
                vaultSdkSource.decryptFolderList(
                    userId = userId,
                    folderList = mockEncryptedFolderList,
                )
            } returns mockFolderViewList.asSuccess()

            vaultSyncManager
                .foldersStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableFoldersStateFlow.tryEmit(mockFolderList)

                    // No additional emissions until vault is unlocked
                    expectNoEvents()
                    setVaultToUnlocked(userId = userId)

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
        setupVaultDiskSourceFlows(foldersFlow = mutableFoldersStateFlow)
        coEvery {
            vaultSdkSource.decryptFolderList(userId = userId, folderList = mockEncryptedFolderList)
        } returns throwable.asFailure()

        vaultSyncManager
            .foldersStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableFoldersStateFlow.tryEmit(mockFolderList)

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

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
            setupVaultDiskSourceFlows(sendsFlow = mutableSendsStateFlow)
            coEvery {
                vaultSdkSource.decryptSendList(userId = userId, sendList = mockEncryptedSendList)
            } returns mockSendViewList.asSuccess()

            vaultSyncManager
                .sendDataStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    mutableSendsStateFlow.tryEmit(mockSendList)

                    // No additional emissions until vault is unlocked
                    expectNoEvents()
                    setVaultToUnlocked(userId = userId)

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
        setupVaultDiskSourceFlows(sendsFlow = mutableSendsStateFlow)
        coEvery {
            vaultSdkSource.decryptSendList(userId = userId, sendList = mockEncryptedSendList)
        } returns throwable.asFailure()

        vaultSyncManager
            .sendDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendsStateFlow.tryEmit(mockSendList)

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

                assertEquals(DataState.Error<SendData>(throwable), awaitItem())
            }
    }

    @Test
    fun `databaseSchemeChangeFlow should trigger sync on emission`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } just awaits

        mutableDatabaseSchemeChangeFlow.tryEmit(Unit)

        coVerify(exactly = 1) { syncService.sync() }
    }

    @Test
    fun `sync with forced should skip checks and call the syncService sync`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns Throwable("failure").asFailure()

        vaultSyncManager.sync(forced = true)

        coVerify(exactly = 0) {
            syncService.getAccountRevisionDateMillis()
        }
        coVerify(exactly = 1) {
            syncService.sync()
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
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(userId = userId, vault = mockSyncResponse)
            } just runs
            every {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            } just runs

            vaultSyncManager.sync()

            val updatedUserState = MOCK_USER_STATE.copy(
                accounts = mapOf(
                    userId to MOCK_ACCOUNT.copy(
                        profile = MOCK_PROFILE.copy(
                            avatarColorHex = "mockAvatarColor-1",
                            stamp = "mockSecurityStamp-1",
                        ),
                    ),
                ),
            )
            fakeAuthDiskSource.assertUserState(userState = updatedUserState)
            fakeAuthDiskSource.assertUserKey(userId = userId, userKey = "mockKey-1")
            fakeAuthDiskSource.assertPrivateKey(userId = userId, privateKey = "mockPrivateKey-1")
            fakeAuthDiskSource.assertOrganizationKeys(
                userId = userId,
                organizationKeys = mapOf(userId to "mockKey-1"),
            )
            fakeAuthDiskSource.assertOrganizations(
                userId = userId,
                organizations = listOf(createMockOrganization(number = 1)),
            )
            fakeAuthDiskSource.assertPolicies(
                userId = userId,
                policies = listOf(createMockPolicy(number = 1)),
            )
            fakeAuthDiskSource.assertShouldUseKeyConnector(
                userId = userId,
                shouldUseKeyConnector = false,
            )
            coVerify {
                vaultDiskSource.replaceVaultData(userId = userId, vault = mockSyncResponse)
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with syncService Success with a different security stamp should logout and return early`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(number = 1, securityStamp = "newStamp"),
            )
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()

            vaultSyncManager.sync()

            coVerify(exactly = 1) {
                userLogoutManager.softLogout(userId = userId, reason = LogoutReason.SecurityStamp)
            }

            coVerify(exactly = 0) {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = any(),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            }
        }

    @Test
    fun `sync with syncService Failure should update DataStateFlow with an Error`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val mockException = IllegalStateException("sad")
        coEvery { syncService.sync() } returns mockException.asFailure()

        vaultSyncManager.sync()

        assertEquals(
            DataState.Error<DecryptCipherListResult>(mockException),
            vaultSyncManager.decryptCipherListResultStateFlow.value,
        )
        assertEquals(
            DataState.Error<List<CollectionView>>(mockException),
            vaultSyncManager.collectionsStateFlow.value,
        )
        assertEquals(
            DataState.Error<DomainsData>(mockException),
            vaultSyncManager.domainsStateFlow.value,
        )
        assertEquals(
            DataState.Error<List<FolderView>>(mockException),
            vaultSyncManager.foldersStateFlow.value,
        )
        assertEquals(
            DataState.Error<SendData>(mockException),
            vaultSyncManager.sendDataStateFlow.value,
        )
    }

    @Test
    fun `sync with syncService Failure should update vaultDataStateFlow with an Error`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val mockException = IllegalStateException("sad")
        coEvery { syncService.sync() } returns mockException.asFailure()
        setupVaultDiskSourceFlows()

        vaultSyncManager
            .vaultDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                vaultSyncManager.sync()
                assertEquals(DataState.Error<VaultData>(mockException), awaitItem())
            }
    }

    @Test
    fun `sync with NoNetwork should update DataStateFlows to NoNetwork`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns UnknownHostException().asFailure()

        vaultSyncManager.sync()

        assertEquals(
            DataState.NoNetwork(data = null),
            vaultSyncManager.decryptCipherListResultStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultSyncManager.collectionsStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultSyncManager.domainsStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultSyncManager.foldersStateFlow.value,
        )
        assertEquals(
            DataState.NoNetwork(data = null),
            vaultSyncManager.sendDataStateFlow.value,
        )
    }

    @Test
    fun `sync with NoNetwork should update vaultDataStateFlow to NoNetwork`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns UnknownHostException().asFailure()
        setupVaultDiskSourceFlows()

        vaultSyncManager
            .vaultDataStateFlow
            .test {
                assertEquals(DataState.Loading, awaitItem())
                vaultSyncManager.sync()
                assertEquals(DataState.NoNetwork(data = null), awaitItem())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with NoNetwork data should update sendDataStateFlow to Pending and NoNetwork with data`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            setVaultToUnlocked(userId = userId)
            coEvery { syncService.sync() } returns UnknownHostException().asFailure()
            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = userId,
                    sendList = listOf(createMockSdkSend(1)),
                )
            } returns listOf(createMockSendView(1)).asSuccess()

            vaultSyncManager
                .sendDataStateFlow
                .test {
                    assertEquals(DataState.Loading, awaitItem())
                    sendsFlow.tryEmit(listOf(createMockSend(1)))
                    assertEquals(
                        DataState.Loaded(data = SendData(listOf(createMockSendView(1)))),
                        awaitItem(),
                    )
                    vaultSyncManager.sync()
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

    @Test
    fun `syncIfNecessary when there is no last sync time should sync the vault`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every { settingsDiskSource.getLastSyncTime(userId = userId) } returns null
        coEvery { syncService.sync() } just awaits

        vaultSyncManager.syncIfNecessary()

        coVerify { syncService.sync() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncIfNecessary when the current time is greater than 30 minutes after the last sync time should sync the vault`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every {
            settingsDiskSource.getLastSyncTime(userId = userId)
        } returns clock.instant().minus(31, ChronoUnit.MINUTES)
        coEvery { syncService.sync() } just awaits

        vaultSyncManager.syncIfNecessary()

        coVerify { syncService.sync() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncIfNecessary when the current time is less than 30 minutes after the last sync time should not sync the vault`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every {
            settingsDiskSource.getLastSyncTime(userId = userId)
        } returns clock.instant().minus(29, ChronoUnit.MINUTES)
        coEvery { syncService.sync() } just awaits

        vaultSyncManager.syncIfNecessary()

        coVerify(exactly = 0) { syncService.sync() }
    }

    @Test
    fun `sync when the last sync time is older than the revision date should sync the vault`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every {
            settingsDiskSource.getLastSyncTime(userId = userId)
        } returns clock.instant().minus(1, ChronoUnit.MINUTES)

        coEvery { syncService.sync() } just awaits

        vaultSyncManager.sync()

        coVerify { syncService.sync() }
    }

    @Test
    fun `sync when the last sync time is more recent than the revision date should not sync `() =
        runTest {
            val userId = "mockId-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            every {
                settingsDiskSource.getLastSyncTime(userId = userId)
            } returns clock.instant().plus(2, ChronoUnit.MINUTES)

            vaultSyncManager.sync()

            verify(exactly = 1) {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            }
            coVerify(exactly = 0) { syncService.sync() }
        }

    @Test
    fun `vaultDataStateFlow should return empty when last sync time is populated`() =
        runTest {
            val userId = "mockId-1"
            coEvery { vaultLockManager.waitUntilUnlocked(userId = userId) } just runs
            every { settingsDiskSource.getLastSyncTime(userId = userId) } returns clock.instant()

            mutableVaultStateFlow.update {
                listOf(VaultUnlockData(userId = userId, status = VaultUnlockData.Status.UNLOCKED))
            }
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setupEmptyDecryptionResults()
            setupVaultDiskSourceFlows(
                ciphersFlow = flowOf(emptyList()),
                collectionsFlow = flowOf(emptyList()),
                domainsFlow = flowOf(
                    SyncResponseJson.Domains(
                        globalEquivalentDomains = emptyList(),
                        equivalentDomains = emptyList(),
                    ),
                ),
                foldersFlow = flowOf(emptyList()),
                sendsFlow = flowOf(emptyList()),
            )

            turbineScope {
                val ciphersStateFlow = vaultSyncManager
                    .decryptCipherListResultStateFlow
                    .testIn(backgroundScope)
                val collectionsStateFlow = vaultSyncManager
                    .collectionsStateFlow
                    .testIn(backgroundScope)
                val foldersStateFlow = vaultSyncManager.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultSyncManager.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultSyncManager.domainsStateFlow.testIn(backgroundScope)

                assertEquals(
                    DataState.Loaded(
                        createMockDecryptCipherListResult(
                            number = 1,
                            successes = emptyList(),
                        ),
                    ),
                    ciphersStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(emptyList<CollectionView>()),
                    collectionsStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(emptyList<FolderView>()),
                    foldersStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(SendData(sendViewList = emptyList())),
                    sendsStateFlow.awaitItem(),
                )
                assertEquals(
                    DataState.Loaded(
                        DomainsData(
                            equivalentDomains = emptyList(),
                            globalEquivalentDomains = emptyList(),
                        ),
                    ),
                    domainsStateFlow.awaitItem(),
                )
            }
        }

    @Test
    fun `vaultDataStateFlow should return loading when last sync time is null`() =
        runTest {
            val userId = "mockId-1"
            coEvery { vaultLockManager.waitUntilUnlocked(userId = userId) } just runs
            every { settingsDiskSource.getLastSyncTime(userId = userId) } returns null
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setupEmptyDecryptionResults()
            setupVaultDiskSourceFlows(
                ciphersFlow = flowOf(emptyList()),
                collectionsFlow = flowOf(emptyList()),
                domainsFlow = flowOf(),
                foldersFlow = flowOf(emptyList()),
                sendsFlow = flowOf(emptyList()),
            )
            turbineScope {
                val ciphersStateFlow = vaultSyncManager
                    .decryptCipherListResultStateFlow
                    .testIn(backgroundScope)
                val collectionsStateFlow = vaultSyncManager
                    .collectionsStateFlow
                    .testIn(backgroundScope)
                val foldersStateFlow = vaultSyncManager.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultSyncManager.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultSyncManager.domainsStateFlow.testIn(backgroundScope)

                assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
                assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
                assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
                assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
                assertEquals(DataState.Loading, domainsStateFlow.awaitItem())
            }
        }

    @Test
    fun `fullSyncFlow emission with active user ID should trigger an unforced sync`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } just awaits

        mutableFullSyncFlow.tryEmit(userId)

        coVerify(exactly = 1) { syncService.sync() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `fullSyncFlow emission with non-active user ID should clear last sync time for that user`() {
        val userId = "mockId-2"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        mutableFullSyncFlow.tryEmit(userId)

        coVerify(exactly = 1) {
            settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncForResult should return success result with itemsAvailable = true when sync succeeds and ciphers list is not empty`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = userId,
                    vault = mockSyncResponse,
                )
            } just runs

            every {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            } just runs

            val syncResult = vaultSyncManager.syncForResult()
            assertEquals(SyncVaultDataResult.Success(itemsAvailable = true), syncResult)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncForResult should return success result with itemsAvailable = false when sync succeeds and ciphers list is empty`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1, ciphers = emptyList())
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(number = 1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(userId = userId, vault = mockSyncResponse)
            } just runs

            every {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            } just runs

            val syncResult = vaultSyncManager.syncForResult()
            assertEquals(SyncVaultDataResult.Success(itemsAvailable = false), syncResult)
        }

    @Test
    fun `syncForResult should return error when getAccountRevisionDateMillis fails`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val throwable = Throwable()
            coEvery { syncService.getAccountRevisionDateMillis() } returns throwable.asFailure()
            val syncResult = vaultSyncManager.syncForResult()
            assertEquals(
                SyncVaultDataResult.Error(throwable = throwable),
                syncResult,
            )
        }

    @Test
    fun `syncForResult should return error when sync fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val throwable = Throwable()
        coEvery { syncService.sync() } returns throwable.asFailure()
        val syncResult = vaultSyncManager.syncForResult()
        assertEquals(
            SyncVaultDataResult.Error(throwable = throwable),
            syncResult,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncForResult when the last sync time is more recent than the revision date should return result from disk source data`() =
        runTest {
            val userId = "mockId-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            every {
                settingsDiskSource.getLastSyncTime(userId = userId)
            } returns clock.instant().plus(2, ChronoUnit.MINUTES)
            mutableGetCiphersFlow.update { emptyList() }
            val result = vaultSyncManager.syncForResult()
            assertEquals(
                SyncVaultDataResult.Success(itemsAvailable = false),
                result,
            )
            verify(exactly = 1) {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            }
            coVerify(exactly = 0) { syncService.sync() }
        }

    //region Helper functions

    /**
     * Ensures the vault for the given [userId] is unlocked and can pass any
     * [VaultLockManager.waitUntilUnlocked] or [VaultLockManager.isVaultUnlocked] checks.
     */
    private fun setVaultToUnlocked(userId: String) {
        mutableUnlockedUserIdsStateFlow.update { it + userId }
        mutableVaultStateFlow.tryEmit(
            listOf(VaultUnlockData(userId, VaultUnlockData.Status.UNLOCKED)),
        )
    }

    /**
     * Helper setup all flows required to properly subscribe to the
     * [VaultSyncManager.vaultDataStateFlow].
     */
    @Suppress("LongParameterList")
    private fun setupVaultDiskSourceFlows(
        userId: String = MOCK_USER_STATE.activeUserId,
        ciphersFlow: Flow<List<SyncResponseJson.Cipher>> = bufferedMutableSharedFlow(),
        collectionsFlow: Flow<List<SyncResponseJson.Collection>> = bufferedMutableSharedFlow(),
        domainsFlow: Flow<SyncResponseJson.Domains> = bufferedMutableSharedFlow(),
        foldersFlow: Flow<List<SyncResponseJson.Folder>> = bufferedMutableSharedFlow(),
        sendsFlow: Flow<List<SyncResponseJson.Send>> = bufferedMutableSharedFlow(),
    ) {
        coEvery { vaultDiskSource.getCiphersFlow(userId = userId) } returns ciphersFlow
        coEvery { vaultDiskSource.getCollections(userId = userId) } returns collectionsFlow
        coEvery { vaultDiskSource.getDomains(userId = userId) } returns domainsFlow
        coEvery { vaultDiskSource.getFolders(userId = userId) } returns foldersFlow
        coEvery { vaultDiskSource.getSends(userId = userId) } returns sendsFlow
    }

    private fun setupEmptyDecryptionResults(
        userId: String = MOCK_USER_STATE.activeUserId,
    ) {
        coEvery {
            vaultSdkSource.decryptCipherListWithFailures(userId = userId, cipherList = emptyList())
        } returns createMockDecryptCipherListResult(number = 1, successes = emptyList()).asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(userId = userId, folderList = emptyList())
        } returns emptyList<FolderView>().asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(userId = userId, collectionList = emptyList())
        } returns emptyList<CollectionView>().asSuccess()
        coEvery {
            vaultSdkSource.decryptSendList(userId = userId, sendList = emptyList())
        } returns emptyList<SendView>().asSuccess()
    }
    //endregion Helper functions
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = "mockSecurityStamp-1",
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
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
