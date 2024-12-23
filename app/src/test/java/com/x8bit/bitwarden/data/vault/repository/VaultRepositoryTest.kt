package com.x8bit.bitwarden.data.vault.repository

import android.net.Uri
import android.util.Base64
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.bitwarden.core.DateTime
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateFileSendResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateSendJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.FolderJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateFolderResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCollection
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockDomains
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFileSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganizationKeys
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockProfile
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSend
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.FolderService
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
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.model.createMockDomainsData
import com.x8bit.bitwarden.data.vault.repository.util.toDomainsData
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import java.io.File
import java.net.UnknownHostException
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Suppress("LargeClass")
class VaultRepositoryTest {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val userLogoutManager: UserLogoutManager = mockk {
        every { softLogout(any(), any()) } just runs
    }
    private val fileManager: FileManager = mockk {
        coEvery { delete(*anyVararg()) } just runs
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val settingsDiskSource = mockk<SettingsDiskSource> {
        every { getLastSyncTime(userId = any()) } returns clock.instant()
        every { storeLastSyncTime(userId = any(), lastSyncTime = any()) } just runs
    }
    private val syncService: SyncService = mockk {
        coEvery {
            getAccountRevisionDateMillis()
        } returns clock.instant().plus(1, ChronoUnit.MINUTES).toEpochMilli().asSuccess()
    }
    private val sendsService: SendsService = mockk()
    private val ciphersService: CiphersService = mockk()
    private val folderService: FolderService = mockk()
    private val mutableGetCiphersFlow: MutableStateFlow<List<SyncResponseJson.Cipher>> =
        MutableStateFlow(listOf(createMockCipher(1)))
    private val vaultDiskSource: VaultDiskSource = mockk {
        coEvery { resyncVaultData(any()) } just runs
        every { getCiphers(any()) } returns mutableGetCiphersFlow
    }
    private val totpCodeManager: TotpCodeManager = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val mutableVaultStateFlow = MutableStateFlow<List<VaultUnlockData>>(
        emptyList(),
    )
    private val mutableUnlockedUserIdsStateFlow = MutableStateFlow<Set<String>>(emptySet())
    private val cipherManager: CipherManager = mockk()
    private val vaultLockManager: VaultLockManager = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultStateFlow
        every {
            isVaultUnlocked(any())
        } answers { call ->
            val userId = call.invocation.args.first()
            userId in mutableUnlockedUserIdsStateFlow.value
        }
        every { isVaultUnlocking(any()) } returns false
        every { lockVault(any()) } just runs
        every { lockVaultForCurrentUser() } just runs
        coEvery {
            waitUntilUnlocked(any())
        } coAnswers { call ->
            val userId = call.invocation.args.first()
            mutableUnlockedUserIdsStateFlow.first { userId in it }
        }
    }
    private val mutableDatabaseSchemeChangeFlow = bufferedMutableSharedFlow<Unit>()
    private val databaseSchemeManager: DatabaseSchemeManager = mockk {
        every { databaseSchemeChangeFlow } returns mutableDatabaseSchemeChangeFlow
    }
    private val reviewPromptManager: ReviewPromptManager = mockk {
        every { registerCreateSendAction() } just runs
    }

    private val mutableFullSyncFlow = bufferedMutableSharedFlow<Unit>()
    private val mutableSyncCipherDeleteFlow = bufferedMutableSharedFlow<SyncCipherDeleteData>()
    private val mutableSyncCipherUpsertFlow = bufferedMutableSharedFlow<SyncCipherUpsertData>()
    private val mutableSyncSendDeleteFlow = bufferedMutableSharedFlow<SyncSendDeleteData>()
    private val mutableSyncSendUpsertFlow = bufferedMutableSharedFlow<SyncSendUpsertData>()
    private val mutableSyncFolderDeleteFlow = bufferedMutableSharedFlow<SyncFolderDeleteData>()
    private val mutableSyncFolderUpsertFlow = bufferedMutableSharedFlow<SyncFolderUpsertData>()
    private val pushManager: PushManager = mockk {
        every { fullSyncFlow } returns mutableFullSyncFlow
        every { syncCipherDeleteFlow } returns mutableSyncCipherDeleteFlow
        every { syncCipherUpsertFlow } returns mutableSyncCipherUpsertFlow
        every { syncSendDeleteFlow } returns mutableSyncSendDeleteFlow
        every { syncSendUpsertFlow } returns mutableSyncSendUpsertFlow
        every { syncFolderDeleteFlow } returns mutableSyncFolderDeleteFlow
        every { syncFolderUpsertFlow } returns mutableSyncFolderUpsertFlow
    }

