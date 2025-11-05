package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ShareCipherResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the [VaultMoveToOrganizationScreen].
 */
@HiltViewModel
@Suppress("MaxLineLength", "TooManyFunctions")
class VaultMoveToOrganizationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<VaultMoveToOrganizationState, VaultMoveToOrganizationEvent, VaultMoveToOrganizationAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toVaultMoveToOrganizationArgs()
            VaultMoveToOrganizationState(
                vaultItemId = args.vaultItemId,
                onlyShowCollections = args.showOnlyCollections,
                viewState = VaultMoveToOrganizationState.ViewState.Loading,
                dialogState = null,
            )
        },
) {

    init {
        combine(
            vaultRepository.getVaultItemStateFlow(state.vaultItemId),
            vaultRepository.collectionsStateFlow,
            authRepository.userStateFlow,
        ) { cipherViewState, collectionsState, userState ->
            VaultMoveToOrganizationAction.Internal.VaultDataReceive(
                vaultData = combineDataStates(
                    dataState1 = cipherViewState,
                    dataState2 = collectionsState,
                    dataState3 = DataState.Loaded(userState),
                ) { cipherData, collectionsData, userData ->
                    Triple(
                        first = cipherData,
                        second = collectionsData,
                        third = userData,
                    )
                },
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultMoveToOrganizationAction) {
        when (action) {
            is VaultMoveToOrganizationAction.BackClick -> handleBackClick()
            is VaultMoveToOrganizationAction.CollectionSelect -> handleCollectionSelect(action)
            is VaultMoveToOrganizationAction.MoveClick -> handleMoveClick()
            is VaultMoveToOrganizationAction.DismissClick -> handleDismissClick()
            is VaultMoveToOrganizationAction.OrganizationSelect -> handleOrganizationSelect(action)
            is VaultMoveToOrganizationAction.Internal.VaultDataReceive -> {
                handleVaultDataReceive(action)
            }

            is VaultMoveToOrganizationAction.Internal.ShareCipherResultReceive -> {
                handleShareCipherResultReceive(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(VaultMoveToOrganizationEvent.NavigateBack)
    }

    private fun handleOrganizationSelect(action: VaultMoveToOrganizationAction.OrganizationSelect) {
        updateContent { it.copy(selectedOrganizationId = action.organization.id) }
    }

    private fun handleCollectionSelect(action: VaultMoveToOrganizationAction.CollectionSelect) {
        updateContent { currentContentState ->
            currentContentState.copy(
                organizations = currentContentState
                    .organizations
                    .toUpdatedOrganizations(
                        selectedOrganizationId = currentContentState.selectedOrganizationId,
                        selectedCollectionId = action.collection.id,
                    ),
            )
        }
    }

    private fun handleMoveClick() {
        onContent { contentState ->
            contentState.cipherToMove?.let { cipherView ->
                if (contentState.selectedOrganization.collections.any { it.isSelected }) {
                    moveCipher(cipherView = cipherView, contentState = contentState)
                } else {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = VaultMoveToOrganizationState.DialogState.Error(
                                message = BitwardenString.select_one_collection.asText(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun handleDismissClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleVaultDataReceive(
        action: VaultMoveToOrganizationAction.Internal.VaultDataReceive,
    ) {
        when (action.vaultData) {
            is DataState.Error -> vaultErrorReceive(action.vaultData)
            is DataState.Loaded -> vaultLoadedReceive(action.vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(action.vaultData)
            is DataState.Pending -> vaultPendingReceive(action.vaultData)
        }
    }

    private fun handleShareCipherResultReceive(
        action: VaultMoveToOrganizationAction.Internal.ShareCipherResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialogState = null) }
        when (val result = action.shareCipherResult) {
            is ShareCipherResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultMoveToOrganizationState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is ShareCipherResult.Success -> {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = action.message),
                    relay = SnackbarRelay.CIPHER_MOVED_TO_ORGANIZATION,
                )
                sendEvent(VaultMoveToOrganizationEvent.NavigateBack)
            }
        }
    }

    private fun vaultErrorReceive(
        vaultData: DataState.Error<Triple<CipherView?, List<CollectionView>, UserState?>>,
    ) {
        mutableStateFlow.update {
            val data = vaultData.data
            if (data != null) {
                it.copy(
                    viewState = data.toViewState(),
                    dialogState = VaultMoveToOrganizationState.DialogState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                )
            } else {
                it.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                    dialogState = null,
                )
            }
        }
    }

    private fun vaultLoadedReceive(
        vaultData: DataState.Loaded<Triple<CipherView?, List<CollectionView>, UserState?>>,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.data.toViewState(),
                dialogState = null,
            )
        }
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultMoveToOrganizationState.ViewState.Loading,
                dialogState = null,
            )
        }
    }

    private fun vaultNoNetworkReceive(
        vaultData: DataState.NoNetwork<Triple<CipherView?, List<CollectionView>, UserState?>>,
    ) {
        mutableStateFlow.update {
            val data = vaultData.data
            if (data != null) {
                it.copy(
                    viewState = data.toViewState(),
                    dialogState = VaultMoveToOrganizationState.DialogState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                BitwardenString.internet_connection_required_message.asText(),
                            ),
                    ),
                )
            } else {
                it.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                BitwardenString.internet_connection_required_message.asText(),
                            ),
                    ),
                    dialogState = null,
                )
            }
        }
    }

    private fun vaultPendingReceive(
        vaultData: DataState.Pending<Triple<CipherView?, List<CollectionView>, UserState?>>,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.data.toViewState(),
                dialogState = null,
            )
        }
    }

    private inline fun updateContent(
        crossinline block: (
            VaultMoveToOrganizationState.ViewState.Content,
        ) -> VaultMoveToOrganizationState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? VaultMoveToOrganizationState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun onContent(
        crossinline block: (VaultMoveToOrganizationState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? VaultMoveToOrganizationState.ViewState.Content)?.let(block)
    }

    private fun moveCipher(
        cipherView: CipherView,
        contentState: VaultMoveToOrganizationState.ViewState.Content,
    ) {
        val collectionIds = contentState
            .selectedOrganization
            .collections
            .filter { it.isSelected }
            .map { it.id }

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultMoveToOrganizationState.DialogState.Loading(
                    message = BitwardenString.saving.asText(),
                ),
            )
        }
        viewModelScope.launch {
            trySendAction(
                if (state.onlyShowCollections) {
                    VaultMoveToOrganizationAction.Internal.ShareCipherResultReceive(
                        shareCipherResult = vaultRepository.updateCipherCollections(
                            cipherId = state.vaultItemId,
                            cipherView = cipherView,
                            collectionIds = collectionIds,
                        ),
                        message = BitwardenString.item_updated.asText(),
                    )
                } else {
                    VaultMoveToOrganizationAction.Internal.ShareCipherResultReceive(
                        shareCipherResult = vaultRepository.shareCipher(
                            cipherId = state.vaultItemId,
                            organizationId = contentState.selectedOrganizationId,
                            cipherView = cipherView,
                            collectionIds = collectionIds,
                        ),
                        message = BitwardenString.moved_item_to_org.asText(
                            requireNotNull(contentState.cipherToMove).name,
                            contentState.selectedOrganization.name,
                        ),
                    )
                },
            )
        }
    }
}

