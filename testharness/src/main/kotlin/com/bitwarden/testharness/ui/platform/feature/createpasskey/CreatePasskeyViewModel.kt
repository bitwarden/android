package com.bitwarden.testharness.ui.platform.feature.createpasskey

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
 * ViewModel for Create Passkey test screen.
 */
@HiltViewModel
class CreatePasskeyViewModel @Inject constructor(
    private val credentialTestManager: CredentialTestManager,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CreatePasskeyState, CreatePasskeyEvent, CreatePasskeyAction>(
    initialState = savedStateHandle[KEY_STATE] ?: CreatePasskeyState(),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CreatePasskeyAction) {
        when (action) {
            is CreatePasskeyAction.UsernameChanged -> handleUsernameChanged(action)
            is CreatePasskeyAction.RpIdChanged -> handleRpIdChanged(action)
            is CreatePasskeyAction.OriginChanged -> handleOriginChanged(action)
            CreatePasskeyAction.ExecuteClick -> handleExecuteClick()
            CreatePasskeyAction.ClearResultClick -> handleClearResultClick()
            CreatePasskeyAction.BackClick -> handleBackClick()
            is CreatePasskeyAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(CreatePasskeyEvent.NavigateBack)
    }

    private fun handleUsernameChanged(action: CreatePasskeyAction.UsernameChanged) {
        mutableStateFlow.update {
            it.copy(username = action.username)
        }
    }

    private fun handleRpIdChanged(action: CreatePasskeyAction.RpIdChanged) {
        mutableStateFlow.update {
            it.copy(rpId = action.rpId)
        }
    }

    private fun handleOriginChanged(action: CreatePasskeyAction.OriginChanged) {
        mutableStateFlow.update {
            it.copy(origin = action.origin)
        }
    }

    private fun handleExecuteClick() {
        val currentState = stateFlow.value
        val username = currentState.username
        val rpId = currentState.rpId
        val origin = currentState.origin.takeIf { it.isNotBlank() }

        if (username.isBlank() || rpId.isBlank()) {
            val errorMessage = "\n${timestamp()} ⚠️ Validation Error: " +
                "Username and Relying Party ID are required\n"
            mutableStateFlow.update {
                it.copy(
                    resultText = it.resultText + errorMessage + "\n" +
                        "─".repeat(RESULT_SEPARATOR_LENGTH) + "\n",
                )
            }
            return
        }

        val startMessage = "\n${timestamp()} ⏳ Creating passkey credential...\n"
        mutableStateFlow.update {
            it.copy(
                isLoading = true,
                resultText = it.resultText + startMessage,
            )
        }

        viewModelScope.launch {
            val result = credentialTestManager.createPasskey(
                username = username,
                rpId = rpId,
                origin = origin,
            )
            sendAction(CreatePasskeyAction.Internal.CredentialResultReceived(result))
        }
    }

    private fun handleClearResultClick() {
        mutableStateFlow.update {
            it.copy(resultText = "Result cleared.\n")
        }
    }

    private fun handleInternalAction(action: CreatePasskeyAction.Internal) {
        when (action) {
            is CreatePasskeyAction.Internal.CredentialResultReceived -> {
                handleCredentialResultReceived(action)
            }
        }
    }

    private fun handleCredentialResultReceived(
        action: CreatePasskeyAction.Internal.CredentialResultReceived,
    ) {
        val resultMessage = when (val result = action.result) {
            is CredentialTestResult.Success -> {
                buildString {
                    append("${timestamp()} ✅ SUCCESS: Passkey created successfully\n")
                    if (result.data != null) {
                        append("\n📋 Response Data:\n${result.data}\n")
                    }
                }
            }

            is CredentialTestResult.Error -> {
                buildString {
                    val errorMessage = result.exception?.message ?: "Unknown error"
                    append("${timestamp()} ❌ ERROR: Failed to create passkey: $errorMessage\n")
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
 * State for Create Passkey screen.
 */
@Parcelize
data class CreatePasskeyState(
    val username: String = "",
    val rpId: String = "",
    val origin: String = "",
    val isLoading: Boolean = false,
    val resultText: String = "Ready to create passkey credential.\n\n" +
        "Enter username and Relying Party ID, then click Execute.",
) : Parcelable

/**
 * Events for Create Passkey screen.
 */
sealed class CreatePasskeyEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CreatePasskeyEvent()
}

/**
 * Actions for Create Passkey screen.
 */
sealed class CreatePasskeyAction {
    /**
     * Username input value changed.
     */
    data class UsernameChanged(val username: String) : CreatePasskeyAction()

    /**
     * Relying Party ID input value changed.
     */
    data class RpIdChanged(val rpId: String) : CreatePasskeyAction()

    /**
     * Origin input value changed.
     */
    data class OriginChanged(val origin: String) : CreatePasskeyAction()

    /**
     * User clicked execute button to create passkey credential.
     */
    data object ExecuteClick : CreatePasskeyAction()

    /**
     * User clicked clear button to reset results.
     */
    data object ClearResultClick : CreatePasskeyAction()

    /**
     * User clicked back button.
     */
    data object BackClick : CreatePasskeyAction()

    /**
     * Internal actions for Create Passkey screen.
     */
    sealed class Internal : CreatePasskeyAction() {
        /**
         * Credential operation result received.
         */
        data class CredentialResultReceived(
            val result: CredentialTestResult,
        ) : Internal()
    }
}
