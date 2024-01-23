package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Handles [VerificationCodeAction],
 * and launches [VerificationCodeEvent] for the [VerificationCodeScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VerificationCodeViewModel @Inject constructor(
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<VerificationCodeState, VerificationCodeEvent, VerificationCodeAction>(
    initialState = run {
        VerificationCodeState(
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
            vaultFilterType = vaultRepository.vaultFilterType,
            viewState = VerificationCodeState.ViewState.Loading,
            dialogState = null,
        )
    },
) {

    init {
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { VerificationCodeAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isIconLoadingDisabledFlow
            .map { VerificationCodeAction.Internal.IconLoadingSettingReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepository
            .getAuthCodesFlow()
            .onEach {
                sendAction(
                    VerificationCodeAction.Internal.AuthCodesReceive(it),
                )
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VerificationCodeAction) {
        when (action) {
            is VerificationCodeAction.BackClick -> handleBackClick()
            is VerificationCodeAction.CopyClick -> handleCopyClick(action)
            is VerificationCodeAction.ItemClick -> handleItemClick(action)
            is VerificationCodeAction.LockClick -> handleLockClick()
            is VerificationCodeAction.RefreshClick -> handleRefreshClick()
            is VerificationCodeAction.RefreshPull -> handleRefreshPull()
            is VerificationCodeAction.SearchIconClick -> handleSearchIconClick()
            is VerificationCodeAction.SyncClick -> handleSyncClick()
            is VerificationCodeAction.Internal -> handleInternalAction(action)
        }
    }

    //region VerificationCode Handlers
    private fun handleBackClick() {
        sendEvent(
            event = VerificationCodeEvent.NavigateBack,
        )
    }

    private fun handleCopyClick(action: VerificationCodeAction.CopyClick) {
        clipboardManager.setText(text = action.text)
    }

    private fun handleItemClick(action: VerificationCodeAction.ItemClick) {
        sendEvent(
            VerificationCodeEvent.NavigateToVaultItem(action.id),
        )
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleRefreshClick() {
        vaultRepository.sync()
    }

    private fun handleRefreshPull() {
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepository.sync()
    }

    private fun handleSearchIconClick() {
        sendEvent(
            event = VerificationCodeEvent.NavigateToVaultSearchScreen,
        )
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VerificationCodeState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            )
        }
        vaultRepository.sync()
    }

    private fun handleInternalAction(action: VerificationCodeAction.Internal) {
        when (action) {
            is VerificationCodeAction.Internal.IconLoadingSettingReceive ->
                handleIconsSettingReceived(action)

            is VerificationCodeAction.Internal.PullToRefreshEnableReceive ->
                handlePullToRefreshEnableReceive(action)

            is VerificationCodeAction.Internal.AuthCodesReceive ->
                handleAuthCodeReceive(action)
        }
    }

    private fun handleIconsSettingReceived(
        action: VerificationCodeAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled)
        }
    }

    private fun handlePullToRefreshEnableReceive(
        action: VerificationCodeAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleAuthCodeReceive(action: VerificationCodeAction.Internal.AuthCodesReceive) {
        updateViewState(action.verificationCodeData)
    }

    private fun updateViewState(
        verificationCodeData: DataState<List<VerificationCodeItem>>,
    ) {
        when (verificationCodeData) {
            is DataState.Loaded -> {
                vaultLoadedReceive(verificationCodeData)
            }

            is DataState.Error -> {
                vaultErrorReceive(verificationCodeData)
            }

            is DataState.Loading -> {
                vaultLoadingReceive()
            }

            is DataState.NoNetwork -> {
                vaultNoNetworkReceive(verificationCodeData)
            }

            is DataState.Pending -> {
                vaultPendingReceive(verificationCodeData)
            }
        }
    }

    private fun vaultNoNetworkReceive(
        verificationCodeData:
        DataState.NoNetwork<List<VerificationCodeItem>>,
    ) {
        if (verificationCodeData.data != null) {
            updateStateWithVerificationCodeData(
                verificationCodeData = verificationCodeData.data,
                clearDialogState = true,
            )
        } else {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = VerificationCodeState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(R.string.internet_connection_required_message.asText()),
                    ),
                    dialogState = null,
                )
            }
        }
        sendEvent(VerificationCodeEvent.DismissPullToRefresh)
    }

    private fun vaultPendingReceive(
        verificationCodeData: DataState.Pending<List<VerificationCodeItem>>,
    ) {
        updateStateWithVerificationCodeData(
            verificationCodeData = verificationCodeData.data,
            clearDialogState = false,
        )
    }

    private fun vaultLoadedReceive(
        verificationCodeData:
        DataState.Loaded<List<VerificationCodeItem>>,
    ) {
        updateStateWithVerificationCodeData(
            verificationCodeData = verificationCodeData.data,
            clearDialogState = true,
        )
        sendEvent(VerificationCodeEvent.DismissPullToRefresh)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VerificationCodeState.ViewState.Loading) }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<List<VerificationCodeItem>>) {
        if (vaultData.data != null) {
            updateStateWithVerificationCodeData(
                verificationCodeData = vaultData.data,
                clearDialogState = true,
            )
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = VerificationCodeState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                    dialogState = null,
                )
            }
        }
        sendEvent(VerificationCodeEvent.DismissPullToRefresh)
    }

    private fun updateStateWithVerificationCodeData(
        verificationCodeData: List<VerificationCodeItem>,
        clearDialogState: Boolean,
    ) {
        if (verificationCodeData.isEmpty()) {
            sendEvent(VerificationCodeEvent.NavigateBack)
            return
        }

        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = verificationCodeData
                        .map { item ->
                            VerificationCodeDisplayItem(
                                id = item.id,
                                authCode = item.code,
                                label = item.name,
                                supportingLabel = item.username,
                                periodSeconds = item.periodSeconds,
                                timeLeftSeconds = item.timeLeftSeconds,
                                startIcon = item.uriLoginViewList.toLoginIconData(
                                    baseIconUrl = state.baseIconUrl,
                                    isIconLoadingDisabled = state.isIconLoadingDisabled,
                                ),
                            )
                        },
                ),
                dialogState = state.dialogState.takeUnless { clearDialogState },
            )
        }
    }

