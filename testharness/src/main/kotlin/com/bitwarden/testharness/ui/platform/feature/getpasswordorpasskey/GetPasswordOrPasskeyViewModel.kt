package com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey

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
 * ViewModel for Get Password or Passkey test screen.
 *
 * This ViewModel handles the combined credential retrieval test where both password and
 * passkey options are included in a single GetCredentialRequest.
 */
@HiltViewModel
class GetPasswordOrPasskeyViewModel @Inject constructor(
    private val credentialTestManager: CredentialTestManager,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<GetPasswordOrPasskeyState, GetPasswordOrPasskeyEvent, GetPasswordOrPasskeyAction>(
    initialState = savedStateHandle[KEY_STATE] ?: GetPasswordOrPasskeyState(),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: GetPasswordOrPasskeyAction) {
        when (action) {
            is GetPasswordOrPasskeyAction.RpIdChanged -> handleRpIdChanged(action)
            is GetPasswordOrPasskeyAction.OriginChanged -> handleOriginChanged(action)
            GetPasswordOrPasskeyAction.ExecuteClick -> handleExecuteClick()
            GetPasswordOrPasskeyAction.ClearResultClick -> handleClearResultClick()
            GetPasswordOrPasskeyAction.BackClick -> handleBackClick()
            is GetPasswordOrPasskeyAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(GetPasswordOrPasskeyEvent.NavigateBack)
    }

    private fun handleRpIdChanged(action: GetPasswordOrPasskeyAction.RpIdChanged) {
        mutableStateFlow.update {
            it.copy(rpId = action.rpId)
        }
    }

    private fun handleOriginChanged(action: GetPasswordOrPasskeyAction.OriginChanged) {
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

        val startMessage = "\n${timestamp()} ⏳ Starting credential retrieval...\n"
        mutableStateFlow.update {
            it.copy(
                isLoading = true,
                resultText = it.resultText + startMessage,
            )
        }

        viewModelScope.launch {
            val result = credentialTestManager.getPasswordOrPasskey(
                rpId = rpId,
                origin = origin,
            )
            sendAction(GetPasswordOrPasskeyAction.Internal.CredentialResultReceived(result))
        }
    }

    private fun handleClearResultClick() {
        mutableStateFlow.update {
            it.copy(resultText = "Result cleared.\n")
        }
    }

    private fun handleInternalAction(action: GetPasswordOrPasskeyAction.Internal) {
        when (action) {
            is GetPasswordOrPasskeyAction.Internal.CredentialResultReceived -> {
                handleCredentialResultReceived(action)
            }
        }
    }

    private fun handleCredentialResultReceived(
        action: GetPasswordOrPasskeyAction.Internal.CredentialResultReceived,
    ) {
        val resultMessage = when (val result = action.result) {
            is CredentialTestResult.Success -> {
                buildString {
                    // Determine credential type from data to construct appropriate message
                    val successMessage = when {
                        result.data?.startsWith("Type: PASSWORD") == true ->
                            "Password retrieved successfully"

                        result.data?.startsWith("Type: PASSKEY") == true ->
                            "Passkey authenticated successfully"

                        else -> "Credential retrieved successfully"
                    }
                    append("${timestamp()} ✅ SUCCESS: $successMessage\n")
                    if (result.data != null) {
                        append("\n📋 Response Data:\n${result.data}\n")
                    }
                }
            }

            is CredentialTestResult.Error -> {
                buildString {
                    val errorMessage = result.exception?.message ?: "Unknown error"
                    append("${timestamp()} ❌ ERROR: Failed to get credential: $errorMessage\n")
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
 * State for Get Password or Passkey screen.
 */
@Parcelize
data class GetPasswordOrPasskeyState(
    val rpId: String = "",
    val origin: String = "",
    val isLoading: Boolean = false,
    val resultText: String = "Ready to retrieve password or passkey.\n\n" +
        "Enter Relying Party ID, then click Execute.\n" +
        "System picker will show both passwords and passkeys.",
) : Parcelable

/**
 * Events for Get Password or Passkey screen.
 */
sealed class GetPasswordOrPasskeyEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : GetPasswordOrPasskeyEvent()
}

/**
 * Actions for Get Password or Passkey screen.
 */
sealed class GetPasswordOrPasskeyAction {
    /**
     * Relying Party ID input value changed.
     */
    data class RpIdChanged(val rpId: String) : GetPasswordOrPasskeyAction()

    /**
     * Origin input value changed.
     */
    data class OriginChanged(val origin: String) : GetPasswordOrPasskeyAction()

    /**
     * User clicked execute button to retrieve password or passkey.
     */
    data object ExecuteClick : GetPasswordOrPasskeyAction()

    /**
     * User clicked clear button to reset results.
     */
    data object ClearResultClick : GetPasswordOrPasskeyAction()

    /**
     * User clicked back button.
     */
    data object BackClick : GetPasswordOrPasskeyAction()

    /**
     * Internal actions for Get Password or Passkey screen.
     */
    sealed class Internal : GetPasswordOrPasskeyAction() {
        /**
         * Credential operation result received.
         */
        data class CredentialResultReceived(
            val result: CredentialTestResult,
        ) : Internal()
    }
}
