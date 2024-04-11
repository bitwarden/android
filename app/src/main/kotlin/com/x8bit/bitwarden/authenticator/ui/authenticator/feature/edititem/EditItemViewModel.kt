package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.takeUntilLoaded
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.concat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model responsible for handling user interaction with the edit authenticator item screen.
 */
@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<EditItemState, EditItemEvent, EditItemAction>(
    initialState = savedStateHandle[KEY_STATE] ?: EditItemState(
        itemId = EditItemArgs(savedStateHandle).itemId,
        viewState = EditItemState.ViewState.Loading,
        dialog = null,
    )
) {

    init {
        authenticatorRepository.getItemStateFlow(state.itemId)
            .takeUntilLoaded()
            .map { itemState -> EditItemAction.Internal.EditItemDataReceive(itemState) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: EditItemAction) {
        when (action) {
            is EditItemAction.DismissDialog -> handleDismissDialogClick()
            is EditItemAction.Internal -> handleInternalAction(action)
            is EditItemAction.AlgorithmOptionClick -> handleAlgorithmOptionClick(action)
            is EditItemAction.CancelClick -> handleCancelClick()
            is EditItemAction.TypeOptionClick -> handleTypeOptionClick(action)
            is EditItemAction.AccountNameTextChange -> handleAccountNameTextChange(action)
            is EditItemAction.IssuerTextChange -> handleIssuerTextChange(action)
            is EditItemAction.RefreshPeriodOptionClick -> handlePeriodTextChange(action)
            is EditItemAction.TotpCodeTextChange -> handleTotpCodeTextChange(action)
            is EditItemAction.NumberOfDigitsOptionClick -> handleNumberOfDigitsOptionChange(action)
            is EditItemAction.SaveClick -> handleSaveClick()
            is EditItemAction.ExpandAdvancedOptionsClick -> handleExpandAdvancedOptionsClick()
        }
    }

    private fun handleExpandAdvancedOptionsClick() {
        updateContent { currentContent ->
            currentContent.copy(
                isAdvancedOptionsExpanded = currentContent.isAdvancedOptionsExpanded.not()
            )
        }
    }

    private fun handleSaveClick() = onContent { content ->
        if (content.itemData.accountName.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = EditItemState.DialogState.Generic(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required.asText(R.string.name),
                    )
                )
            }
            return@onContent
        } else if (content.itemData.totpCode.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = EditItemState.DialogState.Generic(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required.asText(R.string.secret_key),
                    )
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = EditItemState.DialogState.Loading(
                    R.string.saving.asText()
                )
            )
        }
        viewModelScope.launch {
            val result = authenticatorRepository.createItem(
                AuthenticatorItemEntity(
                    id = state.itemId,
                    key = content.itemData.totpCode.trim(),
                    accountName = content.itemData.accountName.trim(),
                    type = content.itemData.type,
                    algorithm = content.itemData.algorithm,
                    period = content.itemData.refreshPeriod.seconds,
                    digits = content.itemData.digits.length,
                    issuer = content.itemData.issuer?.trim(),
                )
            )
            trySendAction(EditItemAction.Internal.UpdateItemResult(result))
        }
    }

    private fun handleNumberOfDigitsOptionChange(action: EditItemAction.NumberOfDigitsOptionClick) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                digits = action.digitsOption
            )
        }
    }

    private fun handleAccountNameTextChange(action: EditItemAction.AccountNameTextChange) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                accountName = action.accountName
            )
        }
    }

    private fun handleIssuerTextChange(action: EditItemAction.IssuerTextChange) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                issuer = action.issue
            )
        }
    }

    private fun handleTotpCodeTextChange(action: EditItemAction.TotpCodeTextChange) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                totpCode = action.totpCode
            )
        }
    }

    private fun handlePeriodTextChange(action: EditItemAction.RefreshPeriodOptionClick) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                refreshPeriod = action.period
            )
        }
    }

    private fun handleAlgorithmOptionClick(action: EditItemAction.AlgorithmOptionClick) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                algorithm = action.algorithmOption
            )
        }
    }

    private fun handleCancelClick() {
        sendEvent(EditItemEvent.NavigateBack)
    }

    private fun handleTypeOptionClick(action: EditItemAction.TypeOptionClick) {
        updateItemData { currentItemData ->
            currentItemData.copy(
                type = action.typeOption
            )
        }
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleInternalAction(action: EditItemAction.Internal) {
        when (action) {
            is EditItemAction.Internal.EditItemDataReceive -> handleItemDataReceive(action)
            is EditItemAction.Internal.UpdateItemResult -> handleUpdateItemResultReceive(action)
        }
    }

    private fun handleUpdateItemResultReceive(action: EditItemAction.Internal.UpdateItemResult) {
        when (action.result) {
            CreateItemResult.Error -> mutableStateFlow.update {
                it.copy(
                    dialog = EditItemState.DialogState.Generic(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }

            CreateItemResult.Success -> {
                sendEvent(EditItemEvent.ShowToast(R.string.item_saved.asText()))
                sendEvent(EditItemEvent.NavigateBack)
            }
        }
    }

    private fun handleItemDataReceive(action: EditItemAction.Internal.EditItemDataReceive) {
        when (val itemState = action.itemDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = EditItemState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                val expandAdvancedOptions =
                    (state.viewState as? EditItemState.ViewState.Content)?.isAdvancedOptionsExpanded
                        ?: false
                mutableStateFlow.update {
                    it.copy(
                        viewState = itemState
                            .data
                            ?.toViewState(expandAdvancedOptions)
                            ?: EditItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText()
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = EditItemState.ViewState.Loading
                    )
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = EditItemState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                val expandAdvancedOptions =
                    (state.viewState as? EditItemState.ViewState.Content)?.isAdvancedOptionsExpanded
                        ?: false
                mutableStateFlow.update {
                    it.copy(
                        viewState = itemState
                            .data
                            ?.toViewState(expandAdvancedOptions)
                            ?: EditItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText()
                            ),
                    )
                }
            }
        }
    }

    //region Utility Functions
    private inline fun onContent(
        crossinline block: (EditItemState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? EditItemState.ViewState.Content)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            EditItemState.ViewState.Content,
        ) -> EditItemState.ViewState.Content?,
    ) {
        val currentState = state.viewState
        val updatedContent = (currentState as? EditItemState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateItemData(
        crossinline block: (EditItemData) -> EditItemData?,
    ) {
        updateContent { currentContent ->
            val currentItemData = currentContent.itemData
            val updatedItemData = currentItemData
                .let(block)
                ?: currentContent.itemData

            currentContent.copy(itemData = updatedItemData)
        }
    }

    private fun AuthenticatorItemEntity.toViewState(
        isAdvancedOptionsExpanded: Boolean,
    ) = EditItemState.ViewState.Content(
        isAdvancedOptionsExpanded = isAdvancedOptionsExpanded,
        itemData = EditItemData(
            refreshPeriod = AuthenticatorRefreshPeriodOption.fromSeconds(period)
                ?: AuthenticatorRefreshPeriodOption.THIRTY,
            totpCode = key,
            type = type,
            accountName = accountName,
            issuer = issuer,
            algorithm = algorithm,
            digits = VerificationCodeDigitsOption.fromIntOrNull(digits)
                ?: VerificationCodeDigitsOption.SIX,
        )
    )
    //endregion Utility Functions
}

/**
 * Represents the state for displaying an editable item in the authenticator.
 *
 * @property itemId ID of the item displayed.
 * @property viewState Current state of the [EditItemScreen].
 * @property dialog State of the dialog on the [EditItemScreen]. `null` if no dialog is shown.
 */
@Parcelize
data class EditItemState(
    val itemId: String,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Represents the different view states for the [EditItemScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Represents an error state for the [EditItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Represents the [EditItemScreen] content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [EditItemScreen].
         */
        @Parcelize
        data class Content(
            val isAdvancedOptionsExpanded: Boolean,
            val itemData: EditItemData,
        ) : ViewState()
    }

    /**
     * Displays a dialog on the [EditItemScreen].
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a generic dialog to the user.
         */
        @Parcelize
        data class Generic(
            val title: Text,
            val message: Text,
        ) : DialogState()

        /**
         * Displays the loading dialog with a [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Represents a set of events related to editing an authenticator item.
 */
sealed class EditItemEvent {

    /**
     * Navigates back.
     */
    data object NavigateBack : EditItemEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : EditItemEvent()
}

/**
 * Represents a set of actions related to editing an authenticator item.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class EditItemAction {
    /**
     * The user has clicked the save button.
     */
    data object SaveClick : EditItemAction()

    /**
     * The user has clicked the close button.
     */
    data object CancelClick : EditItemAction()

    /**
     * The user has dismissed the displayed item.
     */
    data object DismissDialog : EditItemAction()

    /**
     * The user has changed the account name text.
     */
    data class AccountNameTextChange(val accountName: String) : EditItemAction()

    /**
     * The user has changed the issue text.
     */
    data class IssuerTextChange(val issue: String) : EditItemAction()

    /**
     * The user has selected an Item Type option.
     */
    data class TypeOptionClick(val typeOption: AuthenticatorItemType) : EditItemAction()

    /**
     * The user has changed the TOTP code text.
     */
    data class TotpCodeTextChange(val totpCode: String) : EditItemAction()

    /**
     * The user has selected a refresh period option.
     */
    data class RefreshPeriodOptionClick(
        val period: AuthenticatorRefreshPeriodOption,
    ) : EditItemAction()

    /**
     * The user has selected an Algorithm option.
     */
    data class AlgorithmOptionClick(
        val algorithmOption: AuthenticatorItemAlgorithm,
    ) : EditItemAction()

    /**
     * The user has selected the number of verification code digits.
     */
    data class NumberOfDigitsOptionClick(
        val digitsOption: VerificationCodeDigitsOption,
    ) : EditItemAction()

    data object ExpandAdvancedOptionsClick : EditItemAction()

    /**
     * Models actions the [EditItemScreen] itself may send.
     */
    sealed class Internal : EditItemAction() {

        /**
         * Indicates that the item data has been received.
         */
        data class EditItemDataReceive(
            val itemDataState: DataState<AuthenticatorItemEntity?>,
        ) : Internal()

        /**
         * Indicates a update item result has been received.
         */
        data class UpdateItemResult(val result: CreateItemResult) : Internal()
    }
}

/**
 * Enum class representing refresh period options.
 */
enum class AuthenticatorRefreshPeriodOption(val seconds: Int) {
    THIRTY(seconds = 30),
    SIXTY(seconds = 60),
    NINETY(seconds = 90),
    ;

    companion object {
        fun fromSeconds(seconds: Int) = entries.find { it.seconds == seconds }
    }
}

/**
 * Enum class representing valid verification code lengths
 */
enum class VerificationCodeDigitsOption(val length: Int) {
    SIX(length = 6),
    EIGHT(length = 8),
    TEN(length = 10),
    TWELVE(length = 12),
    ;

    companion object {
        fun fromIntOrNull(intValue: Int): VerificationCodeDigitsOption? {
            return entries.find { it.length == intValue }
        }
    }
}
