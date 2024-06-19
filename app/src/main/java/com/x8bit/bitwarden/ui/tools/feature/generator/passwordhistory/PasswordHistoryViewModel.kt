package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.PasswordHistoryView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.PasswordHistoryState.GeneratedPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the PasswordHistoryScreen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class PasswordHistoryViewModel @Inject constructor(
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val generatorRepository: GeneratorRepository,
    vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PasswordHistoryState, PasswordHistoryEvent, PasswordHistoryAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            PasswordHistoryState(
                passwordHistoryMode = PasswordHistoryArgs(savedStateHandle).passwordHistoryMode,
                viewState = PasswordHistoryState.ViewState.Loading,
            )
        },
) {

    init {
        when (val passwordHistoryMode = state.passwordHistoryMode) {
            is GeneratorPasswordHistoryMode.Default -> {
                generatorRepository
                    .passwordHistoryStateFlow
                    .map { PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }

            is GeneratorPasswordHistoryMode.Item -> {
                vaultRepository
                    .getVaultItemStateFlow(passwordHistoryMode.itemId)
                    .map { PasswordHistoryAction.Internal.CipherDataReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun handleAction(action: PasswordHistoryAction) {
        when (action) {
            PasswordHistoryAction.CloseClick -> handleCloseClick()
            is PasswordHistoryAction.PasswordCopyClick -> handleCopyClick(action.password)
            PasswordHistoryAction.PasswordClearClick -> handlePasswordHistoryClearClick()
            is PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive -> {
                handleUpdatePasswordHistoryReceive(action)
            }

            is PasswordHistoryAction.Internal.CipherDataReceive -> handleCipherDataReceive(action)
        }
    }

    private fun handleUpdatePasswordHistoryReceive(
        action: PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive,
    ) {
        val newState = when (val state = action.state) {
            is LocalDataState.Loading -> PasswordHistoryState.ViewState.Loading

            is LocalDataState.Error -> {
                PasswordHistoryState.ViewState.Error(R.string.an_error_has_occurred.asText())
            }

            is LocalDataState.Loaded -> state.data.toViewState()
        }

        mutableStateFlow.update {
            it.copy(viewState = newState)
        }
    }

    private fun handleCipherDataReceive(action: PasswordHistoryAction.Internal.CipherDataReceive) {
        val newState: PasswordHistoryState.ViewState = when (action.state) {
            is DataState.Error -> {
                PasswordHistoryState.ViewState.Error(R.string.an_error_has_occurred.asText())
            }

            is DataState.Loaded -> action.state.data?.passwordHistory.toViewState()
            is DataState.Loading -> PasswordHistoryState.ViewState.Loading
            is DataState.NoNetwork -> {
                PasswordHistoryState.ViewState.Error(R.string.an_error_has_occurred.asText())
            }

            is DataState.Pending -> action.state.data?.passwordHistory.toViewState()
        }
        mutableStateFlow.update { it.copy(viewState = newState) }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = PasswordHistoryEvent.NavigateBack,
        )
    }

    private fun handlePasswordHistoryClearClick() {
        viewModelScope.launch {
            generatorRepository.clearPasswordHistory()
        }
    }

    private fun handleCopyClick(password: GeneratedPassword) {
        clipboardManager.setText(text = password.password)
    }

    private fun List<PasswordHistoryView>?.toViewState(): PasswordHistoryState.ViewState {
        val passwords = this?.map { passwordHistoryView ->
            GeneratedPassword(
                password = passwordHistoryView.password,
                date = passwordHistoryView.lastUsedDate.toFormattedPattern(
                    pattern = "MM/dd/yy h:mm a",
                    clock = clock,
                ),
            )
        }
        return if (passwords?.isNotEmpty() == true) {
            PasswordHistoryState.ViewState.Content(passwords)
        } else {
            PasswordHistoryState.ViewState.Empty
        }
    }
}

/**
 * Represents the possible states for the password history screen.
 *
 * @property passwordHistoryMode Indicates whether tje VM is in default or item mode.
 * @property viewState The current view state of the password history screen.
 */
@Parcelize
data class PasswordHistoryState(
    val passwordHistoryMode: GeneratorPasswordHistoryMode,
    val viewState: ViewState,
) : Parcelable {

    /**
     * Helper that represents if the menu is enabled.
     */
    val menuEnabled: Boolean
        get() = passwordHistoryMode is GeneratorPasswordHistoryMode.Default

    /**
     * Represents the specific view states for the password history screen.
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Loading state for the password history screen.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Error state for the password history screen.
         *
         * @property message The error message to be displayed.
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Empty state for the password history screen.
         */
        @Parcelize
        data object Empty : ViewState()

        /**
         * Content state for the password history screen.
         *
         * @property passwords A list of generated passwords, each with its creation date.
         */
        @Parcelize
        data class Content(val passwords: List<GeneratedPassword>) : ViewState()
    }

    /**
     * Represents a generated password with its creation date.
     *
     * @property password The generated password.
     * @property date The date when the password was generated.
     */
    @Parcelize
    data class GeneratedPassword(
        val password: String,
        val date: String,
    ) : Parcelable
}

/**
 * Defines the set of events that can occur in the password history screen.
 */
sealed class PasswordHistoryEvent {

    /**
     * Event to show a toast message.
     *
     * @property message The message to be displayed in the toast.
     */
    data class ShowToast(val message: String) : PasswordHistoryEvent()

    /**
     * Event to navigate back to the previous screen.
     */
    data object NavigateBack : PasswordHistoryEvent()
}

/**
 * Represents the set of actions that can be performed in the password history screen.
 */
sealed class PasswordHistoryAction {

    /**
     * Represents the action triggered when a password copy button is clicked.
     *
     * @param password The [GeneratedPassword] to be copied.
     */
    data class PasswordCopyClick(val password: GeneratedPassword) : PasswordHistoryAction()

    /**
     * Action when the clear passwords button is clicked.
     */
    data object PasswordClearClick : PasswordHistoryAction()

    /**
     * Action when the close button is clicked.
     */
    data object CloseClick : PasswordHistoryAction()

    /**
     * Models actions that the [PasswordHistoryViewModel] itself might send.
     */
    sealed class Internal : PasswordHistoryAction() {

        /**
         * Indicates a password history update is received.
         */
        data class UpdatePasswordHistoryReceive(
            val state: LocalDataState<List<PasswordHistoryView>>,
        ) : Internal()

        /**
         * Indicates cipher data is received.
         */
        data class CipherDataReceive(
            val state: DataState<CipherView?>,
        ) : Internal()
    }
}
