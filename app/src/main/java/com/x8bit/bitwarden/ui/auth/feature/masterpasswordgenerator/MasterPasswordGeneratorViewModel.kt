package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.generators.PassphraseGeneratorRequest
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toStrictestPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.math.max

private const val KEY_STATE = "state"
private const val DEFAULT_SEPARATOR = "-"
private const val DEFAULT_WORD_COUNT = 3

/**
 * ViewModel to support the [MasterPasswordGeneratorScreen]
 */
@HiltViewModel
@Suppress("MaxLineLength")
class MasterPasswordGeneratorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generatorRepository: GeneratorRepository,
    private val policyManager: PolicyManager,
) : BaseViewModel<MasterPasswordGeneratorState, MasterPasswordGeneratorEvent, MasterPasswordGeneratorAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: MasterPasswordGeneratorState(generatedPassword = DEFAULT_SEPARATOR),
) {
    private var generatePasswordJob: Job? = null
    private val passphraseRequest = getPolicyBasedPassphraseRequest()

    init {
        if (state.generatedPassword == DEFAULT_SEPARATOR) {
            generateNewPassphrase()
        }

        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MasterPasswordGeneratorAction) {
        when (action) {
            MasterPasswordGeneratorAction.BackClickAction -> handleBackAction()
            MasterPasswordGeneratorAction.GeneratePasswordClickAction -> {
                handleGeneratePasswordAction()
            }

            MasterPasswordGeneratorAction.PreventLockoutClickAction -> handlePreventLockoutAction()
            MasterPasswordGeneratorAction.SavePasswordClickAction -> handleSavePasswordAction()
            is MasterPasswordGeneratorAction.Internal -> {
                handleInternalAction(internalAction = action)
            }
        }
    }

    private fun handleBackAction() = sendEvent(MasterPasswordGeneratorEvent.NavigateBack)

    private fun handleSavePasswordAction() {
        generatorRepository.emitGeneratorResult(GeneratorResult.Password(state.generatedPassword))
        sendEvent(MasterPasswordGeneratorEvent.NavigateBackToRegistration)
    }

    private fun handlePreventLockoutAction() =
        sendEvent(MasterPasswordGeneratorEvent.NavigateToPreventLockout)

    private fun handleInternalAction(internalAction: MasterPasswordGeneratorAction.Internal) {
        when (internalAction) {
            is MasterPasswordGeneratorAction.Internal.ReceiveUpdatedPassphraseResultAction -> {
                handleUpdatedPassphraseResult(internalAction.result)
            }
        }
    }

    private fun handleUpdatedPassphraseResult(passphraseResult: GeneratedPassphraseResult) {
        when (passphraseResult) {
            GeneratedPassphraseResult.InvalidRequest -> {
                sendEvent(
                    MasterPasswordGeneratorEvent.ShowSnackbar(
                        R.string.an_error_has_occurred.asText(),
                    ),
                )
            }

            is GeneratedPassphraseResult.Success -> {
                mutableStateFlow.update {
                    it.copy(generatedPassword = passphraseResult.generatedString)
                }
            }
        }
    }

    private fun handleGeneratePasswordAction() = generateNewPassphrase()

    private fun generateNewPassphrase() {
        generatePasswordJob?.cancel()
        generatePasswordJob = viewModelScope.launch {
            val result = generatorRepository.generatePassphrase(
                passphraseGeneratorRequest = passphraseRequest,
            )
            sendAction(
                MasterPasswordGeneratorAction.Internal.ReceiveUpdatedPassphraseResultAction(
                    result = result,
                ),
            )
        }
    }

    private fun getPolicyBasedPassphraseRequest(): PassphraseGeneratorRequest {
        val policy = policyManager
            .getActivePolicies<PolicyInformation.MasterPassword>()
            .toStrictestPolicy()
        val options = generatorRepository.getPasscodeGenerationOptions()
        val optionsWordCount = options?.numWords ?: DEFAULT_WORD_COUNT
        return PassphraseGeneratorRequest(
            numWords = max(optionsWordCount, DEFAULT_WORD_COUNT).toUByte(),
            wordSeparator = options?.wordSeparator ?: DEFAULT_SEPARATOR,
            capitalize = policy.requireUpper == true,
            includeNumber = policy.requireNumbers == true,
        )
    }
}

/**
 * MasterPasswordGeneratorState
 */
@Parcelize
data class MasterPasswordGeneratorState(
    val generatedPassword: String,
) : Parcelable

/**
 * Model events to send to the UI
 */
sealed class MasterPasswordGeneratorEvent {

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : MasterPasswordGeneratorEvent()

    /**
     * Navigate to the prevent account lockout tips screen.
     */
    data object NavigateToPreventLockout : MasterPasswordGeneratorEvent()

    /**
     * Show a Snackbar message.
     */
    data class ShowSnackbar(val text: Text) : MasterPasswordGeneratorEvent()

    /**
     * Navigate back to the complete registration screen.
     */
    data object NavigateBackToRegistration : MasterPasswordGeneratorEvent()
}

/**
 * Model actions from the UI and internal sources for the ViewModel to handle.
 */
sealed class MasterPasswordGeneratorAction {

    /**
     * Internal actions that should only be sent via the owner of the action flow.
     * @see [MasterPasswordGeneratorViewModel]
     */
    @VisibleForTesting
    sealed class Internal : MasterPasswordGeneratorAction() {

        /**
         * Internal action to indicate a generated password result has been received.
         */
        data class ReceiveUpdatedPassphraseResultAction(
            val result: GeneratedPassphraseResult,
        ) : Internal()
    }

    /**
     * Indicate the generate new passphrase button has been clicked.
     */
    data object GeneratePasswordClickAction : MasterPasswordGeneratorAction()

    /**
     * Indicate the prevent lockout link has been clicked.
     */
    data object PreventLockoutClickAction : MasterPasswordGeneratorAction()

    /**
     * Indicate the back arrow button has been clicked.
     */
    data object BackClickAction : MasterPasswordGeneratorAction()

    /**
     * Indicate the save button has been clicked.
     */
    data object SavePasswordClickAction : MasterPasswordGeneratorAction()
}
