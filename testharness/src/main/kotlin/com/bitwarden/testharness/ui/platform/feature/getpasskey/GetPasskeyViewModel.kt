package com.bitwarden.testharness.ui.platform.feature.getpasskey

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.testharness.data.manager.CredentialTestManager
import com.bitwarden.testharness.data.model.CredentialTestResult
import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val MAX_STACK_TRACE_LINES = 5
private const val RESULT_SEPARATOR_LENGTH = 40

/**
 * ViewModel for Get Passkey test screen.
 */
@HiltViewModel
class GetPasskeyViewModel @Inject constructor(
    private val credentialTestManager: CredentialTestManager,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<GetPasskeyState, GetPasskeyEvent, GetPasskeyAction>(
    initialState = savedStateHandle[KEY_STATE] ?: GetPasskeyState(),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: GetPasskeyAction) {
        when (action) {
            is GetPasskeyAction.RpIdChanged -> handleRpIdChanged(action)
            is GetPasskeyAction.OriginChanged -> handleOriginChanged(action)
            GetPasskeyAction.ExecuteClick -> handleExecuteClick()
            GetPasskeyAction.ClearResultClick -> handleClearResultClick()
            GetPasskeyAction.BackClick -> handleBackClick()
            is GetPasskeyAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(GetPasskeyEvent.NavigateBack)
    }

    private fun handleRpIdChanged(action: GetPasskeyAction.RpIdChanged) {
        mutableStateFlow.update {
            it.copy(rpId = action.rpId)
        }
    }

    private fun handleOriginChanged(action: GetPasskeyAction.OriginChanged) {
        mutableStateFlow.update {
            it.copy(origin = action.origin)
        }
    }

    private fun handleExecuteClick() {
        val currentState = stateFlow.value
        val rpId = currentState.rpId
        val origin = currentState.origin.takeIf { it.isNotBlank() }

        if (rpId.isBlank()) {
            val errorMessage = "\n${timestamp()} ⚠️ Validation Error: " +
                "Relying Party ID is required\n"
            mutableStateFlow.update {
                it.copy(
                    resultText = it.resultText + errorMessage + "\n" +
                        "─".repeat(RESULT_SEPARATOR_LENGTH) + "\n",
                )
            }
            return
        }

        val startMessage = "\n${timestamp()} ⏳ Starting passkey authentication...\n"
        mutableStateFlow.update {
            it.copy(
                isLoading = true,
                resultText = it.resultText + startMessage,
            )
        }

        viewModelScope.launch {
            val result = credentialTestManager.getPasskey(
                rpId = rpId,
                origin = origin,
            )
            sendAction(GetPasskeyAction.Internal.CredentialResultReceived(result))
        }
    }

    private fun handleClearResultClick() {
        mutableStateFlow.update {
            it.copy(resultText = "Result cleared.\n")
        }
    }

    private fun handleInternalAction(action: GetPasskeyAction.Internal) {
        when (action) {
            is GetPasskeyAction.Internal.CredentialResultReceived -> {
                handleCredentialResultReceived(action)
            }
        }
    }

    private fun handleCredentialResultReceived(
        action: GetPasskeyAction.Internal.CredentialResultReceived,
    ) {
        val resultMessage = when (val result = action.result) {
            is CredentialTestResult.Success -> {
                buildString {
                    append("${timestamp()} ✅ SUCCESS: Passkey authenticated successfully\n")
                    if (result.data != null) {
                        append("\n📋 Response Data:\n${result.data}\n")
                    }
                }
            }

            is CredentialTestResult.Error -> {
                buildString {
                    val errorMessage = result.exception?.message ?: "Unknown error"
                    append(
                        "${timestamp()} ❌ ERROR: Failed to authenticate passkey: " +
                            "$errorMessage\n",
                    )
                    if (result.exception != null) {
                        append("\n🔍 Exception:\n${result.exception}\n")
                        append("\nStack trace:\n")
                        result.exception.stackTrace.take(MAX_STACK_TRACE_LINES).forEach {
                            append("  at $it\n")
                        }
                    }
                }
            }

            CredentialTestResult.Cancelled -> {
                "${timestamp()} 🚫 CANCELLED: User cancelled the operation\n"
            }
        }

        mutableStateFlow.update {
            it.copy(
                isLoading = false,
                resultText = it.resultText + resultMessage + "\n" +
                    "─".repeat(RESULT_SEPARATOR_LENGTH) + "\n",
            )
        }
    }

    private fun timestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return clock.instant()
            .atZone(ZoneId.systemDefault())
            .format(formatter)
            .let { "[$it]" }
    }
}

/**
 * State for Get Passkey screen.
 */
@Parcelize
data class GetPasskeyState(
    val rpId: String = "",
    val origin: String = "",
    val isLoading: Boolean = false,
    val resultText: String = "Ready to authenticate passkey.\n\n" +
        "Enter Relying Party ID, then click Execute.",
) : Parcelable

/**
 * Events for Get Passkey screen.
 */
sealed class GetPasskeyEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : GetPasskeyEvent()
}

/**
 * Actions for Get Passkey screen.
 */
sealed class GetPasskeyAction {
    /**
     * Relying Party ID input value changed.
     */
    data class RpIdChanged(val rpId: String) : GetPasskeyAction()

    /**
     * Origin input value changed.
     */
    data class OriginChanged(val origin: String) : GetPasskeyAction()

    /**
     * User clicked execute button to authenticate passkey.
     */
    data object ExecuteClick : GetPasskeyAction()

    /**
     * User clicked clear button to reset results.
     */
    data object ClearResultClick : GetPasskeyAction()

    /**
     * User clicked back button.
     */
    data object BackClick : GetPasskeyAction()

    /**
     * Internal actions for Get Passkey screen.
     */
    sealed class Internal : GetPasskeyAction() {
        /**
         * Credential operation result received.
         */
        data class CredentialResultReceived(
            val result: CredentialTestResult,
        ) : Internal()
    }
}