/**
 * Models state for the [VaultMoveToOrganizationScreen].
 *
 * @property vaultItemId the id for the item being moved.
 * @property viewState indicates what view state the screen is in.
 * @property dialogState the dialog state.
 */
@Parcelize
data class VaultMoveToOrganizationState(
    val vaultItemId: String,
    val onlyShowCollections: Boolean,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {

    val appBarText: Text
        get() = if (onlyShowCollections) {
            BitwardenString.collections.asText()
        } else {
            BitwardenString.move_to_organization.asText()
        }

    val appBarButtonText: Text
        get() = if (onlyShowCollections) {
            BitwardenString.save.asText()
        } else {
            BitwardenString.move.asText()
        }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message].
         */
        @Parcelize
        data class Error(
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

    /**
     * Represents the specific view states for the [VaultMoveToOrganizationScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultMoveToOrganizationScreen].
         *
         * @property message the error message to display.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Represents a loading state for the [VaultMoveToOrganizationScreen].
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultMoveToOrganizationScreen].
         *
         * @property selectedOrganizationId the selected organization id.
         * @property organizations the organizations available.
         * @property cipherToMove the cipher that is being moved to an organization.
         */
        @Parcelize
        data class Content(
            val selectedOrganizationId: String,
            val organizations: List<Organization>,
            @IgnoredOnParcel
            val cipherToMove: CipherView? = null,
        ) : ViewState() {

            val selectedOrganization: Organization
                get() = organizations.first { it.id == selectedOrganizationId }

            val selectableCollections: List<VaultCollection>
                get() {
                    val collections = organizations
                        .first { it.id == selectedOrganizationId }
                        .collections
                    return collections.filter {
                        !it.isDefaultUserCollection ||
                            isDefaultUserCollectionSelected
                    }
                }

            private val isDefaultUserCollectionSelected: Boolean
                get() =
                    cipherToMove
                        ?.collectionIds
                        ?.any { collectionId ->
                            selectedOrganization.collections.any {
                                it.id == collectionId && it.isDefaultUserCollection
                            }
                        }
                        ?: false

            /**
             * Models an organization.
             *
             * @property id the organization id.
             * @property name the organization name.
             * @property collections the list of collections associated with the organization.
             */
            @Parcelize
            data class Organization(
                val id: String,
                val name: String,
                val collections: List<VaultCollection>,
            ) : Parcelable
        }

        /**
         * Represents an empty state for the [VaultMoveToOrganizationScreen].
         */
        @Parcelize
        data object Empty : ViewState()
    }
}

