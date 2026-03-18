package com.x8bit.bitwarden.ui.platform.feature.settings.collections

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.collections.model.CollectionDisplayItem
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Handles [CollectionsAction],
 * and launches [CollectionsEvent] for the [CollectionsScreen].
 */
@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    vaultRepository: VaultRepository,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<CollectionsState, CollectionsEvent, CollectionsAction>(
    initialState = CollectionsState(viewState = CollectionsState.ViewState.Loading),
) {
    init {
        vaultRepository
            .collectionsStateFlow
            .onEach {
                sendAction(CollectionsAction.Internal.VaultDataReceive(it))
            }
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(
                SnackbarRelay.COLLECTION_CREATED,
                SnackbarRelay.COLLECTION_DELETED,
                SnackbarRelay.COLLECTION_UPDATED,
            )
            .map { CollectionsAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CollectionsAction): Unit = when (action) {
        is CollectionsAction.AddCollectionButtonClick -> handleAddCollectionButtonClicked()
        is CollectionsAction.CloseButtonClick -> handleCloseButtonClicked()
        is CollectionsAction.Internal -> handleInternalAction(action)
        is CollectionsAction.CollectionClick -> handleCollectionClick(action)
    }

    private fun handleInternalAction(action: CollectionsAction.Internal) {
        when (action) {
            is CollectionsAction.Internal.SnackbarDataReceived -> {
                handleSnackbarDataReceived(action)
            }

            is CollectionsAction.Internal.VaultDataReceive -> {
                handleVaultDataReceive(action)
            }
        }
    }

    private fun handleCollectionClick(action: CollectionsAction.CollectionClick) {
        sendEvent(
            CollectionsEvent.NavigateToEditCollectionScreen(
                collectionId = action.collectionId,
                organizationId = action.organizationId,
            ),
        )
    }

    private fun handleAddCollectionButtonClicked() {
        // For now, use the first org with create permission.
        // TODO: If multiple orgs, show org picker (pending G1 decision).
        val org = authRepository.organizations.firstOrNull {
            it.canCreateNewCollections
        }
        if (org != null) {
            sendEvent(CollectionsEvent.NavigateToAddCollectionScreen(org.id))
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(CollectionsEvent.NavigateBack)
    }

    private fun handleSnackbarDataReceived(
        action: CollectionsAction.Internal.SnackbarDataReceived,
    ) {
        sendEvent(CollectionsEvent.ShowSnackbar(action.data))
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(
        action: CollectionsAction.Internal.VaultDataReceive,
    ) {
        val organizations = authRepository.organizations
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = CollectionsState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = CollectionsState.ViewState.Content(
                            collectionList = vaultDataState.data
                                .toDisplayItems(organizations),
                            showAddButton = organizations.any {
                                org -> org.canCreateNewCollections
                            },
                        ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = CollectionsState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
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
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = CollectionsState.ViewState.Content(
                            collectionList = vaultDataState.data
                                .toDisplayItems(organizations),
                            showAddButton = organizations.any {
                                org -> org.canCreateNewCollections
                            },
                        ),
                    )
                }
            }
        }
    }
}

private fun List<CollectionView>.toDisplayItems(
    organizations: List<com.x8bit.bitwarden.data.auth.repository.model.Organization>,
): List<CollectionDisplayItem> =
    map { collection ->
        CollectionDisplayItem(
            id = collection.id.toString(),
            name = collection.name,
            organizationName = organizations
                .find { it.id == collection.organizationId.toString() }
                ?.name
                .orEmpty(),
        )
    }

/**
 * Represents the state for the collections screen.
 *
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class CollectionsState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [CollectionsScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [CollectionsScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [CollectionsScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [CollectionsScreen].
         */
        @Parcelize
        data class Content(
            val collectionList: List<CollectionDisplayItem>,
            val showAddButton: Boolean,
        ) : ViewState()
    }
}

/**
 * Models events for the collections screen.
 */
sealed class CollectionsEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : CollectionsEvent()

    /**
     * Navigates to the screen to add a collection.
     */
    data class NavigateToAddCollectionScreen(
        val organizationId: String,
    ) : CollectionsEvent()

    /**
     * Navigates to the screen to edit a collection.
     */
    data class NavigateToEditCollectionScreen(
        val collectionId: String,
        val organizationId: String,
    ) : CollectionsEvent()

    /**
     * Show a snackbar.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : CollectionsEvent(), BackgroundEvent
}

/**
 * Models actions for the collections screen.
 */
sealed class CollectionsAction {
    /**
     * Indicates that the user clicked the add collection button.
     */
    data object AddCollectionButtonClick : CollectionsAction()

    /**
     * Indicates that the user clicked a collection.
     */
    data class CollectionClick(
        val collectionId: String,
        val organizationId: String,
    ) : CollectionsAction()

    /**
     * Indicates that the user clicked the close button.
     */
    data object CloseButtonClick : CollectionsAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : CollectionsAction() {
        /**
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates that the vault collections data has been received.
         */
        data class VaultDataReceive(
            val vaultDataState: DataState<List<CollectionView>>,
        ) : Internal()
    }
}
