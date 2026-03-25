package com.x8bit.bitwarden.ui.platform.feature.settings.collections

import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.collections.model.CollectionDisplayItem
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CollectionsViewModelTest : BaseViewModelTest() {

    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<com.bitwarden.collections.CollectionView>>>(
            DataState.Loaded(emptyList()),
        )

    private val vaultRepository: VaultRepository = mockk {
        every { collectionsStateFlow } returns mutableCollectionsStateFlow
    }

    private val authRepository: AuthRepository = mockk {
        every { organizations } returns listOf(DEFAULT_ORGANIZATION)
    }

    private val mutableSnackbarDataFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
    }

    @Test
    fun `on snackbar data received should emit ShowSnackbar`() = runTest {
        val viewModel = createViewModel()

        val data = BitwardenSnackbarData(message = "Snackbar!".asText())
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.emit(data)
            assertEquals(
                CollectionsEvent.ShowSnackbar(data = data),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CollectionsAction.CloseButtonClick)
            assertEquals(CollectionsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CollectionClick with canManage should emit NavigateToEditCollectionScreen`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    CollectionsAction.CollectionClick(
                        collectionId = COLLECTION_ID,
                        organizationId = ORGANIZATION_ID,
                        canManage = true,
                    ),
                )
                assertEquals(
                    CollectionsEvent.NavigateToEditCollectionScreen(
                        collectionId = COLLECTION_ID,
                        organizationId = ORGANIZATION_ID,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `CollectionClick without canManage should not emit`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                CollectionsAction.CollectionClick(
                    collectionId = COLLECTION_ID,
                    organizationId = ORGANIZATION_ID,
                    canManage = false,
                ),
            )
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `AddCollectionButtonClick should emit NavigateToAddCollectionScreen when org has permission`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(CollectionsAction.AddCollectionButtonClick)
                assertEquals(
                    CollectionsEvent.NavigateToAddCollectionScreen(
                        organizationId = ORGANIZATION_ID,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `VaultDataReceive Loading should show Loading state`() {
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(DataState.Loading)

        assertEquals(
            createCollectionsState(),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive Loaded should show Content with collections`() {
        val collectionView = createMockCollectionView(number = 1)
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.Loaded(listOf(collectionView)),
        )

        assertEquals(
            createCollectionsState(
                viewState = CollectionsState.ViewState.Content(
                    collectionList = listOf(DEFAULT_DISPLAY_ITEM),
                    showAddButton = true,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive Error should show Error state`() {
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.Error(
                data = emptyList(),
                error = IllegalStateException(),
            ),
        )

        assertEquals(
            createCollectionsState(
                viewState = CollectionsState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive NoNetwork should show Error state`() {
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.NoNetwork(data = emptyList()),
        )

        assertEquals(
            createCollectionsState(
                viewState = CollectionsState.ViewState.Error(
                    message = BitwardenString.internet_connection_required_title
                        .asText()
                        .concat(
                            " ".asText(),
                            BitwardenString
                                .internet_connection_required_message
                                .asText(),
                        ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive Pending should show Content with collections`() {
        val collectionView = createMockCollectionView(number = 1)
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.Pending(listOf(collectionView)),
        )

        assertEquals(
            createCollectionsState(
                viewState = CollectionsState.ViewState.Content(
                    collectionList = listOf(DEFAULT_DISPLAY_ITEM),
                    showAddButton = true,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultDataReceive should show FAB when user canManageCollections`() {
        every { authRepository.organizations } returns listOf(
            createMockOrganization(
                number = 1,
                id = ORGANIZATION_ID,
                role = OrganizationType.ADMIN,
            ),
        )
        val collectionView = createMockCollectionView(number = 1)
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.Loaded(listOf(collectionView)),
        )

        val content = viewModel.stateFlow.value.viewState
            as CollectionsState.ViewState.Content
        assertEquals(true, content.showAddButton)
    }

    @Test
    fun `VaultDataReceive should hide FAB when user cannot manage collections`() {
        every { authRepository.organizations } returns listOf(
            createMockOrganization(
                number = 1,
                id = ORGANIZATION_ID,
                role = OrganizationType.USER,
                limitCollectionCreation = true,
                canCreateNewCollections = false,
            ),
        )
        val collectionView = createMockCollectionView(number = 1)
        val viewModel = createViewModel()

        mutableCollectionsStateFlow.tryEmit(
            DataState.Loaded(listOf(collectionView)),
        )

        val content = viewModel.stateFlow.value.viewState
            as CollectionsState.ViewState.Content
        assertEquals(false, content.showAddButton)
    }

    private fun createViewModel(): CollectionsViewModel = CollectionsViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        snackbarRelayManager = snackbarRelayManager,
    )

    private fun createCollectionsState(
        viewState: CollectionsState.ViewState = CollectionsState.ViewState.Loading,
    ) = CollectionsState(
        viewState = viewState,
    )
}

private const val ORGANIZATION_ID = "mockId-1"
private const val COLLECTION_ID = "mockId-1"

private val DEFAULT_ORGANIZATION = createMockOrganization(
    number = 1,
    id = ORGANIZATION_ID,
    role = OrganizationType.ADMIN,
)

private val DEFAULT_DISPLAY_ITEM = CollectionDisplayItem(
    id = "mockId-1",
    name = "mockName-1",
    organizationName = "",
    organizationId = "mockOrganizationId-1",
    canManage = true,
)