//endregion VerificationCode Handlers
}

/**
 * Models state of the verification code screen.
 */
@Parcelize
data class VerificationCodeState(
    val viewState: ViewState,
    val vaultFilterType: VaultFilterType,
    val isIconLoadingDisabled: Boolean,
    val baseIconUrl: String,
    val dialogState: DialogState?,
    val isPullToRefreshSettingEnabled: Boolean,
) : Parcelable {

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

    /**
     * Represents the specific view states for the [VerificationCodeScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Represents an error state for the [VerificationCodeScreen].
         *
         * @property message Error message to display.
         */
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Loading state for the [VerificationCodeScreen],
         * signifying that the content is being processed.
         */
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
        }

        /**
         * Content state for the [VerificationCodeScreen] showing the actual content or items.
         */
        data class Content(
            val verificationCodeDisplayItems: List<VerificationCodeDisplayItem>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }
    }
}

/**
 * The data for the verification code item to displayed.
 */
@Parcelize
data class VerificationCodeDisplayItem(
    val id: String,
    val label: String,
    val supportingLabel: String?,
    val timeLeftSeconds: Int,
    val periodSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(R.drawable.ic_login_item),
) : Parcelable

/**
 * Models events for the [VerificationCodeScreen].
 */
sealed class VerificationCodeEvent {

    /**
     * Dismisses the pull-to-refresh indicator.
     */
    data object DismissPullToRefresh : VerificationCodeEvent()

    /**
     * Navigate back.
     */
    data object NavigateBack : VerificationCodeEvent()

    /**
     * Navigates to the VaultItemScreen.
     *
     * @property id the id of the item to navigate to.
     */
    data class NavigateToVaultItem(val id: String) : VerificationCodeEvent()

    /**
     * Navigates to the VaultSearchScreen.
     */
    data object NavigateToVaultSearchScreen : VerificationCodeEvent()
}

/**
 * Models actions for the [VerificationCodeScreen].
 */
sealed class VerificationCodeAction {

    /**
     * User has clicked the back button.
     */
    data object BackClick : VerificationCodeAction()

    /**
     * User has clicked the copy button.
     */
    data class CopyClick(val text: String) : VerificationCodeAction()

    /**
     * Navigates to an item.
     *
     * @property id the id of the item to navigate to.
     */
    data class ItemClick(val id: String) : VerificationCodeAction()

    /**
     * User has clicked the lock button.
     */
    data object LockClick : VerificationCodeAction()

    /**
     * User has clicked the refresh button.
     */
    data object RefreshClick : VerificationCodeAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : VerificationCodeAction()

    /**
     * User has clicked the search icon.
     */
    data object SearchIconClick : VerificationCodeAction()

    /**
     * User has clicked the refresh button.
     */
    data object SyncClick : VerificationCodeAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : VerificationCodeAction() {

        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(
            val isPullToRefreshEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates the icon setting was received.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates the verification code data was received.
         */
        data class AuthCodesReceive(
            val verificationCodeData: DataState<List<VerificationCodeItem>>,
        ) : Internal()
    }
}
