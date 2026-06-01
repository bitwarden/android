package com.bitwarden.testharness.ui.platform.feature.getpassword

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
 * ViewModel for Get Password test screen.
 */
@HiltViewModel
class GetPasswordViewModel @Inject constructor(
    private val credentialTestManager: CredentialTestManager,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<GetPasswordState, GetPasswordEvent, GetPasswordAction>(
    initialState = savedStateHandle[KEY_STATE] ?: GetPasswordState(),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: GetPasswordAction) {
        when (action) {
            GetPasswordAction.ExecuteClick -> handleExecuteClick()
            GetPasswordAction.ClearResultClick -> handleClearResultClick()
            GetPasswordAction.BackClick -> handleBackClick()
            is GetPasswordAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(GetPasswordEvent.NavigateBack)
    }

    private fun handleExecuteClick() {
        val startMessage = "\n${timestamp()} ⏳ Starting password retrieval...\n"
        mutableStateFlow.update {
            it.copy(
                isLoading = true,
                resultText = it.resultText + startMessage,
            )
        }

        viewModelScope.launch {
            val result = credentialTestManager.getPassword()
            sendAction(GetPasswordAction.Internal.CredentialResultReceived(result))
        }
    }

    private fun handleClearResultClick() {
        mutableStateFlow.update {
            it.copy(resultText = "Result cleared.\n")
        }
    }

    private fun handleInternalAction(action: GetPasswordAction.Internal) {
        when (action) {
            is GetPasswordAction.Internal.CredentialResultReceived -> {
                handleCredentialResultReceived(action)
            }
        }
    }

    private fun handleCredentialResultReceived(
        action: GetPasswordAction.Internal.CredentialResultReceived,
    ) {
        val resultMessage = when (val result = action.result) {
            is CredentialTestResult.Success -> {
                buildString {
                    append("${timestamp()} ✅ SUCCESS: Password retrieved successfully\n")
                    if (result.data != null) {
                        append("\n📋 Response Data:\n${result.data}\n")
                    }
                }
            }

            is CredentialTestResult.Error -> {
                buildString {
                    val errorMessage = result.exception?.message ?: "Unknown error"
                    append("${timestamp()} ❌ ERROR: Failed to get password: $errorMessage\n")
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
 * State for Get Password screen.
 */
@Parcelize
data class GetPasswordState(
    val isLoading: Boolean = false,
    val resultText: String = "Ready to retrieve password.\n\n" +
        "Click Execute to open the password picker.",
) : Parcelable

/**
 * Events for Get Password screen.
 */
sealed class GetPasswordEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : GetPasswordEvent()
}

/**
 * Actions for Get Password screen.
 */
sealed class GetPasswordAction {
    /**
     * User clicked execute button to retrieve password.
     */
    data object ExecuteClick : GetPasswordAction()

    /**
     * User clicked clear button to reset results.
     */
    data object ClearResultClick : GetPasswordAction()

    /**
     * User clicked back button.
     */
    data object BackClick : GetPasswordAction()

    /**
     * Internal actions for Get Password screen.
     */
    sealed class Internal : GetPasswordAction() {
        /**
         * Credential operation result received.
         */
        data class CredentialResultReceived(
            val result: CredentialTestResult,
        ) : Internal()
    }
}