    private val vaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        sendsService = sendsService,
        ciphersService = ciphersService,
        folderService = folderService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = settingsDiskSource,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        totpCodeManager = totpCodeManager,
        pushManager = pushManager,
        cipherManager = cipherManager,
        fileManager = fileManager,
        clock = clock,
        userLogoutManager = userLogoutManager,
        databaseSchemeManager = databaseSchemeManager,
        reviewPromptManager = reviewPromptManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(SyncResponseJson.Domains::toDomainsData)
        mockkStatic(Uri::class)
        mockkStatic(MessageDigest::class)
        mockkStatic(Base64::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SyncResponseJson.Domains::toDomainsData)
        unmockkStatic(Uri::class)
        unmockkStatic(MessageDigest::class)
        unmockkStatic(Base64::class)
    }

    @Test
    fun `userSwitchingChangesFlow should cancel any pending sync call`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } just awaits

        vaultRepository.sync()
        vaultRepository.sync()
        coVerify(exactly = 1) {
            // Despite being called twice, we only allow 1 sync
            syncService.sync()
        }

        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = "mockId-2",
            accounts = mapOf("mockId-2" to mockk()),
        )
        vaultRepository.sync()
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
            vaultSdkSource.decryptCipherList(
                userId = userId,
                cipherList = listOf(createMockSdkCipher(1, clock)),
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
        val domainsFlow = bufferedMutableSharedFlow<SyncResponseJson.Domains>()
        setupVaultDiskSourceFlows(
            ciphersFlow = ciphersFlow,
            collectionsFlow = collectionsFlow,
            foldersFlow = foldersFlow,
            sendsFlow = sendsFlow,
            domainsFlow = domainsFlow,
        )

        turbineScope {
            val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
            val collectionsStateFlow = vaultRepository.collectionsStateFlow.testIn(backgroundScope)
            val foldersStateFlow = vaultRepository.foldersStateFlow.testIn(backgroundScope)
            val sendsStateFlow = vaultRepository.sendDataStateFlow.testIn(backgroundScope)
            val domainsStateFlow = vaultRepository.domainsStateFlow.testIn(backgroundScope)

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
                DataState.Loaded(listOf(createMockCipherView(number = 1))),
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
                vaultSdkSource.decryptCipherList(
                    userId = userId,
                    cipherList = listOf(createMockSdkCipher(1, clock)),
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
            val domainsFlow = bufferedMutableSharedFlow<SyncResponseJson.Domains>()
            setupVaultDiskSourceFlows(
                ciphersFlow = ciphersFlow,
                collectionsFlow = collectionsFlow,
                foldersFlow = foldersFlow,
                sendsFlow = sendsFlow,
                domainsFlow = domainsFlow,
            )

            turbineScope {
                val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
                val collectionsStateFlow =
                    vaultRepository.collectionsStateFlow.testIn(backgroundScope)
                val foldersStateFlow = vaultRepository.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultRepository.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultRepository.domainsStateFlow.testIn(backgroundScope)

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
                    DataState.Loaded(listOf(createMockCipherView(number = 1))),
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

                    // No additional emissions until vault is unlocked
                    expectNoEvents()
                    setVaultToUnlocked(userId = userId)

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

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

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

                // No additional emissions until vault is unlocked
                expectNoEvents()
                setVaultToUnlocked(userId = userId)

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

        vaultRepository.sync(forced = true)

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
            every {
                settingsDiskSource.storeLastSyncTime(MOCK_USER_STATE.activeUserId, clock.instant())
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
            fakeAuthDiskSource.assertPolicies(
                userId = "mockId-1",
                policies = listOf(createMockPolicy(number = 1)),
            )
            fakeAuthDiskSource.assertShouldUseKeyConnector(
                userId = "mockId-1",
                shouldUseKeyConnector = false,
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

    @Suppress("MaxLineLength")
    @Test
    fun `sync with syncService Success with a different security stamp should logout and return early`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse
                .copy(profile = createMockProfile(number = 1).copy(securityStamp = "newStamp"))
                .asSuccess()

            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()

            vaultRepository.sync()

            coVerify {
                userLogoutManager.softLogout(userId = userId, isExpired = true)
            }

            coVerify(exactly = 0) {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = any(),
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
            DataState.Error<DomainsData>(mockException),
            vaultRepository.domainsStateFlow.value,
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
            vaultRepository.domainsStateFlow.value,
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

    @Test
    fun `syncIfNecessary when there is no last sync time should sync the vault`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every {
            settingsDiskSource.getLastSyncTime(userId = userId)
        } returns null
        coEvery { syncService.sync() } just awaits

        vaultRepository.syncIfNecessary()

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

        vaultRepository.syncIfNecessary()

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

        vaultRepository.syncIfNecessary()

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

        vaultRepository.sync()

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

            vaultRepository.sync()

            verify(exactly = 1) {
                settingsDiskSource.storeLastSyncTime(
                    userId = userId,
                    lastSyncTime = clock.instant(),
                )
            }
            coVerify(exactly = 0) { syncService.sync() }
        }

    @Test
    fun `lockVaultForCurrentUser should delegate to the VaultLockManager`() {
        vaultRepository.lockVaultForCurrentUser()
        verify { vaultLockManager.lockVaultForCurrentUser() }
    }

    @Test
    fun `unlockVaultWithBiometrics with missing user state should return InvalidStateError`() =
        runTest {
            fakeAuthDiskSource.userState = null
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )

            val result = vaultRepository.unlockVaultWithBiometrics()

            assertEquals(VaultUnlockResult.InvalidStateError, result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Test
    fun `unlockVaultWithBiometrics with missing biometrics key should return InvalidStateError`() =
        runTest {
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = MOCK_USER_STATE.activeUserId
            fakeAuthDiskSource.storeUserBiometricUnlockKey(userId = userId, biometricsKey = null)

            val result = vaultRepository.unlockVaultWithBiometrics()

            assertEquals(VaultUnlockResult.InvalidStateError, result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with VaultLockManager Success and no encrypted PIN should unlock for the current user and return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val privateKey = "mockPrivateKey-1"
            val biometricsKey = "asdf1234"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeUserBiometricUnlockKey(userId = userId, biometricsKey = biometricsKey)
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithBiometrics()

            assertEquals(VaultUnlockResult.Success, result)
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
            }
            coVerify(exactly = 0) {
                vaultSdkSource.derivePinProtectedUserKey(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithBiometrics with VaultLockManager Success and a stored encrypted pin should unlock for the current user, derive a new pin-protected key, and return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val encryptedPin = "encryptedPin"
            val privateKey = "mockPrivateKey-1"
            val pinProtectedUserKey = "pinProtectedUserkey"
            val biometricsKey = "asdf1234"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.derivePinProtectedUserKey(
                    userId = userId,
                    encryptedPin = encryptedPin,
                )
            } returns pinProtectedUserKey.asSuccess()
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeUserBiometricUnlockKey(userId = userId, biometricsKey = biometricsKey)
                storePrivateKey(userId = userId, privateKey = privateKey)
                storeEncryptedPin(userId = userId, encryptedPin = encryptedPin)
                storePinProtectedUserKey(
                    userId = userId,
                    pinProtectedUserKey = null,
                    inMemoryOnly = true,
                )
            }

            val result = vaultRepository.unlockVaultWithBiometrics()

            assertEquals(VaultUnlockResult.Success, result)
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = pinProtectedUserKey,
                inMemoryOnly = true,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = biometricsKey,
                    ),
                    organizationKeys = null,
                )
            }
            coEvery {
                vaultSdkSource.derivePinProtectedUserKey(
                    userId = userId,
                    encryptedPin = encryptedPin,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `unlockVaultWithDecryptedUserKey with VaultLockManager Success should return Success`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val authenticatorSyncUnlockKey = "asdf1234"
            val privateKey = "mockPrivateKey-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            fakeAuthDiskSource.apply {
                storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = authenticatorSyncUnlockKey,
                )
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithDecryptedUserKey(
                userId = userId,
                decryptedUserKey = authenticatorSyncUnlockKey,
            )
            assertEquals(VaultUnlockResult.Success, result)
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `unlockVaultWithDecryptedUserKey with VaultLockManager InvalidStateError should return InvalidStateError`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val authenticatorSyncUnlockKey = "asdf1234"
            val privateKey = "mockPrivateKey-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.InvalidStateError
            fakeAuthDiskSource.apply {
                storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = authenticatorSyncUnlockKey,
                )
                storePrivateKey(userId = userId, privateKey = privateKey)
            }

            val result = vaultRepository.unlockVaultWithDecryptedUserKey(
                userId = userId,
                decryptedUserKey = authenticatorSyncUnlockKey,
            )
            assertEquals(VaultUnlockResult.InvalidStateError, result)
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = authenticatorSyncUnlockKey,
                    ),
                    organizationKeys = null,
                )
            }
        }

    @Test
    fun `unlockVaultWithMasterPassword with missing user state should return InvalidStateError`() =
        runTest {
            fakeAuthDiskSource.userState = null
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )

            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")

            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Test
    fun `unlockVaultWithMasterPassword with missing user key should return InvalidStateError`() =
        runTest {
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )

            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")
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
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with missing private key should return InvalidStateError`() =
        runTest {
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
            val result = vaultRepository.unlockVaultWithMasterPassword(masterPassword = "")
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
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with VaultLockManager Success and no encrypted PIN should unlock for the current user and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            coEvery {
                vaultSdkSource.derivePinProtectedUserKey(any(), any())
            } returns "pinProtectedUserKey".asSuccess()
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)
            fakeAuthDiskSource.apply {
                storeEncryptedPin(
                    userId = userId,
                    encryptedPin = null,
                )
                storePinProtectedUserKey(
                    userId = userId,
                    pinProtectedUserKey = null,
                    inMemoryOnly = true,
                )
            }

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
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
            coVerify(exactly = 0) { vaultSdkSource.derivePinProtectedUserKey(any(), any()) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with VaultLockManager Success and a stored encrypted pin should unlock for the current user, derive a new pin-protected key, and return Success`() =
        runTest {
            val userId = "mockId-1"
            val encryptedPin = "encryptedPin"
            val pinProtectedUserKey = "pinProtectedUserkey"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            coEvery {
                vaultSdkSource.derivePinProtectedUserKey(
                    userId = userId,
                    encryptedPin = encryptedPin,
                )
            } returns pinProtectedUserKey.asSuccess()
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)
            fakeAuthDiskSource.apply {
                storeEncryptedPin(
                    userId = userId,
                    encryptedPin = encryptedPin,
                )
                storePinProtectedUserKey(
                    userId = userId,
                    pinProtectedUserKey = null,
                    inMemoryOnly = true,
                )
            }

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            fakeAuthDiskSource.assertPinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = pinProtectedUserKey,
                inMemoryOnly = true,
            )
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
            coEvery {
                vaultSdkSource.derivePinProtectedUserKey(
                    userId = userId,
                    encryptedPin = encryptedPin,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithMasterPassword with VaultLockManager non-Success should unlock for the current user and return the error`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.InvalidStateError
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithMasterPassword(
                masterPassword = "mockPassword-1",
            )

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
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
    fun `unlockVaultWithPin with missing user state should return InvalidStateError`() = runTest {
        fakeAuthDiskSource.userState = null
        assertEquals(
            emptyList<VaultUnlockData>(),
            vaultRepository.vaultUnlockDataStateFlow.value,
        )

        val result = vaultRepository.unlockVaultWithPin(pin = "1234")

        assertEquals(
            VaultUnlockResult.InvalidStateError,
            result,
        )
        assertEquals(
            emptyList<VaultUnlockData>(),
            vaultRepository.vaultUnlockDataStateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with missing pin-protected user key should return InvalidStateError`() =
        runTest {
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = "mockId-1",
                pinProtectedUserKey = null,
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
                emptyList<VaultUnlockData>(),
                vaultRepository.vaultUnlockDataStateFlow.value,
            )
        }

    @Test
    fun `unlockVaultWithPin with missing private key should return InvalidStateError`() = runTest {
        assertEquals(
            emptyList<VaultUnlockData>(),
            vaultRepository.vaultUnlockDataStateFlow.value,
        )
        val result = vaultRepository.unlockVaultWithPin(pin = "1234")
        fakeAuthDiskSource.storePinProtectedUserKey(
            userId = "mockId-1",
            pinProtectedUserKey = "mockKey-1",
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
            emptyList<VaultUnlockData>(),
            vaultRepository.vaultUnlockDataStateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with VaultLockManager Success should unlock for the current user and return Success`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.Success
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Pin(
                        pin = "1234",
                        pinProtectedUserKey = "mockKey-1",
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultWithPin with VaultLockManager non-Success should unlock for the current user and return the error`() =
        runTest {
            val userId = "mockId-1"
            val mockVaultUnlockResult = VaultUnlockResult.InvalidStateError
            prepareStateForUnlocking(unlockResult = mockVaultUnlockResult)

            val result = vaultRepository.unlockVaultWithPin(pin = "1234")

            assertEquals(
                mockVaultUnlockResult,
                result,
            )
            coVerify {
                vaultLockManager.unlockVault(
                    userId = userId,
                    kdf = MOCK_PROFILE.toSdkParams(),
                    email = "email",
                    privateKey = "mockPrivateKey-1",
                    initUserCryptoMethod = InitUserCryptoMethod.Pin(
                        pin = "1234",
                        pinProtectedUserKey = "mockKey-1",
                    ),
                    organizationKeys = createMockOrganizationKeys(number = 1),
                )
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
    fun `vaultDataStateFlow should return empty when last sync time is populated`() =
        runTest {
            val userId = "mockId-1"
            coEvery {
                vaultLockManager.waitUntilUnlocked(userId = userId)
            } just runs
            every {
                settingsDiskSource.getLastSyncTime(userId = userId)
            } returns clock.instant()

            mutableVaultStateFlow.update {
                listOf(VaultUnlockData(userId, VaultUnlockData.Status.UNLOCKED))
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
                val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
                val collectionsStateFlow =
                    vaultRepository.collectionsStateFlow.testIn(backgroundScope)
                val foldersStateFlow = vaultRepository.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultRepository.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultRepository.domainsStateFlow.testIn(backgroundScope)

                assertEquals(
                    DataState.Loaded(emptyList<CipherView>()),
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
            coEvery {
                vaultLockManager.waitUntilUnlocked(userId = userId)
            } just runs
            every {
                settingsDiskSource.getLastSyncTime(userId = userId)
            } returns null
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
                val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
                val collectionsStateFlow =
                    vaultRepository.collectionsStateFlow.testIn(backgroundScope)
                val foldersStateFlow = vaultRepository.foldersStateFlow.testIn(backgroundScope)
                val sendsStateFlow = vaultRepository.sendDataStateFlow.testIn(backgroundScope)
                val domainsStateFlow = vaultRepository.domainsStateFlow.testIn(backgroundScope)

                assertEquals(DataState.Loading, ciphersStateFlow.awaitItem())
                assertEquals(DataState.Loading, collectionsStateFlow.awaitItem())
                assertEquals(DataState.Loading, foldersStateFlow.awaitItem())
                assertEquals(DataState.Loading, sendsStateFlow.awaitItem())
                assertEquals(DataState.Loading, domainsStateFlow.awaitItem())
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

            // No additional emissions until vault is unlocked
            expectNoEvents()
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)

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
    fun `createSend with no active user should return CreateSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.createSend(
                sendView = mockk(),
                fileUri = mockk(),
            )

            assertEquals(
                CreateSendResult.Error(message = null),
                result,
            )
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

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Error(message = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with TEXT and sendsService createTextSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            coEvery {
                sendsService.createTextSend(
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Error(IllegalStateException().message), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createSend with TEXT and sendsService createTextSend success should return CreateSendResult success and increment send action count`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            val mockSdkSend = createMockSdkSend(number = 1, type = SendType.TEXT)
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            val mockSendViewResult = createMockSendView(number = 2)
            val sendTextResponse = CreateSendJsonResponse.Success(send = mockSend)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery {
                sendsService.createTextSend(
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns sendTextResponse.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId, mockSend) } just runs
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendViewResult.asSuccess()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Success(mockSendViewResult), result)

            verify(exactly = 1) { reviewPromptManager.registerCreateSendAction() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService createFileSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val uri = setupMockUri(url = "www.test.com")
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { length() } returns 1
                every { absolutePath } returns "mockAbsolutePath"
            }
            val encryptedFile = mockk<File> {
                every { length() } returns 1
                every { absolutePath } returns "mockAbsolutePath"
            }
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(IllegalStateException().message), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService uploadFile failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
                every { length() } returns 1
            }
            val encryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
                every { length() } returns 1
            }
            val sendFileResponse = CreateFileSendResponse.Success(
                createMockFileSendResponseJson(
                    number = 1,
                    type = SendTypeJson.FILE,
                ),
            )
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendView.asSuccess()
            every { fileManager.filesDirectory } returns "mockFilesDirectory"
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            coEvery {
                vaultDiskSource.saveSend(
                    userId,
                    sendFileResponse.createFileJsonResponse.sendResponse,
                )
            } just runs
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns sendFileResponse.asSuccess()
            coEvery {
                sendsService.uploadFile(
                    sendFileResponse = sendFileResponse.createFileJsonResponse,
                    encryptedFile = encryptedFile,
                )
            } returns Throwable().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and fileManager uriToByteArray failure should return CreateSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery { fileManager.writeUriToCache(any()) } returns Throwable().asFailure()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(message = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService uploadFile success should return CreateSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
            }
            val encryptedFile = mockk<File> {
                every { length() } returns 1
            }
            val sendFileResponse = CreateFileSendResponse.Success(
                createMockFileSendResponseJson(number = 1),
            )
            val mockSendViewResult = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()

            every { fileManager.filesDirectory } returns "mockFilesDirectory"
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns sendFileResponse.asSuccess()
            coEvery {
                sendsService.uploadFile(
                    sendFileResponse = sendFileResponse.createFileJsonResponse,
                    encryptedFile = encryptedFile,
                )
            } returns sendFileResponse.createFileJsonResponse.sendResponse.asSuccess()
            coEvery {
                vaultDiskSource.saveSend(
                    userId,
                    sendFileResponse.createFileJsonResponse.sendResponse,
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendViewResult.asSuccess()

            val result = vaultRepository.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Success(mockSendViewResult), result)
        }

    @Test
    fun `updateSend with no active user should return UpdateSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.updateSend(
                sendId = "sendId",
                sendView = mockk(),
            )

            assertEquals(
                UpdateSendResult.Error(null),
                result,
            )
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
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
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
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
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
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(
                    userId = userId, send = createMockSdkSend(number = 1, type = SendType.TEXT),
                )
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
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            val mockSendViewResult = createMockSendView(number = 2, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.decryptSend(
                    userId = userId,
                    send = createMockSdkSend(number = 1, type = SendType.TEXT),
                )
            } returns mockSendViewResult.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = vaultRepository.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Success(mockSendViewResult), result)
        }

    @Test
    fun `removePasswordSend with no active user should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.removePasswordSend(
                sendId = "sendId",
            )

            assertEquals(
                RemovePasswordSendResult.Error(null),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Error should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendId = "sendId1234"
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

    @Test
    fun `deleteSend with no active user should return DeleteSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.deleteSend(
                sendId = "sendId",
            )

            assertEquals(
                DeleteSendResult.Error,
                result,
            )
        }

    @Test
    fun `deleteSend with sendsService deleteSend failure should return DeleteSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendId = "mockId-1"
            coEvery {
                sendsService.deleteSend(sendId = sendId)
            } returns Throwable("Fail").asFailure()

            val result = vaultRepository.deleteSend(sendId)

            assertEquals(DeleteSendResult.Error, result)
        }

    @Test
    fun `deleteSend with sendsService deleteSend success should return DeleteSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "mockId-1"
            coEvery { sendsService.deleteSend(sendId = sendId) } returns Unit.asSuccess()
            coEvery { vaultDiskSource.deleteSend(userId, sendId) } just runs

            val result = vaultRepository.deleteSend(sendId)

            assertEquals(DeleteSendResult.Success, result)
        }

    @Test
    fun `generateTotp with no active user should return GenerateTotpResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.generateTotp(
                totpCode = "totpCode",
                time = DateTime.now(),
            )

            assertEquals(
                GenerateTotpResult.Error,
                result,
            )
        }

    @Test
    fun `generateTotp should return a success result on getting a code`() = runTest {
        val totpResponse = TotpResponse("Testcode", 30u)
        coEvery {
            vaultSdkSource.generateTotp(any(), any(), any())
        } returns totpResponse.asSuccess()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val result = vaultRepository.generateTotp(
            totpCode = "testCode",
            time = DateTime.now(),
        )

        assertEquals(
            GenerateTotpResult.Success(
                code = totpResponse.code,
                periodSeconds = totpResponse.period.toInt(),
            ),
            result,
        )
    }

    @Test
    fun `deleteFolder with no active user should return DeleteFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.deleteFolder("Test")

            assertEquals(
                DeleteFolderResult.Error,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteFolder with folderService Delete failure should return DeleteFolderResult Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "mockId-1"
            coEvery { folderService.deleteFolder(folderId) } returns Throwable("fail").asFailure()

            val result = vaultRepository.deleteFolder(folderId)
            assertEquals(DeleteFolderResult.Error, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteFolder with folderService Delete success should return DeleteFolderResult Success and update ciphers`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "mockFolderId-1"
            coEvery { folderService.deleteFolder(folderId) } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.deleteFolder(
                    MOCK_USER_STATE.activeUserId,
                    folderId,
                )
            } just runs

            val mockCipher = createMockCipher(1)

            val mutableCiphersStateFlow =
                MutableStateFlow(
                    listOf(
                        mockCipher,
                        createMockCipher(2),
                    ),
                )

            coEvery {
                vaultDiskSource.getCiphers(MOCK_USER_STATE.activeUserId)
            } returns mutableCiphersStateFlow

            coEvery {
                vaultDiskSource.saveCipher(
                    MOCK_USER_STATE.activeUserId,
                    mockCipher.copy(
                        folderId = null,
                    ),
                )
            } just runs

            val result = vaultRepository.deleteFolder(folderId)

            coVerify(exactly = 1) {
                vaultDiskSource.saveCipher(
                    MOCK_USER_STATE.activeUserId,
                    mockCipher.copy(
                        folderId = null,
                    ),
                )
            }

            assertEquals(DeleteFolderResult.Success, result)
        }

    @Test
    fun `createFolder with no active user should return CreateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.createFolder(mockk())

            assertEquals(
                CreateFolderResult.Error,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFolder with folderService Delete failure should return DeleteFolderResult Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "mockId-1"
            coEvery { folderService.deleteFolder(folderId) } returns Throwable("fail").asFailure()

            val result = vaultRepository.deleteFolder(folderId)
            assertEquals(DeleteFolderResult.Error, result)
        }

    @Test
    fun `createFolder with encryptFolder failure should return CreateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = null,
                name = "TestName",
                revisionDate = DateTime.now(),
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createFolder(folderView)
            assertEquals(CreateFolderResult.Error, result)
        }

    @Test
    fun `createFolder with folderService failure should return CreateFolderResult failure`() =
        runTest {
            val date = DateTime.now()
            val testFolderName = "TestName"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = null,
                name = testFolderName,
                revisionDate = date,
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns Folder(id = null, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.createFolder(
                    body = FolderJsonRequest(testFolderName),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.createFolder(folderView)
            assertEquals(CreateFolderResult.Error, result)
        }

    @Test
    fun `createFolder with folderService createFolder should return CreateFolderResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val date = DateTime.now()
            val testFolderName = "TestName"

            val folderView = FolderView(
                id = null,
                name = testFolderName,
                revisionDate = date,
            )

            val networkFolder = SyncResponseJson.Folder(
                id = "1",
                name = testFolderName,
                revisionDate = ZonedDateTime.now(),
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns Folder(id = null, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.createFolder(
                    body = FolderJsonRequest(testFolderName),
                )
            } returns networkFolder.asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(
                    MOCK_USER_STATE.activeUserId,
                    networkFolder,
                )
            } just runs

            coEvery {
                vaultSdkSource.decryptFolder(
                    MOCK_USER_STATE.activeUserId,
                    networkFolder.toEncryptedSdkFolder(),
                )
            } returns folderView.asSuccess()

            val result = vaultRepository.createFolder(folderView)
            assertEquals(CreateFolderResult.Success(folderView), result)
        }

    @Test
    fun `updateFolder with no active user should return UpdateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.updateFolder("Test", mockk())

            assertEquals(
                UpdateFolderResult.Error(null),
                result,
            )
        }

    @Test
    fun `updateFolder with encryptFolder failure should return UpdateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "testId"
            val folderView = FolderView(
                id = folderId,
                name = "TestName",
                revisionDate = DateTime.now(),
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateFolder(folderId, folderView)

            assertEquals(UpdateFolderResult.Error(errorMessage = null), result)
        }

    @Test
    fun `updateFolder with folderService failure should return UpdateFolderResult failure`() =
        runTest {
            val date = DateTime.now()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(testFolderName),
                )
            } returns IllegalStateException().asFailure()

            val result = vaultRepository.updateFolder(folderId, folderView)
            assertEquals(UpdateFolderResult.Error(errorMessage = null), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateFolder with folderService updateFolder Invalid response should return UpdateFolderResult Error with a non-null message`() =
        runTest {
            val date = DateTime.now()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(testFolderName),
                )
            } returns UpdateFolderResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = vaultRepository.updateFolder(folderId, folderView)
            assertEquals(
                UpdateFolderResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateFolder with folderService updateFolder success should return UpdateFolderResult success`() =
        runTest {
            val date = DateTime.now()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )

            val networkFolder = SyncResponseJson.Folder(
                id = "1",
                name = testFolderName,
                revisionDate = ZonedDateTime.now(),
            )

            coEvery {
                vaultSdkSource.encryptFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folderView,
                )
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(testFolderName),
                )
            } returns UpdateFolderResponseJson
                .Success(folder = networkFolder)
                .asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(
                    MOCK_USER_STATE.activeUserId,
                    networkFolder,
                )
            } just runs

            coEvery {
                vaultSdkSource.decryptFolder(
                    MOCK_USER_STATE.activeUserId,
                    networkFolder.toEncryptedSdkFolder(),
                )
            } returns folderView.asSuccess()

            val result = vaultRepository.updateFolder(folderId, folderView)
            assertEquals(UpdateFolderResult.Success(folderView), result)
        }

    @Test
    fun `getAuthCodeFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultRepository.getAuthCodeFlow(cipherId = "cipherId").value is DataState.Error)
    }

    @Test
    fun `getAuthCodeFlow for a single cipher should update data state when state changes`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            setVaultToUnlocked(userId = userId)

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

            every {
                settingsDiskSource.storeLastSyncTime(
                    MOCK_USER_STATE.activeUserId,
                    clock.instant(),
                )
            } just runs

            val stateFlow = MutableStateFlow<DataState<VerificationCodeItem?>>(
                DataState.Loading,
            )

            every {
                totpCodeManager.getTotpCodeStateFlow(userId = userId, any())
            } returns stateFlow

            setupDataStateFlow(userId = userId)

            vaultRepository.getAuthCodeFlow(createMockCipherView(1).id.toString()).test {
                assertEquals(
                    DataState.Loading,
                    awaitItem(),
                )

                stateFlow.tryEmit(DataState.Loaded(createVerificationCodeItem()))

                assertEquals(
                    DataState.Loaded(createVerificationCodeItem()),
                    awaitItem(),
                )

                vaultRepository.sync()

                assertEquals(
                    DataState.Pending(createVerificationCodeItem()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `getAuthCodesFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultRepository.getAuthCodesFlow().value is DataState.Error)
    }

    @Test
    fun `getAuthCodesFlow should update data state when state changes`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        setVaultToUnlocked(userId = userId)

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
        every {
            settingsDiskSource.storeLastSyncTime(
                MOCK_USER_STATE.activeUserId,
                clock.instant(),
            )
        } just runs

        val stateFlow = MutableStateFlow<DataState<List<VerificationCodeItem>>>(
            DataState.Loading,
        )

        every {
            totpCodeManager.getTotpCodesStateFlow(userId = userId, any())
        } returns stateFlow

        setupDataStateFlow(userId = userId)

        vaultRepository.getAuthCodesFlow().test {
            assertEquals(
                DataState.Loading,
                awaitItem(),
            )

            stateFlow.tryEmit(DataState.Loaded(listOf(createVerificationCodeItem())))

            assertEquals(
                DataState.Loaded(listOf(createVerificationCodeItem())),
                awaitItem(),
            )

            vaultRepository.sync()

            assertEquals(
                DataState.Pending(listOf(createVerificationCodeItem())),
                awaitItem(),
            )
        }
    }

    @Test
    fun `fullSyncFlow emission should trigger sync if necessary`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every {
            settingsDiskSource.getLastSyncTime(userId = userId)
        } returns null
        coEvery { syncService.sync() } just awaits

        mutableFullSyncFlow.tryEmit(Unit)

        coVerify { syncService.sync() }
    }

    @Test
    fun `syncCipherDeleteFlow should delete cipher from disk`() {
        val userId = "mockId-1"
        val cipherId = "mockId-1"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) } just runs

        mutableSyncCipherDeleteFlow.tryEmit(
            SyncCipherDeleteData(cipherId = cipherId),
        )

        coVerify { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create with local cipher with no common collections should do nothing`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val cipherView = createMockCipherView(number = number)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(createMockSdkCipher(number = number, clock = clock)),
                )
            } returns listOf(cipherView).asSuccess()

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

            vaultRepository.ciphersStateFlow.test {
                // Populate and consume items related to the ciphers flow
                awaitItem()
                ciphersFlow.tryEmit(listOf(createMockCipher(number = number)))
                awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                        collectionIds = null,
                        organizationId = null,
                    ),
                )
            }

            coVerify(exactly = 0) {
                ciphersService.getCipher(any())
                vaultDiskSource.saveCipher(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create with local cipher, and with common collections, should make a request and save cipher to disk`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val cipherView = createMockCipherView(number = number)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(createMockSdkCipher(number = number, clock = clock)),
                )
            } returns listOf(cipherView).asSuccess()
            val collectionView = createMockCollectionView(number = number)
            coEvery {
                vaultSdkSource.decryptCollectionList(
                    userId = MOCK_USER_STATE.activeUserId,
                    collectionList = listOf(createMockSdkCollection(number = number)),
                )
            } returns listOf(collectionView).asSuccess()

            val cipher: SyncResponseJson.Cipher = mockk()
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns cipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = MOCK_USER_STATE.activeUserId, cipher = cipher)
            } just runs

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            val collectionsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>()
            setupVaultDiskSourceFlows(
                ciphersFlow = ciphersFlow,
                collectionsFlow = collectionsFlow,
            )

            turbineScope {
                val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
                val collectionsStateFlow =
                    vaultRepository.collectionsStateFlow.testIn(backgroundScope)

                // Populate and consume items related to the ciphers flow
                ciphersStateFlow.awaitItem()
                ciphersFlow.tryEmit(listOf(createMockCipher(number = number)))
                ciphersStateFlow.awaitItem()

                // Populate and consume items related to the collections flow
                collectionsStateFlow.awaitItem()
                collectionsFlow.tryEmit(listOf(createMockCollection(number = number)))
                collectionsStateFlow.awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                        collectionIds = listOf("mockId-1"),
                        organizationId = "mock-id",
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(any())
                vaultDiskSource.saveCipher(any(), any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow update with no local cipher, but with common collections, should make a request save cipher to disk`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(),
                )
            } returns listOf<CipherView>().asSuccess()
            val collectionView = createMockCollectionView(number = number)
            coEvery {
                vaultSdkSource.decryptCollectionList(
                    userId = MOCK_USER_STATE.activeUserId,
                    collectionList = listOf(createMockSdkCollection(number = number)),
                )
            } returns listOf(collectionView).asSuccess()

            val cipher: SyncResponseJson.Cipher = mockk()
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns cipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = MOCK_USER_STATE.activeUserId, cipher = cipher)
            } just runs

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            val collectionsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>()
            setupVaultDiskSourceFlows(
                ciphersFlow = ciphersFlow,
                collectionsFlow = collectionsFlow,
            )

            turbineScope {
                val ciphersStateFlow = vaultRepository.ciphersStateFlow.testIn(backgroundScope)
                val collectionsStateFlow =
                    vaultRepository.collectionsStateFlow.testIn(backgroundScope)

                // Populate and consume items related to the ciphers flow
                ciphersStateFlow.awaitItem()
                ciphersFlow.tryEmit(listOf())
                ciphersStateFlow.awaitItem()

                // Populate and consume items related to the collections flow
                collectionsStateFlow.awaitItem()
                collectionsFlow.tryEmit(listOf(createMockCollection(number = number)))
                collectionsStateFlow.awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                        collectionIds = listOf("mockId-1"),
                        organizationId = "mock-id",
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(any())
                vaultDiskSource.saveCipher(any(), any())
            }
        }

    @Test
    fun `syncCipherUpsertFlow update with no local cipher should do nothing`() = runTest {
        val number = 1
        val cipherId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = MOCK_USER_STATE.activeUserId,
                cipherList = listOf(),
            )
        } returns listOf<CipherView>().asSuccess()

        val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
        setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

        vaultRepository.ciphersStateFlow.test {
            // Populate and consume items related to the ciphers flow
            awaitItem()
            ciphersFlow.tryEmit(listOf())
            awaitItem()

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(),
                    isUpdate = true,
                    collectionIds = null,
                    organizationId = null,
                ),
            )
        }

        coVerify(exactly = 0) {
            ciphersService.getCipher(any())
            vaultDiskSource.saveCipher(any(), any())
        }
    }

    @Test
    fun `syncCipherUpsertFlow update with more recent local cipher should do nothing`() = runTest {
        val number = 1
        val cipherId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        val cipherView = createMockCipherView(number = number)
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = MOCK_USER_STATE.activeUserId,
                cipherList = listOf(createMockSdkCipher(number = number, clock = clock)),
            )
        } returns listOf(cipherView).asSuccess()

        val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
        setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

        vaultRepository.ciphersStateFlow.test {
            // Populate and consume items related to the cipher flow
            awaitItem()
            ciphersFlow.tryEmit(listOf(createMockCipher(number = number)))
            awaitItem()

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(0), ZoneId.of("UTC"),
                    ),
                    isUpdate = true,
                    collectionIds = null,
                    organizationId = null,
                ),
            )
        }

        coVerify(exactly = 0) {
            ciphersService.getCipher(any())
            vaultDiskSource.saveCipher(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow update failure with 404 code should make a request for a cipher and then delete it`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns response.asFailure()

            coEvery {
                vaultDiskSource.deleteCipher(any(), any())
            } just runs

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val cipherView = createMockCipherView(number = number)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(createMockSdkCipher(number = number, clock = clock)),
                )
            } returns listOf(cipherView).asSuccess()

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

            vaultRepository.ciphersStateFlow.test {
                // Populate and consume items related to the ciphers flow
                awaitItem()
                ciphersFlow.tryEmit(listOf(createMockCipher(number = number)))
                awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                        collectionIds = null,
                        organizationId = null,
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(cipherId)
                vaultDiskSource.deleteCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherId = cipherId,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create failure with 404 code should make a request for a cipher and do nothing`() =
        runTest {
            val cipherId = "mockId-1"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)

            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns response.asFailure()

            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(),
                )
            } returns emptyList<CipherView>().asSuccess()

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

            vaultRepository.ciphersStateFlow.test {
                // Populate and consume items related to the ciphers flow
                awaitItem()
                ciphersFlow.tryEmit(emptyList())
                awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                        collectionIds = null,
                        organizationId = null,
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(cipherId)
            }
            coVerify(exactly = 0) {
                vaultDiskSource.deleteCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherId = cipherId,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow valid create success should make a request for a cipher and then store it`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(),
                )
            } returns listOf<CipherView>().asSuccess()

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

            val cipher: SyncResponseJson.Cipher = mockk()
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns cipher.asSuccess()

            coEvery {
                vaultDiskSource.saveCipher(any(), any())
            } just runs

            vaultRepository.ciphersStateFlow.test {
                // Populate and consume items related to the ciphers flow
                awaitItem()
                ciphersFlow.tryEmit(listOf())
                awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                        collectionIds = null,
                        organizationId = null,
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(cipherId)
                vaultDiskSource.saveCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = cipher,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow valid update success should make a request for a cipher and then store it`() =
        runTest {
            val number = 1
            val cipherId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val cipherView = createMockCipherView(number = number)
            coEvery {
                vaultSdkSource.decryptCipherList(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherList = listOf(createMockSdkCipher(number = number, clock = clock)),
                )
            } returns listOf(cipherView).asSuccess()

            val ciphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
            setupVaultDiskSourceFlows(ciphersFlow = ciphersFlow)

            val cipher: SyncResponseJson.Cipher = mockk()
            coEvery {
                ciphersService.getCipher(cipherId)
            } returns cipher.asSuccess()

            coEvery {
                vaultDiskSource.saveCipher(any(), any())
            } just runs

            vaultRepository.ciphersStateFlow.test {
                // Populate and consume items related to the ciphers flow
                awaitItem()
                ciphersFlow.tryEmit(listOf(createMockCipher(number = number)))
                awaitItem()

                mutableSyncCipherUpsertFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = cipherId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                        collectionIds = null,
                        organizationId = null,
                    ),
                )
            }

            coVerify(exactly = 1) {
                ciphersService.getCipher(cipherId)
                vaultDiskSource.saveCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = cipher,
                )
            }
        }

    @Test
    fun `syncSendDeleteFlow should delete send from disk`() {
        val userId = "mockId-1"
        val sendId = "mockId-1"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.deleteSend(userId = userId, sendId = sendId) } just runs

        mutableSyncSendDeleteFlow.tryEmit(
            SyncSendDeleteData(sendId = sendId),
        )

        coVerify { vaultDiskSource.deleteSend(userId = userId, sendId = sendId) }
    }

    @Test
    fun `syncSendUpsertFlow create with local send should do nothing`() = runTest {
        val number = 1
        val sendId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        val sendView = createMockSendView(number = number)
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = listOf(createMockSdkSend(number = number)),
            )
        } returns listOf(sendView).asSuccess()

        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

        vaultRepository.sendDataStateFlow.test {
            // Populate and consume items related to the sends flow
            awaitItem()
            sendsFlow.tryEmit(listOf(createMockSend(number = number)))
            awaitItem()

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(),
                    isUpdate = false,
                ),
            )
        }

        coVerify(exactly = 0) {
            sendsService.getSend(any())
            vaultDiskSource.saveSend(any(), any())
        }
    }

    @Test
    fun `syncSendUpsertFlow update with no local send should do nothing`() = runTest {
        val number = 1
        val sendId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = listOf(),
            )
        } returns listOf<SendView>().asSuccess()

        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

        vaultRepository.sendDataStateFlow.test {
            // Populate and consume items related to the sends flow
            awaitItem()
            sendsFlow.tryEmit(listOf())
            awaitItem()

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(),
                    isUpdate = true,
                ),
            )
        }

        coVerify(exactly = 0) {
            sendsService.getSend(any())
            vaultDiskSource.saveSend(any(), any())
        }
    }

    @Test
    fun `syncSendUpsertFlow update with more recent local send should do nothing`() = runTest {
        val number = 1
        val sendId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        val sendView = createMockSendView(number = number)
        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = listOf(createMockSdkSend(number = number)),
            )
        } returns listOf(sendView).asSuccess()

        val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
        setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

        vaultRepository.sendDataStateFlow.test {
            // Populate and consume items related to the send flow
            awaitItem()
            sendsFlow.tryEmit(listOf(createMockSend(number = number)))
            awaitItem()

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    sendId = sendId,
                    revisionDate = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(0), ZoneId.of("UTC"),
                    ),
                    isUpdate = true,
                ),
            )
        }

        coVerify(exactly = 0) {
            sendsService.getSend(any())
            vaultDiskSource.saveSend(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow update failure with 404 code should make a request for a send and then delete it`() =
        runTest {
            val number = 1
            val sendId = "mockId-$number"

            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery {
                sendsService.getSend(sendId)
            } returns response.asFailure()

            coEvery {
                vaultDiskSource.deleteSend(any(), any())
            } just runs

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val sendView = createMockSendView(number = number)
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendList = listOf(createMockSdkSend(number = number)),
                )
            } returns listOf(sendView).asSuccess()

            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

            vaultRepository.sendDataStateFlow.test {
                // Populate and consume items related to the sends flow
                awaitItem()
                sendsFlow.tryEmit(listOf(createMockSend(number = number)))
                awaitItem()

                mutableSyncSendUpsertFlow.tryEmit(
                    SyncSendUpsertData(
                        sendId = sendId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                    ),
                )
            }

            coVerify(exactly = 1) {
                sendsService.getSend(sendId)
                vaultDiskSource.deleteSend(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendId = sendId,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow create failure with 404 code should make a request for a send and do nothing`() =
        runTest {
            val sendId = "mockId-1"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)

            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery {
                sendsService.getSend(sendId)
            } returns response.asFailure()

            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendList = listOf(),
                )
            } returns emptyList<SendView>().asSuccess()

            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

            vaultRepository.sendDataStateFlow.test {
                // Populate and consume items related to the sends flow
                awaitItem()
                sendsFlow.tryEmit(emptyList())
                awaitItem()

                mutableSyncSendUpsertFlow.tryEmit(
                    SyncSendUpsertData(
                        sendId = sendId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                    ),
                )
            }

            coVerify(exactly = 1) {
                sendsService.getSend(sendId)
            }
            coVerify(exactly = 0) {
                vaultDiskSource.deleteSend(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendId = sendId,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow valid create success should make a request for a send and then store it`() =
        runTest {
            val number = 1
            val sendId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendList = listOf(),
                )
            } returns listOf<SendView>().asSuccess()

            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

            val send: SyncResponseJson.Send = mockk()
            coEvery {
                sendsService.getSend(sendId)
            } returns send.asSuccess()

            coEvery {
                vaultDiskSource.saveSend(any(), any())
            } just runs

            vaultRepository.sendDataStateFlow.test {
                // Populate and consume items related to the sends flow
                awaitItem()
                sendsFlow.tryEmit(listOf())
                awaitItem()

                mutableSyncSendUpsertFlow.tryEmit(
                    SyncSendUpsertData(
                        sendId = sendId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                    ),
                )
            }

            coVerify(exactly = 1) {
                sendsService.getSend(sendId)
                vaultDiskSource.saveSend(
                    userId = MOCK_USER_STATE.activeUserId,
                    send = send,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow valid update success should make a request for a send and then store it`() =
        runTest {
            val number = 1
            val sendId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val sendView = createMockSendView(number = number)
            coEvery {
                vaultSdkSource.decryptSendList(
                    userId = MOCK_USER_STATE.activeUserId,
                    sendList = listOf(createMockSdkSend(number = number)),
                )
            } returns listOf(sendView).asSuccess()

            val sendsFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()
            setupVaultDiskSourceFlows(sendsFlow = sendsFlow)

            val send: SyncResponseJson.Send = mockk()
            coEvery {
                sendsService.getSend(sendId)
            } returns send.asSuccess()

            coEvery {
                vaultDiskSource.saveSend(any(), any())
            } just runs

            vaultRepository.sendDataStateFlow.test {
                // Populate and consume items related to the sends flow
                awaitItem()
                sendsFlow.tryEmit(listOf(createMockSend(number = number)))
                awaitItem()

                mutableSyncSendUpsertFlow.tryEmit(
                    SyncSendUpsertData(
                        sendId = sendId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                    ),
                )
            }

            coVerify(exactly = 1) {
                sendsService.getSend(sendId)
                vaultDiskSource.saveSend(
                    userId = MOCK_USER_STATE.activeUserId,
                    send = send,
                )
            }
        }

    @Test
    fun `syncFolderDeleteFlow should delete folder from disk and update ciphers`() {
        val userId = "mockId-1"
        val folderId = "mockId-1"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.deleteFolder(userId = userId, folderId = folderId) } just runs
        coEvery {
            vaultDiskSource.getCiphers(userId)
        } returns flowOf()

        mutableSyncFolderDeleteFlow.tryEmit(
            SyncFolderDeleteData(folderId = folderId),
        )

        coVerify {
            vaultDiskSource.deleteFolder(userId = userId, folderId = folderId)
            vaultDiskSource.getCiphers(userId)
        }
    }

    @Test
    fun `syncFolderUpsertFlow create with local folder should do nothing`() = runTest {
        val number = 1
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        val folderView = createMockFolderView(number = number)
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = MOCK_USER_STATE.activeUserId,
                folderList = listOf(createMockSdkFolder(number = number)),
            )
        } returns listOf(folderView).asSuccess()

        val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
        setupVaultDiskSourceFlows(foldersFlow = foldersFlow)

        vaultRepository.foldersStateFlow.test {
            // Populate and consume items related to the folders flow
            awaitItem()
            foldersFlow.tryEmit(listOf(createMockFolder(number = number)))
            awaitItem()

            mutableSyncFolderUpsertFlow.tryEmit(
                SyncFolderUpsertData(
                    folderId = folderId,
                    revisionDate = ZonedDateTime.now(),
                    isUpdate = false,
                ),
            )
        }

        coVerify(exactly = 0) {
            folderService.getFolder(any())
            vaultDiskSource.saveFolder(any(), any())
        }
    }

    @Test
    fun `syncFolderUpsertFlow update with no local folder should do nothing`() = runTest {
        val number = 1
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = MOCK_USER_STATE.activeUserId,
                folderList = listOf(),
            )
        } returns listOf<FolderView>().asSuccess()

        val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
        setupVaultDiskSourceFlows(foldersFlow = foldersFlow)

        vaultRepository.foldersStateFlow.test {
            // Populate and consume items related to the folders flow
            awaitItem()
            foldersFlow.tryEmit(listOf())
            awaitItem()

            mutableSyncFolderUpsertFlow.tryEmit(
                SyncFolderUpsertData(
                    folderId = folderId,
                    revisionDate = ZonedDateTime.now(),
                    isUpdate = true,
                ),
            )
        }

        coVerify(exactly = 0) {
            folderService.getFolder(any())
            vaultDiskSource.saveFolder(any(), any())
        }
    }

    @Test
    fun `syncFolderUpsertFlow update with more recent local folder should do nothing`() = runTest {
        val number = 1
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
        val folderView = createMockFolderView(number = number)
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = MOCK_USER_STATE.activeUserId,
                folderList = listOf(createMockSdkFolder(number = number)),
            )
        } returns listOf(folderView).asSuccess()

        val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
        setupVaultDiskSourceFlows(foldersFlow = foldersFlow)

        vaultRepository.foldersStateFlow.test {
            // Populate and consume items related to the folders flow
            awaitItem()
            foldersFlow.tryEmit(listOf(createMockFolder(number = number)))
            awaitItem()

            mutableSyncFolderUpsertFlow.tryEmit(
                SyncFolderUpsertData(
                    folderId = folderId,
                    revisionDate = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(0), ZoneId.of("UTC"),
                    ),
                    isUpdate = true,
                ),
            )
        }

        coVerify(exactly = 0) {
            folderService.getFolder(any())
            vaultDiskSource.saveFolder(any(), any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncFolderUpsertFlow valid create success should make a request for a folder and then store it`() =
        runTest {
            val number = 1
            val folderId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            coEvery {
                vaultSdkSource.decryptFolderList(
                    userId = MOCK_USER_STATE.activeUserId,
                    folderList = listOf(),
                )
            } returns listOf<FolderView>().asSuccess()

            val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
            setupVaultDiskSourceFlows(foldersFlow = foldersFlow)

            val folder: SyncResponseJson.Folder = mockk()
            coEvery {
                folderService.getFolder(folderId)
            } returns folder.asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(any(), any())
            } just runs

            vaultRepository.foldersStateFlow.test {
                // Populate and consume items related to the folders flow
                awaitItem()
                foldersFlow.tryEmit(listOf())
                awaitItem()

                mutableSyncFolderUpsertFlow.tryEmit(
                    SyncFolderUpsertData(
                        folderId = folderId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = false,
                    ),
                )
            }

            coVerify(exactly = 1) {
                folderService.getFolder(folderId)
                vaultDiskSource.saveFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folder,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncFolderUpsertFlow valid update success should make a request for a folder and then store it`() =
        runTest {
            val number = 1
            val folderId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            setVaultToUnlocked(userId = MOCK_USER_STATE.activeUserId)
            val folderView = createMockFolderView(number = number)
            coEvery {
                vaultSdkSource.decryptFolderList(
                    userId = MOCK_USER_STATE.activeUserId,
                    folderList = listOf(createMockSdkFolder(number = number)),
                )
            } returns listOf(folderView).asSuccess()

            val foldersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
            setupVaultDiskSourceFlows(foldersFlow = foldersFlow)

            val folder: SyncResponseJson.Folder = mockk()
            coEvery {
                folderService.getFolder(folderId)
            } returns folder.asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(any(), any())
            } just runs

            vaultRepository.foldersStateFlow.test {
                // Populate and consume items related to the folders flow
                awaitItem()
                foldersFlow.tryEmit(listOf(createMockFolder(number = number)))
                awaitItem()

                mutableSyncFolderUpsertFlow.tryEmit(
                    SyncFolderUpsertData(
                        folderId = folderId,
                        revisionDate = ZonedDateTime.now(),
                        isUpdate = true,
                    ),
                )
            }

            coVerify(exactly = 1) {
                folderService.getFolder(folderId)
                vaultDiskSource.saveFolder(
                    userId = MOCK_USER_STATE.activeUserId,
                    folder = folder,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `exportVaultDataToString should return a success result when data is successfully converted for export`() =
        runTest {
            val format = ExportFormat.Json
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"

            val userCipher = createMockCipher(1).copy(
                collectionIds = null,
                deletedDate = null,
            )
            val deletedCipher = createMockCipher(2).copy(collectionIds = null)
            val orgCipher = createMockCipher(3).copy(deletedDate = null)

            coEvery {
                vaultDiskSource.getCiphers(userId)
            } returns flowOf(listOf(userCipher, deletedCipher, orgCipher))

            coEvery {
                vaultDiskSource.getFolders(userId)
            } returns flowOf(listOf(createMockFolder(1)))

            coEvery {
                vaultSdkSource.exportVaultDataToString(userId, any(), any(), format)
            } returns "TestResult".asSuccess()

            val expected = ExportVaultDataResult.Success(vaultData = "TestResult")
            val result = vaultRepository.exportVaultDataToString(format = format)

            coVerify {
                vaultSdkSource.exportVaultDataToString(
                    userId = userId,
                    ciphers = listOf(userCipher.toEncryptedSdkCipher()),
                    folders = listOf(createMockSdkFolder(1)),
                    format = ExportFormat.Json,
                )
            }

            assertEquals(
                expected,
                result,
            )
        }

    @Test
    fun `exportVaultDataToString should return a failure result when the data conversion fails`() =
        runTest {
            val format = ExportFormat.Json
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"

            coEvery {
                vaultDiskSource.getCiphers(userId)
            } returns flowOf(listOf(createMockCipher(1)))

            coEvery {
                vaultDiskSource.getFolders(userId)
            } returns flowOf(listOf(createMockFolder(1)))

            coEvery {
                vaultSdkSource.exportVaultDataToString(userId, any(), any(), format)
            } returns Throwable("Fail").asFailure()

            val expected = ExportVaultDataResult.Error
            val result = vaultRepository.exportVaultDataToString(format = format)

            assertEquals(
                expected,
                result,
            )
        }

    @Test
    fun `getDecryptedFido2CredentialAutofillViews should return error when userId not found`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val expected = DecryptFido2CredentialAutofillViewResult.Error
            val result = vaultRepository
                .getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = listOf(createMockCipherView(number = 1)),
                )

            assertEquals(
                expected,
                result,
            )
            coVerify(exactly = 0) {
                vaultSdkSource.decryptFido2CredentialAutofillViews(any(), any())
            }
        }

    @Test
    fun `getDecryptedFido2CredentialAutofillViews should return error when decryption fails`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherViewList = listOf(createMockCipherView(number = 1))
            coEvery {
                vaultSdkSource.decryptFido2CredentialAutofillViews(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherViews = cipherViewList.toTypedArray(),
                )
            } returns Throwable().asFailure()

            val result = vaultRepository.getDecryptedFido2CredentialAutofillViews(
                cipherViewList = cipherViewList,
            )

            assertEquals(DecryptFido2CredentialAutofillViewResult.Error, result)
            coVerify {
                vaultSdkSource.decryptFido2CredentialAutofillViews(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherViews = cipherViewList.toTypedArray(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getDecryptedFido2CredentialAutofillViews should return correct results when decryption succeeds`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherViewList = listOf(createMockCipherView(number = 1))
            val autofillViewList = mockk<List<Fido2CredentialAutofillView>>()
            val expected = DecryptFido2CredentialAutofillViewResult.Success(
                fido2CredentialAutofillViews = autofillViewList,
            )
            coEvery {
                vaultSdkSource.decryptFido2CredentialAutofillViews(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherViews = cipherViewList.toTypedArray(),
                )
            } returns autofillViewList.asSuccess()

            val result = vaultRepository.getDecryptedFido2CredentialAutofillViews(
                cipherViewList = cipherViewList,
            )

            assertEquals(expected, result)
            coVerify {
                vaultSdkSource.decryptFido2CredentialAutofillViews(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherViews = cipherViewList.toTypedArray(),
                )
            }
        }

    @Test
    fun `silentlyDiscoverCredentials should return result`() = runTest {
        val userId = "userId"
        val fido2CredentialStore: Fido2CredentialStore = mockk()
        val relyingPartyId = "relyingPartyId"
        val expected: List<Fido2CredentialAutofillView> = mockk()
        coEvery {
            vaultSdkSource.silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
            )
        } returns expected.asSuccess()

        val result = vaultRepository.silentlyDiscoverCredentials(
            userId = userId,
            fido2CredentialStore = fido2CredentialStore,
            relyingPartyId = relyingPartyId,
        )

        assertEquals(expected.asSuccess(), result)

        coVerify(exactly = 1) {
            vaultSdkSource.silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncForResult should return success result with itemsAvailable = true when sync succeeds and ciphers list is not empty`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery {
                syncService.sync()
            } returns mockSyncResponse.asSuccess()
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

            every {
                settingsDiskSource.storeLastSyncTime(
                    MOCK_USER_STATE.activeUserId,
                    clock.instant(),
                )
            } just runs

            val syncResult = vaultRepository.syncForResult()
            assertEquals(SyncVaultDataResult.Success(itemsAvailable = true), syncResult)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncForResult should return success result with itemsAvailable = false when sync succeeds and ciphers list is empty`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1).copy(ciphers = emptyList())
            coEvery {
                syncService.sync()
            } returns mockSyncResponse.asSuccess()
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

            every {
                settingsDiskSource.storeLastSyncTime(
                    MOCK_USER_STATE.activeUserId,
                    clock.instant(),
                )
            } just runs

            val syncResult = vaultRepository.syncForResult()
            assertEquals(SyncVaultDataResult.Success(itemsAvailable = false), syncResult)
        }

    @Test
    fun `syncForResult should return error when getAccountRevisionDateMillis fails`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val throwable = Throwable()
            coEvery {
                syncService.getAccountRevisionDateMillis()
            } returns throwable.asFailure()
            val syncResult = vaultRepository.syncForResult()
            assertEquals(
                SyncVaultDataResult.Error(throwable = throwable),
                syncResult,
            )
        }

    @Test
    fun `syncForResult should return error when sync fails`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val throwable = Throwable()
        coEvery {
            syncService.sync()
        } returns throwable.asFailure()
        val syncResult = vaultRepository.syncForResult()
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
            val result = vaultRepository.syncForResult()
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
     * Prepares for an unlock call with the given [unlockResult].
     */
    private fun prepareStateForUnlocking(
        unlockResult: VaultUnlockResult,
        mockMasterPassword: String = "mockPassword-1",
        mockPin: String = "1234",
    ) {
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
                userId = userId,
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
            userId = userId,
            privateKey = "mockPrivateKey-1",
        )
        fakeAuthDiskSource.storeUserKey(
            userId = userId,
            userKey = "mockKey-1",
        )
        fakeAuthDiskSource.storePinProtectedUserKey(
            userId = userId,
            pinProtectedUserKey = "mockKey-1",
        )
        fakeAuthDiskSource.storeOrganizationKeys(
            userId = userId,
            organizationKeys = createMockOrganizationKeys(number = 1),
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Master password unlock
        coEvery {
            vaultLockManager.unlockVault(
                userId = userId,
                kdf = MOCK_PROFILE.toSdkParams(),
                email = "email",
                privateKey = "mockPrivateKey-1",
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = mockMasterPassword,
                    userKey = "mockKey-1",
                ),
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
        } returns unlockResult

        // PIN unlock
        coEvery {
            vaultLockManager.unlockVault(
                userId = userId,
                kdf = MOCK_PROFILE.toSdkParams(),
                email = "email",
                privateKey = "mockPrivateKey-1",
                initUserCryptoMethod = InitUserCryptoMethod.Pin(
                    pin = mockPin,
                    pinProtectedUserKey = "mockKey-1",
                ),
                organizationKeys = createMockOrganizationKeys(number = 1),
            )
        } returns unlockResult
    }

    /**
     * Ensures the vault for the given [userId] is unlocked and can pass any
     * [VaultLockManager.waitUntilUnlocked] or [VaultLockManager.isVaultUnlocked] checks.
     */
    private fun setVaultToUnlocked(userId: String) {
        mutableUnlockedUserIdsStateFlow.update { it + userId }
        mutableVaultStateFlow.tryEmit(
            listOf(
                VaultUnlockData(
                    userId,
                    VaultUnlockData.Status.UNLOCKED,
                ),
            ),
        )
    }

    /**
     * Helper setup all flows required to properly subscribe to the
     * [VaultRepository.vaultDataStateFlow].
     */
    private fun setupVaultDiskSourceFlows(
        ciphersFlow: Flow<List<SyncResponseJson.Cipher>> = bufferedMutableSharedFlow(),
        collectionsFlow: Flow<List<SyncResponseJson.Collection>> = bufferedMutableSharedFlow(),
        domainsFlow: Flow<SyncResponseJson.Domains> = bufferedMutableSharedFlow(),
        foldersFlow: Flow<List<SyncResponseJson.Folder>> = bufferedMutableSharedFlow(),
        sendsFlow: Flow<List<SyncResponseJson.Send>> = bufferedMutableSharedFlow(),
    ) {
        coEvery { vaultDiskSource.getCiphers(MOCK_USER_STATE.activeUserId) } returns ciphersFlow
        coEvery {
            vaultDiskSource.getCollections(MOCK_USER_STATE.activeUserId)
        } returns collectionsFlow
        coEvery { vaultDiskSource.getDomains(MOCK_USER_STATE.activeUserId) } returns domainsFlow
        coEvery { vaultDiskSource.getFolders(MOCK_USER_STATE.activeUserId) } returns foldersFlow
        coEvery { vaultDiskSource.getSends(MOCK_USER_STATE.activeUserId) } returns sendsFlow
    }

    private fun setupEmptyDecryptionResults() {
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = MOCK_USER_STATE.activeUserId,
                cipherList = emptyList(),
            )
        } returns emptyList<CipherView>().asSuccess()
        coEvery {
            vaultSdkSource.decryptFolderList(
                userId = MOCK_USER_STATE.activeUserId,
                folderList = emptyList(),
            )
        } returns emptyList<FolderView>().asSuccess()
        coEvery {
            vaultSdkSource.decryptCollectionList(
                userId = MOCK_USER_STATE.activeUserId,
                collectionList = emptyList(),
            )
        } returns emptyList<CollectionView>().asSuccess()

        coEvery {
            vaultSdkSource.decryptSendList(
                userId = MOCK_USER_STATE.activeUserId,
                sendList = emptyList(),
            )
        } returns emptyList<SendView>().asSuccess()
    }

    private suspend fun setupDataStateFlow(userId: String) {
        coEvery {
            vaultSdkSource.decryptCipherList(
                userId = userId,
                cipherList = listOf(createMockSdkCipher(1, clock)),
            )
        } returns listOf(createMockCipherView(1)).asSuccess()
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
            ciphersFlow.tryEmit(listOf(createMockCipher(1)))
            collectionsFlow.tryEmit(listOf(createMockCollection(number = 1)))
            foldersFlow.tryEmit(listOf(createMockFolder(number = 1)))
            sendsFlow.tryEmit(listOf(createMockSend(number = 1)))
            assertEquals(DataState.Loading, awaitItem())

            assertEquals(
                DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(createMockCipherView(1)),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    private fun setupMockUri(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
    ): Uri {
        val mockUri = mockk<Uri> {
            queryParams.forEach {
                every { getQueryParameter(it.key) } returns it.value
            }
        }
        every { Uri.parse(url) } returns mockUri
        return mockUri
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
