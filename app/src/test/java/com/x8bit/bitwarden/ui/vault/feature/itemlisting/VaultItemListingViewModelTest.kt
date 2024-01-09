package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.createMockItemListingDisplayItem
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultItemListingViewModelTest : BaseViewModelTest() {

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
        every { sync() } returns Unit
    }
    private val initialState = createVaultItemListingState()
    private val initialSavedStateHandle = createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType = VaultItemListingType.Login,
    )

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                initialState, awaitItem(),
            )
        }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.SearchIconClick)
            assertEquals(VaultItemListingEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }

    @Test
    fun `ItemClick should emit NavigateToVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.ItemClick(id = "mock"))
            assertEquals(VaultItemListingEvent.NavigateToVaultItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `AddVaultItemClick should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(VaultItemListingEvent.NavigateToAddVaultItem, awaitItem())
        }
    }

    @Test
    fun `RefreshClick should sync`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.actionChannel.trySend(VaultItemListingsAction.RefreshClick)
        verify { vaultRepository.sync() }
    }

    @Test
    fun `vaultDataStateFlow Loaded with items should update ViewState to Content`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
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
                ),
            )

            val viewModel = createVaultItemListingViewModel()

            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.Content(
                        displayItemList = listOf(
                            createMockItemListingDisplayItem(number = 1),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loaded with empty items should update ViewState to NoItems`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = emptyList(),
                        folderViewList = emptyList(),
                        collectionViewList = emptyList(),
                        sendViewList = emptyList(),
                    ),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                createVaultItemListingState(viewState = VaultItemListingState.ViewState.NoItems),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loaded with trash items should update ViewState to NoItems`() =
        runTest {
            mutableVaultDataStateFlow.tryEmit(
                value = DataState.Loaded(
                    data = VaultData(
                        cipherViewList = listOf(createMockCipherView(number = 1)),
                        folderViewList = listOf(createMockFolderView(number = 1)),
                        collectionViewList = listOf(createMockCollectionView(number = 1)),
                        sendViewList = listOf(createMockSendView(number = 1)),
                    ),
                ),
            )
            val viewModel = createVaultItemListingViewModel()
            assertEquals(
                createVaultItemListingState(
                    viewState = VaultItemListingState.ViewState.NoItems,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultDataStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultDataStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(viewState = VaultItemListingState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with data should update state to Content`() = runTest {
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

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createMockItemListingDisplayItem(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with empty data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(viewState = VaultItemListingState.ViewState.NoItems),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Pending with trash data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Pending(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(viewState = VaultItemListingState.ViewState.NoItems),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error without data should update state to Error`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Error(
                error = IllegalStateException(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with data should update state to Content`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Error(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
                error = IllegalStateException(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createMockItemListingDisplayItem(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with empty data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Error(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
                error = IllegalStateException(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow Error with trash data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.Error(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
                error = IllegalStateException(),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork without data should update state to Error`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Error(
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
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = false)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createMockItemListingDisplayItem(number = 1),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with empty data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(
                data = VaultData(
                    cipherViewList = emptyList(),
                    folderViewList = emptyList(),
                    collectionViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultDataStateFlow NoNetwork with trash data should update state to NoItems`() = runTest {
        mutableVaultDataStateFlow.tryEmit(
            value = DataState.NoNetwork(
                data = VaultData(
                    cipherViewList = listOf(createMockCipherView(number = 1, isDeleted = true)),
                    folderViewList = listOf(createMockFolderView(number = 1)),
                    collectionViewList = listOf(createMockCollectionView(number = 1)),
                    sendViewList = listOf(createMockSendView(number = 1)),
                ),
            ),
        )

        val viewModel = createVaultItemListingViewModel()

        assertEquals(
            createVaultItemListingState(
                viewState = VaultItemListingState.ViewState.NoItems,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType: VaultItemListingType,
    ) = SavedStateHandle().apply {
        set(
            "vault_item_listing_type",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> "card"
                is VaultItemListingType.Collection -> "collection"
                is VaultItemListingType.Folder -> "folder"
                is VaultItemListingType.Identity -> "identity"
                is VaultItemListingType.Login -> "login"
                is VaultItemListingType.SecureNote -> "secure_note"
                is VaultItemListingType.Trash -> "trash"
            },
        )
        set(
            "id",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> null
                is VaultItemListingType.Collection -> vaultItemListingType.collectionId
                is VaultItemListingType.Folder -> vaultItemListingType.folderId
                is VaultItemListingType.Identity -> null
                is VaultItemListingType.Login -> null
                is VaultItemListingType.SecureNote -> null
                is VaultItemListingType.Trash -> null
            },
        )
    }

    private fun createVaultItemListingViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
        vaultRepository: VaultRepository = this.vaultRepository,
    ): VaultItemListingViewModel =
        VaultItemListingViewModel(
            savedStateHandle = savedStateHandle,
            vaultRepository = vaultRepository,
        )

    @Suppress("MaxLineLength")
    private fun createVaultItemListingState(
        itemListingType: VaultItemListingState.ItemListingType = VaultItemListingState.ItemListingType.Login,
        viewState: VaultItemListingState.ViewState = VaultItemListingState.ViewState.Loading,
    ): VaultItemListingState =
        VaultItemListingState(
            itemListingType = itemListingType,
            viewState = viewState,
        )
}
