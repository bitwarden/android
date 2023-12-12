package com.x8bit.bitwarden.ui.vault.feature.item

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VerifyPasswordResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the vault item screen
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<VaultItemState, VaultItemEvent, VaultItemAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: VaultItemState(
        vaultItemId = VaultItemArgs(savedStateHandle).vaultItemId,
        viewState = VaultItemState.ViewState.Loading,
        dialog = null,
    ),
) {

    init {
        combine(
            vaultRepository.getVaultItemStateFlow(state.vaultItemId),
            authRepository.userStateFlow,
        ) { cipherViewState, userState ->
            VaultItemAction.Internal.VaultDataReceive(
                userState = userState,
                vaultDataState = cipherViewState,
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultItemAction) {
        when (action) {
            VaultItemAction.CloseClick -> handleCloseClick()
            VaultItemAction.DismissDialogClick -> handleDismissDialogClick()
            VaultItemAction.EditClick -> handleEditClick()
            is VaultItemAction.MasterPasswordSubmit -> handleMasterPasswordSubmit(action)
            VaultItemAction.RefreshClick -> handleRefreshClick()
            is VaultItemAction.Login -> handleLoginActions(action)
            is VaultItemAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleLoginActions(action: VaultItemAction.Login) {
        when (action) {
            VaultItemAction.Login.CheckForBreachClick -> handleCheckForBreachClick()
            VaultItemAction.Login.CopyPasswordClick -> handleCopyPasswordClick()
            is VaultItemAction.Login.CopyCustomHiddenFieldClick -> {
                handleCopyCustomHiddenFieldClick(action)
            }

            is VaultItemAction.Login.CopyCustomTextFieldClick -> {
                handleCopyCustomTextFieldClick(action)
            }

            is VaultItemAction.Login.CopyUriClick -> handleCopyUriClick(action)
            VaultItemAction.Login.CopyUsernameClick -> handleCopyUsernameClick()
            is VaultItemAction.Login.LaunchClick -> handleLaunchClick(action)
            VaultItemAction.Login.PasswordHistoryClick -> handlePasswordHistoryClick()
            is VaultItemAction.Login.PasswordVisibilityClicked -> {
                handlePasswordVisibilityClicked(action)
            }

            is VaultItemAction.Login.HiddenFieldVisibilityClicked -> {
                handleHiddenFieldVisibilityClicked(action)
            }
        }
    }

    private fun handleInternalAction(action: VaultItemAction.Internal) {
        when (action) {
            is VaultItemAction.Internal.PasswordBreachReceive -> handlePasswordBreachReceive(action)
            is VaultItemAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemAction.Internal.VerifyPasswordReceive -> handleVerifyPasswordReceive(action)
        }
    }

    private fun handleCloseClick() {
        sendEvent(VaultItemEvent.NavigateBack)
    }

    private fun handleCheckForBreachClick() {
        onLoginContent { login ->
            val password = requireNotNull(login.passwordData?.password)
            mutableStateFlow.update {
                it.copy(dialog = VaultItemState.DialogState.Loading)
            }
            viewModelScope.launch {
                val result = authRepository.getPasswordBreachCount(password = password)
                sendAction(VaultItemAction.Internal.PasswordBreachReceive(result))
            }
        }
    }

    private fun handleCopyPasswordClick() {
        onLoginContent { login ->
            val password = requireNotNull(login.passwordData?.password)
            if (login.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onLoginContent
            }
            sendEvent(VaultItemEvent.CopyToClipboard(password.asText()))
        }
    }

    private fun handleCopyCustomHiddenFieldClick(
        action: VaultItemAction.Login.CopyCustomHiddenFieldClick,
    ) {
        onContent { content ->
            if (content.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onContent
            }
            sendEvent(VaultItemEvent.CopyToClipboard(action.field.asText()))
        }
    }

    private fun handleCopyCustomTextFieldClick(
        action: VaultItemAction.Login.CopyCustomTextFieldClick,
    ) {
        sendEvent(VaultItemEvent.CopyToClipboard(action.field.asText()))
    }

    private fun handleCopyUriClick(action: VaultItemAction.Login.CopyUriClick) {
        sendEvent(VaultItemEvent.CopyToClipboard(action.uri.asText()))
    }

    private fun handleCopyUsernameClick() {
        onLoginContent { login ->
            val username = requireNotNull(login.username)
            if (login.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onLoginContent
            }
            sendEvent(VaultItemEvent.CopyToClipboard(username.asText()))
        }
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleEditClick() {
        onContent { content ->
            if (content.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToEdit(state.vaultItemId))
        }
    }

    private fun handleLaunchClick(action: VaultItemAction.Login.LaunchClick) {
        sendEvent(VaultItemEvent.NavigateToUri(action.uri))
    }

    private fun handleMasterPasswordSubmit(action: VaultItemAction.MasterPasswordSubmit) {
        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Loading)
        }
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(2_000)
            // TODO: Actually verify the password (BIT-1213)
            sendAction(
                VaultItemAction.Internal.VerifyPasswordReceive(
                    VerifyPasswordResult.Success(isVerified = true),
                ),
            )
            sendEvent(
                VaultItemEvent.ShowToast("Password verification not yet implemented.".asText()),
            )
        }
    }

    private fun handlePasswordHistoryClick() {
        onContent { content ->
            if (content.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToPasswordHistory(state.vaultItemId))
        }
    }

    private fun handleRefreshClick() {
        // No need to update the view state, the vault repo will emit a new state during this time
        vaultRepository.sync()
    }

    private fun handlePasswordVisibilityClicked(
        action: VaultItemAction.Login.PasswordVisibilityClicked,
    ) {
        onLoginContent { login ->
            if (login.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onLoginContent
            }
            mutableStateFlow.update {
                it.copy(
                    viewState = login.copy(
                        passwordData = login.passwordData?.copy(
                            isVisible = action.isVisible,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleHiddenFieldVisibilityClicked(
        action: VaultItemAction.Login.HiddenFieldVisibilityClicked,
    ) {
        onLoginContent { login ->
            if (login.requiresReprompt) {
                mutableStateFlow.update {
                    it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
                }
                return@onLoginContent
            }

            mutableStateFlow.update {
                it.copy(
                    viewState = login.copy(
                        customFields = login.customFields.map { customField ->
                            if (customField == action.field) {
                                action.field.copy(isVisible = action.isVisible)
                            } else {
                                customField
                            }
                        },
                    ),
                )
            }
        }
    }

    private fun handlePasswordBreachReceive(
        action: VaultItemAction.Internal.PasswordBreachReceive,
    ) {
        val message = when (val result = action.result) {
            BreachCountResult.Error -> R.string.generic_error_message.asText()
            is BreachCountResult.Success -> {
                if (result.breachCount > 0) {
                    R.string.password_exposed.asText(result.breachCount)
                } else {
                    R.string.password_safe.asText()
                }
            }
        }
        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Generic(message = message))
        }
    }

    private fun handleVaultDataReceive(action: VaultItemAction.Internal.VaultDataReceive) {
        // Leave the current data alone if there is no UserState; we are in the process of logging
        // out.
        val userState = action.userState ?: return

        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultItemState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.data
                            ?.toViewState(isPremiumUser = userState.activeAccount.isPremium)
                            ?: VaultItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = VaultItemState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultItemState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.data
                            ?.toViewState(isPremiumUser = userState.activeAccount.isPremium)
                            ?: VaultItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleVerifyPasswordReceive(
        action: VaultItemAction.Internal.VerifyPasswordReceive,
    ) {
        when (val result = action.result) {
            VerifyPasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultItemState.DialogState.Generic(
                            message = R.string.invalid_master_password.asText(),
                        ),
                    )
                }
            }

            is VerifyPasswordResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = null,
                        viewState = when (val viewState = state.viewState) {
                            is VaultItemState.ViewState.Content.Login -> viewState.copy(
                                requiresReprompt = !result.isVerified,
                            )

                            is VaultItemState.ViewState.Error -> viewState

                            VaultItemState.ViewState.Loading -> viewState
                        },
                    )
                }
            }
        }
    }

    private inline fun onContent(
        crossinline block: (VaultItemState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? VaultItemState.ViewState.Content)?.let(block)
    }

    private inline fun onLoginContent(
        crossinline block: (VaultItemState.ViewState.Content.Login) -> Unit,
    ) {
        (state.viewState as? VaultItemState.ViewState.Content.Login)?.let(block)
    }
}

/**
 * Represents the state for viewing an item in the vault.
 */
@Parcelize
data class VaultItemState(
    val vaultItemId: String,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Represents the specific view states for the [VaultItemScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Loading state for the [VaultItemScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultItemScreen].
         */
        sealed class Content : ViewState() {

            /**
             * The name of the cipher.
             */
            abstract val name: String

            /**
             * A formatted date string indicating when the cipher was last updated.
             */
            abstract val lastUpdated: String

            /**
             * An integer indicating how many times the password has been changed.
             */
            abstract val passwordHistoryCount: Int?

            /**
             * Contains general notes taken by the user.
             */
            abstract val notes: String?

            /**
             * Indicates if the user has subscribed to a premium account or not.
             */
            abstract val isPremiumUser: Boolean

            /**
             * A list of custom fields that user has added.
             */
            abstract val customFields: List<Custom>

            /**
             * Indicates if a master password prompt is required to view secure fields.
             */
            abstract val requiresReprompt: Boolean

            /**
             * Represents a loaded content state for the [VaultItemScreen] when displaying a
             * login cipher.
             */
            @Parcelize
            data class Login(
                override val name: String,
                override val lastUpdated: String,
                override val passwordHistoryCount: Int?,
                override val notes: String?,
                override val isPremiumUser: Boolean,
                override val customFields: List<Custom>,
                override val requiresReprompt: Boolean,
                val username: String?,
                val passwordData: PasswordData?,
                val uris: List<UriData>,
                val passwordRevisionDate: String?,
                val totp: String?,
            ) : Content()

            /**
             * A wrapper for the password data, this includes the [password] itself and whether it
             * should be visible.
             */
            @Parcelize
            data class PasswordData(
                val password: String,
                val isVisible: Boolean,
            ) : Parcelable

            /**
             * A wrapper for URI data, including the [uri] itself and whether it is copyable and
             * launchable.
             */
            @Parcelize
            data class UriData(
                val uri: String,
                val isCopyable: Boolean,
                val isLaunchable: Boolean,
            ) : Parcelable

            /**
             * Represents a custom field, TextField, HiddenField, BooleanField, or LinkedField.
             */
            sealed class Custom : Parcelable {
                /**
                 * Represents the data for displaying a custom text field.
                 */
                @Parcelize
                data class TextField(
                    val name: String,
                    val value: String,
                    val isCopyable: Boolean,
                ) : Custom()

                /**
                 * Represents the data for displaying a custom hidden text field.
                 */
                @Parcelize
                data class HiddenField(
                    val name: String,
                    val value: String,
                    val isCopyable: Boolean,
                    val isVisible: Boolean,
                ) : Custom()

                /**
                 * Represents the data for displaying a custom boolean property field.
                 */
                @Parcelize
                data class BooleanField(
                    val name: String,
                    val value: Boolean,
                ) : Custom()

                /**
                 * Represents the data for displaying a custom linked field.
                 */
                @Parcelize
                data class LinkedField(
                    private val id: UInt,
                    val name: String,
                ) : Custom() {
                    val type: Type get() = Type.values().first { it.id == id }

                    /**
                     * Represents the types linked fields.
                     */
                    enum class Type(
                        val id: UInt,
                        val label: Text,
                    ) {
                        USERNAME(id = 100.toUInt(), label = R.string.username.asText()),
                        PASSWORD(id = 101.toUInt(), label = R.string.password.asText()),
                    }
                }
            }
        }
    }

    /**
     * Displays a dialog.
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a generic dialog to the user.
         */
        @Parcelize
        data class Generic(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the loading dialog to the user.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * Displays the master password dialog to the user.
         */
        @Parcelize
        data object MasterPasswordDialog : DialogState()
    }
}

/**
 * Represents a set of events related view a vault item.
 */
sealed class VaultItemEvent {
    /**
     * Places the given [message] in your clipboard.
     */
    data class CopyToClipboard(
        val message: Text,
    ) : VaultItemEvent()

    /**
     * Navigates back.
     */
    data object NavigateBack : VaultItemEvent()

    /**
     * Navigates to the edit screen.
     */
    data class NavigateToEdit(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Navigates to the password history screen.
     */
    data class NavigateToPasswordHistory(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Launches the external URI.
     */
    data class NavigateToUri(
        val uri: String,
    ) : VaultItemEvent()

    /**
     * Places the given [message] in your clipboard.
     */
    data class ShowToast(
        val message: Text,
    ) : VaultItemEvent()
}

/**
 * Represents a set of actions related view a vault item.
 */
sealed class VaultItemAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : VaultItemAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialogClick : VaultItemAction()

    /**
     * The user has clicked the edit button.
     */
    data object EditClick : VaultItemAction()

    /**
     * The user has submitted their master password.
     */
    data class MasterPasswordSubmit(
        val masterPassword: String,
    ) : VaultItemAction()

    /**
     * The user has clicked the refresh button.
     */
    data object RefreshClick : VaultItemAction()

    /**
     * Models actions that are associated with the [VaultItemState.ViewState.Content.Login] state.
     */
    sealed class Login : VaultItemAction() {
        /**
         * The user has clicked the check for breach button.
         */
        data object CheckForBreachClick : Login()

        /**
         * The user has clicked the copy button for a custom hidden field.
         */
        data class CopyCustomHiddenFieldClick(
            val field: String,
        ) : Login()

        /**
         * The user has clicked the copy button for a custom text field.
         */
        data class CopyCustomTextFieldClick(
            val field: String,
        ) : Login()

        /**
         * The user has clicked the copy button for the password.
         */
        data object CopyPasswordClick : Login()

        /**
         * The user has clicked the copy button for a URI.
         */
        data class CopyUriClick(
            val uri: String,
        ) : Login()

        /**
         * The user has clicked the copy button for the username.
         */
        data object CopyUsernameClick : Login()

        /**
         * The user has clicked the launch button for a URI.
         */
        data class LaunchClick(
            val uri: String,
        ) : Login()

        /**
         * The user has clicked the password history text.
         */
        data object PasswordHistoryClick : Login()

        /**
         * The user has clicked to display the password.
         */
        data class PasswordVisibilityClicked(
            val isVisible: Boolean,
        ) : Login()

        /**
         * The user has clicked to display the a hidden field.
         */
        data class HiddenFieldVisibilityClicked(
            val field: VaultItemState.ViewState.Content.Custom.HiddenField,
            val isVisible: Boolean,
        ) : Login()
    }

    /**
     * Models actions that the [VaultItemViewModel] itself might send.
     */
    sealed class Internal : VaultItemAction() {
        /**
         * Indicates that the password breach results have been received.
         */
        data class PasswordBreachReceive(
            val result: BreachCountResult,
        ) : Internal()

        /**
         * Indicates that the vault item data has been received.
         */
        data class VaultDataReceive(
            val userState: UserState?,
            val vaultDataState: DataState<CipherView?>,
        ) : Internal()

        /**
         * Indicates that the verify password result has been received.
         */
        data class VerifyPasswordReceive(
            val result: VerifyPasswordResult,
        ) : Internal()
    }
}
