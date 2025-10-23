package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.card
import com.x8bit.bitwarden.data.autofill.util.isActiveWithCopyablePassword
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.util.toImportCredentialsRequestDataOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel for the Review Export screen.
 *
 * This ViewModel manages the UI state for reviewing items before an export operation.
 * It fetches cipher data from the [VaultRepository], transforms it into a summary list for review,
 * handles user actions such as initiating the export or cancelling the process.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ReviewExportViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val policyManager: PolicyManager,
    specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<ReviewExportState, ReviewExportEvent, ReviewExportAction>(
    initialState = ReviewExportState(
        importCredentialsRequestData = requireNotNull(
            specialCircumstanceManager
                .specialCircumstance
                ?.toImportCredentialsRequestDataOrNull(),
        ),
        viewState = ReviewExportState.ViewState.Content(
            itemTypeCounts = ReviewExportState.ItemTypeCounts(),
        ),
        dialog = null,
        hasOtherAccounts = authRepository.userStateFlow.value?.accounts.orEmpty().size > 1,
    ),
) {

    init {
        vaultRepository
            .decryptCipherListResultStateFlow
            .map { ReviewExportAction.Internal.VaultDataReceive(data = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ReviewExportAction) {
        when (action) {
            is ReviewExportAction.ImportItemsClick -> handleImportItemsClicked()
            is ReviewExportAction.CancelClick -> handleCancelClicked()
            is ReviewExportAction.DismissDialog -> handleDismissDialog()
            is ReviewExportAction.NavigateBackClick -> handleBackClick()
            is ReviewExportAction.SelectAnotherAccountClick -> handleSelectAnotherAccountClick()
            is ReviewExportAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleImportItemsClicked() {
        showLoadingDialog(message = BitwardenString.exporting_items.asText())
        viewModelScope.launch {
            trySendAction(
                ReviewExportAction.Internal.ExportResultReceive(
                    vaultRepository
                        .exportVaultDataToCxf(
                            ciphers = vaultRepository
                                .decryptCipherListResultStateFlow
                                .value
                                .data
                                ?.successes
                                ?.filter { it.deletedDate == null }
                                .filterRestrictedItemsIfNecessary(),
                        )
                        .fold(
                            onSuccess = { payload ->
                                ExportCredentialsResult.Success(
                                    payload = payload,
                                    uri = state.importCredentialsRequestData.uri,
                                )
                            },
                            onFailure = { error ->
                                ExportCredentialsResult.Failure(
                                    error = error as ImportCredentialsException,
                                )
                            },
                        ),
                ),
            )
        }
    }

    private fun handleCancelClicked() {
        sendEvent(
            ReviewExportEvent.CompleteExport(
                ExportCredentialsResult.Failure(
                    ImportCredentialsCancellationException(),
                ),
            ),
        )
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleBackClick() {
        sendEvent(ReviewExportEvent.NavigateBack)
    }

    private fun handleSelectAnotherAccountClick() {
        sendEvent(ReviewExportEvent.NavigateToAccountSelection)
    }

    private fun handleInternalAction(action: ReviewExportAction.Internal) {
        when (action) {
            is ReviewExportAction.Internal.VaultDataReceive -> {
                handleVaultDataReceive(action)
            }

            is ReviewExportAction.Internal.ExportResultReceive -> {
                handleExportResultReceive(action)
            }
        }
    }

    private fun handleVaultDataReceive(action: ReviewExportAction.Internal.VaultDataReceive) {
        when (val data = action.data) {
            DataState.Loading -> handleVaultDataLoading()

            is DataState.Loaded -> handleVaultDataLoaded(data)

            is DataState.Pending -> handleVaultDataPending(data)

            is DataState.NoNetwork -> handleVaultDataNoNetwork(data)

            is DataState.Error -> handleVaultDataError(data)
        }
    }

    private fun handleVaultDataLoading() {
        showLoadingDialog(BitwardenString.loading_vault_data.asText())
    }

    private fun handleVaultDataLoaded(data: DataState.Loaded<DecryptCipherListResult>) {
        updateItemTypeCounts(data = data, clearDialog = true)
    }

    private fun handleVaultDataPending(data: DataState.Pending<DecryptCipherListResult>) {
        updateItemTypeCounts(data = data, clearDialog = false)
    }

    private fun handleVaultDataNoNetwork(data: DataState.NoNetwork<DecryptCipherListResult>) {
        updateItemTypeCounts(data = data, clearDialog = true)
    }

    private fun handleVaultDataError(data: DataState.Error<DecryptCipherListResult>) {
        mutableStateFlow.update {
            it.copy(
                viewState = ReviewExportState.ViewState.Content(
                    itemTypeCounts = data.data.toItemTypeCounts(),
                ),
                dialog = ReviewExportState.DialogState.General(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    error = data.error,
                ),
            )
        }
    }

    private fun handleExportResultReceive(action: ReviewExportAction.Internal.ExportResultReceive) {
        mutableStateFlow.update { it.copy(dialog = null) }
        sendEvent(
            ReviewExportEvent.CompleteExport(
                result = action.result,
            ),
        )
    }

    private fun showLoadingDialog(message: Text = BitwardenString.loading.asText()) {
        mutableStateFlow.update {
            it.copy(
                dialog = ReviewExportState.DialogState.Loading(
                    message = message,
                ),
            )
        }
    }

    private fun List<CipherListView>?.filterRestrictedItemsIfNecessary(): List<CipherListView> {
        this ?: return emptyList()
        val activeUserOrgIds = authRepository.userStateFlow
            .value
            ?.activeAccount
            ?.organizations
            ?.map { it.id }
            ?: return this
        val itemRestrictedOrgIds = policyManager
            .getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
            .filter { it.isEnabled }
            .map { it.organizationId }

        return if (activeUserOrgIds.any { itemRestrictedOrgIds.contains(it) }) {
            this.filter { it.card == null }
        } else {
            this
        }
    }

    private fun DecryptCipherListResult?.toItemTypeCounts(): ReviewExportState.ItemTypeCounts {
        var passwordItemCount = 0
        var passkeyItemCount = 0
        var identityItemCount = 0
        var cardItemCount = 0
        var secureNoteItemCount = 0
        this@toItemTypeCounts
            ?.successes
            ?.filter { it.deletedDate == null }
            .orEmpty()
            .forEach {
                when {
                    it.isActiveWithCopyablePassword -> passwordItemCount++
                    it.isActiveWithFido2Credentials -> passkeyItemCount++
                    it.type is CipherListViewType.Identity -> identityItemCount++
                    it.card != null -> cardItemCount++
                    it.type is CipherListViewType.SecureNote -> secureNoteItemCount++
                }
            }

        return ReviewExportState.ItemTypeCounts(
            passwordCount = passwordItemCount,
            passkeyCount = passkeyItemCount,
            identityCount = identityItemCount,
            cardCount = cardItemCount,
            secureNoteCount = secureNoteItemCount,
        )
    }

    private fun updateItemTypeCounts(
        data: DataState<DecryptCipherListResult>,
        clearDialog: Boolean,
    ) {
        mutableStateFlow.update {
            val itemTypeCounts = data.data.toItemTypeCounts()
            val viewState = if (itemTypeCounts.hasItemsToExport) {
                ReviewExportState.ViewState.Content(
                    itemTypeCounts = itemTypeCounts,
                )
            } else {
                ReviewExportState.ViewState.NoItems
            }

            it.copy(
                viewState = viewState,
                dialog = it.dialog.takeUnless { clearDialog },
            )
        }
    }
}

/**
 * Represents the state of the Review Import screen.
 *
 * @property viewState The current view state containing item type counts.
 * @property dialog The current dialog state to be displayed, if any.
 */
@Parcelize
data class ReviewExportState(
    val viewState: ViewState,
    val dialog: DialogState? = null,
    // Internally used properties
    val importCredentialsRequestData: ImportCredentialsRequestData,
    val hasOtherAccounts: Boolean,
) : Parcelable {

    /**
     * Represents the view state with item type counts.
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Represents the content state with item type counts.
         */
        data class Content(
            val itemTypeCounts: ItemTypeCounts,
        ) : ViewState()

        /**
         * Represents the state when there are no items to be exported.
         */
        data object NoItems : ViewState()
    }

    /**
     * Represents the counts of different item types to be exported.
     */
    @Parcelize
    data class ItemTypeCounts(
        val passwordCount: Int = 0,
        val passkeyCount: Int = 0,
        val identityCount: Int = 0,
        val cardCount: Int = 0,
        val secureNoteCount: Int = 0,
    ) : Parcelable {
        /**
         * Whether there are any items to be exported.
         */
        @IgnoredOnParcel
        val hasItemsToExport: Boolean = passwordCount > 0 ||
            passkeyCount > 0 ||
            identityCount > 0 ||
            cardCount > 0 ||
            secureNoteCount > 0
    }

    /**
     * Represents the possible dialog states for the Review Import screen.
     */
    @Parcelize
    sealed class DialogState : Parcelable {
        /**
         * A generic dialog with a title, message, and optional error.
         *
         * @property title A function to retrieve the dialog title string resource.
         * @property message A function to retrieve the dialog message string resource.
         * @property error An optional throwable associated with the dialog.
         */
        data class General(
            val title: Text,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * A dialog indicating an ongoing loading or processing state.
         *
         * @property message A function to retrieve the loading message string resource.
         */
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Defines the actions that can be processed by the [ReviewExportViewModel].
 */
sealed class ReviewExportAction {
    /**
     * Action triggered when the "Import items" button is clicked by the user.
     */
    data object ImportItemsClick : ReviewExportAction()

    /**
     * Action triggered when the "Cancel" button is clicked by the user.
     */
    data object CancelClick : ReviewExportAction()

    /**
     * Action triggered when a dialog is dismissed by the user.
     */
    data object DismissDialog : ReviewExportAction()

    /**
     * Action triggered when the back button is clicked by the user.
     */
    data object NavigateBackClick : ReviewExportAction()

    /**
     * Action triggered when the Select another account button is clicked by the user.
     */
    data object SelectAnotherAccountClick : ReviewExportAction()

    /**
     * Internal actions that the [ReviewExportViewModel] itself may send.
     */
    sealed class Internal : ReviewExportAction() {

        /**
         * Represents a result of decrypting the cipher list.
         */
        data class VaultDataReceive(
            val data: DataState<DecryptCipherListResult>,
        ) : Internal()

        /**
         * Represents a result of exporting the vault data.
         */
        data class ExportResultReceive(
            val result: ExportCredentialsResult,
        ) : Internal()
    }
}

/**
 * Defines events that the [ReviewExportViewModel] can send to the UI.
 */
@Stable
sealed class ReviewExportEvent {
    /**
     * Event to navigate back to the previous screen, typically used when the user cancels
     * the import process.
     */
    data object NavigateBack : ReviewExportEvent()

    /**
     * Event to navigate to account selection.
     */
    data object NavigateToAccountSelection : ReviewExportEvent()

    /**
     * Event indicating that the import attempt has completed.
     * The consuming screen or navigation controller should handle this event to proceed
     * appropriately based on the [result] of the import operation.
     *
     * The [ExportCredentialsResult] is used here generically to represent the outcome
     * of the import operation. For successful imports related to this feature,
     * the specific fields within [ExportCredentialsResult.Success] (payload, uri)
     * may not be directly relevant but the type indicates a successful outcome.
     * [ExportCredentialsResult.Failure] can be used as is.
     *
     * @property result The [ExportCredentialsResult] object containing details about the outcome of
     * the import process.
     */
    data class CompleteExport(val result: ExportCredentialsResult) : ReviewExportEvent()
}
