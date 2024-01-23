package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import android.net.Uri
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.toDisplayItem
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationCodeViewModelTest : BaseViewModelTest() {

    private val clipboardManager: BitwardenClipboardManager = mockk()

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { vaultFilterType } returns VaultFilterType.AllVaults
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { sync() } just runs
    }

    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
        every { environmentStateFlow } returns mockk()
    }

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)
    private val mutableIsIconLoadingDisabledFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk {
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabledFlow } returns mutableIsIconLoadingDisabledFlow
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
    }
    private val initialState = createVerificationCodeState()

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                initialState, awaitItem(),
            )
        }
    }

    @Test
    fun `on BackClick should emit onNavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VerificationCodeAction.BackClick)
            assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `onCopyClick should call setText on the ClipboardManager`() {
        val authCode = "123456"
        val viewModel = createViewModel()
        every { clipboardManager.setText(text = authCode) } just runs

        viewModel.trySendAction(VerificationCodeAction.CopyClick(authCode))

        verify(exactly = 1) {
            clipboardManager.setText(text = authCode)
        }
    }

    @Test
    fun `on ItemClick should emit ItemClick`() = runTest {
        val viewModel = createViewModel()
        val testId = "testId"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VerificationCodeAction.ItemClick(testId))
            assertEquals(VerificationCodeEvent.NavigateToVaultItem(testId), awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VerificationCodeAction.SearchIconClick)
            assertEquals(VerificationCodeEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }

    @Test
    fun `LockClick should call lockVaultForCurrentUser`() {
        every { vaultRepository.lockVaultForCurrentUser() } just runs
        val viewModel = createViewModel()

        viewModel.trySendAction(VerificationCodeAction.LockClick)

        verify(exactly = 1) {
            vaultRepository.lockVaultForCurrentUser()
        }
    }

    @Test
    fun `SyncClick should display the loading dialog and call sync`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(VerificationCodeAction.SyncClick)

        assertEquals(
            initialState.copy(
                dialogState = VerificationCodeState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.sync()
        }
    }

    @Test
    fun `ItemClick for vault item should emit NavigateToVaultItem`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VerificationCodeAction.ItemClick(id = "mock"))
            assertEquals(VerificationCodeEvent.NavigateToVaultItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `RefreshClick should sync`() = runTest {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(VerificationCodeAction.RefreshClick)
        verify { vaultRepository.sync() }
    }

    @Test
    fun `vaultDataStateFlow Pending with data should update state to Content`() = runTest {
        setupMockUri()

        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            createVerificationCodeState(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createMockCipherView(
                            number = 1,
                            isDeleted = false,
                        )
                            .toDisplayItem(
                                baseIconUrl = initialState.baseIconUrl,
                                isIconLoadingDisabled = initialState.isIconLoadingDisabled,
                            ),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Pending with empty data should call NavigateBack to go to the vault screen`() =
        runTest {
            val dataState = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `vaultDataStateFlow Pending with trash data should call NavigateBack event`() = runTest {
        val dataState = DataState.Pending(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        val dataState = DataState.Error<VaultData>(
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
        }
        assertEquals(
            createVerificationCodeState(
                viewState = VerificationCodeState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.Error(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
            error = IllegalStateException(),
        )

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
        }
        assertEquals(
            createVerificationCodeState(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createMockCipherView(
                            number = 1,
                            isDeleted = false,
                        )
                            .toDisplayItem(
                                baseIconUrl = initialState.baseIconUrl,
                                isIconLoadingDisabled = initialState.isIconLoadingDisabled,
                            ),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Error with empty data should call NavigateBack to go the vault screen`() =
        runTest {
            val dataState = DataState.Error(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
                error = IllegalStateException(),
            )

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
                assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Error with trash data should call NavigateBack to go to the vault screen`() =
        runTest {
            val dataState = DataState.Error(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
                error = IllegalStateException(),
            )

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
                assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
            }
        }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        val dataState = DataState.NoNetwork<VaultData>()

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
        }
        assertEquals(
            createVerificationCodeState(
                viewState = VerificationCodeState.ViewState.Error(
                    message = R.string.internet_connection_required_title
                        .asText()
                        .concat(R.string.internet_connection_required_message.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with data should update state to Content`() = runTest {
        setupMockUri()

        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
        }
        assertEquals(
            createVerificationCodeState(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createMockCipherView(
                            number = 1,
                            isDeleted = false,
                        )
                            .toDisplayItem(
                                baseIconUrl = initialState.baseIconUrl,
                                isIconLoadingDisabled = initialState.isIconLoadingDisabled,
                            ),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should call NavigateBack`() = runTest {
        val dataState = DataState.NoNetwork(
            data = VaultData(
                cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                folderViewList = listOf(createMockFolderView(number = 1)),
                collectionViewList = listOf(createMockCollectionView(number = 1)),
                sendViewList = listOf(createMockSendView(number = 1)),
            ),
        )

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            mutableVaultDataStateFlow.tryEmit(value = dataState)
            assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
            assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with empty items should update call NavigateBack to go the vault screen`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
                assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultDataStateFlow Loaded with trash items should call NavigateBack to go to the vault screen`() =
        runTest {
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
                assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
            }
        }

    @Test
    fun `vaultDataStateFlow Loaded with items should update ViewState to Content`() =
        runTest {
            setupMockUri()
            val dataState = DataState.Loaded(
                data = VaultData(
                    cipherViewList = listOf(
                        createMockCipherView(
                            number = 1,
                            isDeleted = false,
                        ),
                    ),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            )

            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                mutableVaultDataStateFlow.tryEmit(value = dataState)
                assertEquals(VerificationCodeEvent.DismissPullToRefresh, awaitItem())
            }

            assertEquals(
                createVerificationCodeState(
                    viewState = VerificationCodeState.ViewState.Content(
                        listOf(
                            createMockCipherView(
                                number = 1,
                                isDeleted = false,
                            )
                                .toDisplayItem(
                                    baseIconUrl = initialState.baseIconUrl,
                                    isIconLoadingDisabled = initialState.isIconLoadingDisabled,
                                ),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createViewModel()

        assertEquals(
            createVerificationCodeState(viewState = VerificationCodeState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `icon loading state updates should update isIconLoadingDisabled`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.stateFlow.value.isIconLoadingDisabled)

        mutableIsIconLoadingDisabledFlow.value = true
        assertTrue(viewModel.stateFlow.value.isIconLoadingDisabled)
    }

    @Test
    fun `RefreshPull should call vault repository sync`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(VerificationCodeAction.RefreshPull)

        verify(exactly = 1) {
            vaultRepository.sync()
        }
    }

    @Test
    fun `PullToRefreshEnableReceive should update isPullToRefreshEnabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            VerificationCodeAction.Internal.PullToRefreshEnableReceive(
                isPullToRefreshEnabled = true,
            ),
        )

        assertEquals(
            initialState.copy(isPullToRefreshSettingEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }

    private fun createViewModel(): VerificationCodeViewModel =
        VerificationCodeViewModel(
            clipboardManager = clipboardManager,
            vaultRepository = vaultRepository,
            environmentRepository = environmentRepository,
            settingsRepository = settingsRepository,
        )

    @Suppress("MaxLineLength")
    private fun createVerificationCodeState(
        viewState: VerificationCodeState.ViewState = VerificationCodeState.ViewState.Loading,
    ): VerificationCodeState =
        VerificationCodeState(
            viewState = viewState,
            vaultFilterType = vaultRepository.vaultFilterType,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            dialogState = null,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
        )
}
