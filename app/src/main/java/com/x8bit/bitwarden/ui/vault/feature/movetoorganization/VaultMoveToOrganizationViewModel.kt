package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the [VaultMoveToOrganizationScreen].
 *
 * @param savedStateHandle Handles the navigation arguments of this ViewModel.
 */
@HiltViewModel
@Suppress("MaxLineLength")
class VaultMoveToOrganizationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultMoveToOrganizationState, VaultMoveToOrganizationEvent, VaultMoveToOrganizationAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            VaultMoveToOrganizationState(
                vaultItemId = VaultMoveToOrganizationArgs(savedStateHandle).vaultItemId,
                viewState = VaultMoveToOrganizationState.ViewState.Loading,
                dialogState = null,
            )
        },
) {

    init {
        // TODO Load real orgs/collections BIT-769
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(1500)
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultMoveToOrganizationState.ViewState.Empty,
                )
            }
        }
    }

    override fun handleAction(action: VaultMoveToOrganizationAction) {
        when (action) {
            is VaultMoveToOrganizationAction.BackClick -> handleBackClick()
            is VaultMoveToOrganizationAction.CollectionSelect -> handleCollectionSelect(action)
            is VaultMoveToOrganizationAction.MoveClick -> handleMoveClick()
            is VaultMoveToOrganizationAction.DismissClick -> handleDismissClick()
            is VaultMoveToOrganizationAction.OrganizationSelect -> handleOrganizationSelect(action)
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
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultMoveToOrganizationState.DialogState.Loading(
                    message = R.string.saving.asText(),
                ),
            )
        }
        // TODO implement move organization functionality BIT-769
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(1500)
            sendEvent(VaultMoveToOrganizationEvent.ShowToast("Not yet implemented!".asText()))
            mutableStateFlow.update {
                it.copy(dialogState = null)
            }
            sendEvent(VaultMoveToOrganizationEvent.NavigateBack)
        }
    }

    private fun handleDismissClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
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
}

/**
 * Models state for the [VaultMoveToOrganizationScreen].
 *
 * @property vaultItemId Indicates whether the VM is in add or edit mode.
 * @property viewState indicates what view state the screen is in.
 * @property dialogState the dialog state.
 */
@Parcelize
data class VaultMoveToOrganizationState(
    val vaultItemId: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {

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
         * @property organizations the organizations available.
         */
        @Parcelize
        data class Content(
            val selectedOrganizationId: String,
            val organizations: List<Organization>,
        ) : ViewState() {

            val selectedOrganization: Organization
                get() = organizations.first { it.id == selectedOrganizationId }

            /**
             * Models an organization.
             *
             * @property id the organization id.
             * @property name the organization name.
             * @property isSelected if the organization is selected or not.
             * @property collections the list of collections associated with the organization.
             */
            @Parcelize
            data class Organization(
                val id: String,
                val name: String,
                val collections: List<Collection>,
            ) : Parcelable

            /**
             * Models a collection.
             *
             * @property id the collection id.
             * @property name the collection name.
             * @property isSelected if the collection is selected or not.
             */
            @Parcelize
            data class Collection(
                val id: String,
                val name: String,
                val isSelected: Boolean,
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

    /**
     * Show a toast with the given message.
     *
     * @property text the text to display.
     */
    data class ShowToast(val text: Text) : VaultMoveToOrganizationEvent()
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
        val collection: VaultMoveToOrganizationState.ViewState.Content.Collection,
    ) : VaultMoveToOrganizationAction()
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

private fun List<VaultMoveToOrganizationState.ViewState.Content.Collection>.toUpdatedCollections(
    selectedCollectionId: String,
): List<VaultMoveToOrganizationState.ViewState.Content.Collection> =
    map { collection ->
        collection.copy(
            isSelected = if (selectedCollectionId == collection.id) {
                !collection.isSelected
            } else {
                collection.isSelected
            },
        )
    }