/**
 * Models events for the [VaultMoveToOrganizationScreen].
 */
sealed class VaultMoveToOrganizationEvent {

    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : VaultMoveToOrganizationEvent()
}

/**
 * Models actions for the [VaultMoveToOrganizationScreen].
 */
sealed class VaultMoveToOrganizationAction {

    /**
     * Click the back button.
     */
    data object BackClick : VaultMoveToOrganizationAction()

    /**
     * Click the move button.
     */
    data object MoveClick : VaultMoveToOrganizationAction()

    /**
     * Dismiss the dialog.
     */
    data object DismissClick : VaultMoveToOrganizationAction()

    /**
     * Select an organization.
     *
     * @property organization the organization to select.
     */
    data class OrganizationSelect(
        val organization: VaultMoveToOrganizationState.ViewState.Content.Organization,
    ) : VaultMoveToOrganizationAction()

    /**
     * Select a collection.
     *
     * @property collection the collection to select.
     */
    data class CollectionSelect(
        val collection: VaultCollection,
    ) : VaultMoveToOrganizationAction()

    /**
     * Models actions that the [VaultMoveToOrganizationViewModel] itself might send.
     */
    sealed class Internal : VaultMoveToOrganizationAction() {

        /**
         * Indicates that the vault item data has been received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<Triple<CipherView?, List<CollectionView>, UserState?>>,
        ) : Internal()

        /**
         * Indicates a result for sharing a cipher has been received.
         */
        data class ShareCipherResultReceive(
            val shareCipherResult: ShareCipherResult,
            val message: Text,
        ) : Internal()
    }
}

@Suppress("MaxLineLength")
private fun List<VaultMoveToOrganizationState.ViewState.Content.Organization>.toUpdatedOrganizations(
    selectedOrganizationId: String,
    selectedCollectionId: String,
): List<VaultMoveToOrganizationState.ViewState.Content.Organization> =
    map { organization ->
        if (organization.id != selectedOrganizationId) return@map organization
        organization.copy(
            collections = organization
                .collections
                .toUpdatedCollections(selectedCollectionId = selectedCollectionId),
        )
    }

private fun List<VaultCollection>.toUpdatedCollections(
    selectedCollectionId: String,
): List<VaultCollection> =
    map { collection ->
        collection.copy(
            isSelected = if (selectedCollectionId == collection.id) {
                !collection.isSelected
            } else {
                collection.isSelected
            },
        )
    }
