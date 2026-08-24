package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultDataManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultDiskSource: VaultDiskSource = mockk()
    private val cipherManager: CipherManager = mockk()
    private val totpCodeManager: TotpCodeManager = mockk()
    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)
    private val vaultSyncManager: VaultSyncManager = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { sendDataStateFlow } returns mutableSendDataStateFlow
    }

    private val vaultDataManager: VaultDataManager = VaultDataManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        cipherManager = cipherManager,
        totpCodeManager = totpCodeManager,
        vaultDiskSource = vaultDiskSource,
        vaultSyncManager = vaultSyncManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `getAuthCodeFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultDataManager.getAuthCodeFlow(cipherId = "cipherId").value is DataState.Error)
    }

    @Test
    fun `getAuthCodeFlow for a single cipher should update data state when state changes`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val stateFlow = MutableStateFlow<DataState<VerificationCodeItem?>>(DataState.Loading)

            every {
                totpCodeManager.getTotpCodeStateFlow(userId = userId, cipherListView = any())
            } returns stateFlow
            mutableVaultDataStateFlow.value = DataState.Loaded(
                data = VaultData(
                    decryptCipherListResult = DecryptCipherListResult(
                        successes = listOf(createMockCipherListView(number = 1)),
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )

            vaultDataManager.getAuthCodeFlow(cipherId = cipherId).test {
                assertEquals(DataState.Loading, awaitItem())

                stateFlow.tryEmit(DataState.Loaded(data = createVerificationCodeItem()))
                assertEquals(DataState.Loaded(data = createVerificationCodeItem()), awaitItem())
            }
        }

    @Test
    fun `getAuthCodesFlow with no active user should emit an error`() = runTest {
        fakeAuthDiskSource.userState = null
        assertTrue(vaultDataManager.getAuthCodesFlow().value is DataState.Error)
    }

    @Test
    fun `getAuthCodesFlow should update data state when state changes`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val stateFlow = MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)

        every {
            totpCodeManager.getTotpCodesForCipherListViewsStateFlow(
                userId = userId,
                cipherListViews = any(),
            )
        } returns stateFlow
        mutableVaultDataStateFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = listOf(createMockCipherListView(number = 1)),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        vaultDataManager.getAuthCodesFlow().test {
            assertEquals(DataState.Loading, awaitItem())

            stateFlow.tryEmit(DataState.Loaded(data = listOf(createVerificationCodeItem())))
            assertEquals(DataState.Loaded(data = listOf(createVerificationCodeItem())), awaitItem())
        }
    }

    @Test
    fun `deleteVaultData should call deleteVaultData on VaultDiskSource`() {
        val userId = "userId-1234"
        coEvery { vaultDiskSource.deleteVaultData(userId = userId) } just runs

        vaultDataManager.deleteVaultData(userId = userId)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteVaultData(userId = userId)
        }
    }

    @Test
    fun `getVaultItemStateFlow should update to Error when error state is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")

            vaultDataManager.getVaultItemStateFlow(itemId = folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.Error(error = throwable)
                assertEquals(DataState.Error<CipherView>(error = throwable), awaitItem())
            }
        }

    @Test
    fun `getVaultItemStateFlow should update to NoNetwork when a NoNetwork value is emitted`() =
        runTest {
            val itemId = 1234
            val itemIdString = "mockId-$itemId"

            vaultDataManager.getVaultItemStateFlow(itemId = itemIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<CipherView>(), awaitItem())
            }
        }

    @Test
    fun `getVaultFolderStateFlow should update to NoNetwork when no network value is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"

            vaultDataManager.getVaultFolderStateFlow(folderId = folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<FolderView>(), awaitItem())
            }
        }

    @Test
    fun `getVaultFolderStateFlow should update to Error when an error is emitted`() =
        runTest {
            val folderId = 1234
            val folderIdString = "mockId-$folderId"
            val throwable = Throwable("Fail")

            vaultDataManager.getVaultFolderStateFlow(folderId = folderIdString).test {
                assertEquals(DataState.Loading, awaitItem())
                mutableVaultDataStateFlow.value = DataState.Error(error = throwable)
                assertEquals(DataState.Error<FolderView>(error = throwable), awaitItem())
            }
        }

    @Test
    fun `getSendStateFlow should update emit SendView when present`() = runTest {
        val sendId = 1
        val sendView = createMockSendView(number = sendId)

        vaultDataManager.getSendStateFlow(sendId = "mockId-$sendId").test {
            assertEquals(DataState.Loading, awaitItem())
            mutableSendDataStateFlow.value = DataState.Loaded(data = SendData(emptyList()))
            assertEquals(DataState.Loaded<SendView?>(data = null), awaitItem())
            mutableSendDataStateFlow.value = DataState.Loaded(SendData(listOf(sendView)))
            assertEquals(DataState.Loaded<SendView?>(data = sendView), awaitItem())
        }
    }

    @Test
    fun `getSendStateFlow should update to NoNetwork when NoNetwork value is emitted`() =
        runTest {
            val sendId = 1234
            vaultDataManager.getSendStateFlow(sendId = "mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendDataStateFlow.value = DataState.NoNetwork()
                assertEquals(DataState.NoNetwork<SendView?>(), awaitItem())
            }
        }

    @Test
    fun `getSendStateFlow should update to Error when an error is emitted`() =
        runTest {
            val sendId = 1234
            val throwable = Throwable("Fail")

            vaultDataManager.getSendStateFlow(sendId = "mockId-$sendId").test {
                assertEquals(DataState.Loading, awaitItem())
                mutableSendDataStateFlow.value = DataState.Error(error = throwable)
                assertEquals(DataState.Error<SendView?>(error = throwable), awaitItem())
            }
        }
}

private val MOCK_USER_STATE: UserStateJson = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf("mockId-1" to mockk()),
)
