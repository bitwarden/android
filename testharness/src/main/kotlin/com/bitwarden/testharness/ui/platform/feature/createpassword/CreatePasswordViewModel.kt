package com.bitwarden.testharness.ui.platform.feature.createpassword

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
 * ViewModel for Create Password test screen.
 */
@HiltViewModel
class CreatePasswordViewModel @Inject constructor(
    private val credentialTestManager: CredentialTestManager,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CreatePasswordState, CreatePasswordEvent, CreatePasswordAction>(
    initialState = savedStateHandle[KEY_STATE] ?: CreatePasswordState(),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CreatePasswordAction) {
        when (action) {
            is CreatePasswordAction.UsernameChanged -> handleUsernameChanged(action)
            is CreatePasswordAction.PasswordChanged -> handlePasswordChanged(action)
            CreatePasswordAction.ExecuteClick -> handleExecuteClick()
            CreatePasswordAction.ClearResultClick -> handleClearResultClick()
            CreatePasswordAction.BackClick -> handleBackClick()
            is CreatePasswordAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(CreatePasswordEvent.NavigateBack)
    }

    private fun handleUsernameChanged(action: CreatePasswordAction.UsernameChanged) {
        mutableStateFlow.update {
            it.copy(username = action.username)
        }
    }

    private fun handlePasswordChanged(action: CreatePasswordAction.PasswordChanged) {
        mutableStateFlow.update {
            it.copy(password = action.password)
        }
    }

    private fun handleExecuteClick() {
        val currentState = stateFlow.value
        val username = currentState.username
        val password = currentState.password

        if (username.isBlank() || password.isBlank()) {
            val errorMessage = "\n${timestamp()} ⚠️ Validation Error: " +
                "Username and password are required\n"
            mutableStateFlow.update {
                it.copy(
                    resultText = it.resultText + errorMessage + "\n" +
                        "─".repeat(RESULT_SEPARATOR_LENGTH) + "\n",
                )
            }
            return
        }

        val startMessage = "\n${timestamp()} ⏳ Creating password credential...\n"
        mutableStateFlow.update {
            it.copy(
                isLoading = true,
                resultText = it.resultText + startMessage,
            )
        }

        viewModelScope.launch {
            val result = credentialTestManager.createPassword(
                username = username,
                password = password,
                origin = null,
            )
            sendAction(CreatePasswordAction.Internal.CredentialResultReceived(result))
        }
    }

    private fun handleClearResultClick() {
        mutableStateFlow.update {
            it.copy(resultText = "Result cleared.\n")
        }
    }

    private fun handleInternalAction(action: CreatePasswordAction.Internal) {
        when (action) {
            is CreatePasswordAction.Internal.CredentialResultReceived -> {
                handleCredentialResultReceived(action)
            }
        }
    }

    private fun handleCredentialResultReceived(
        action: CreatePasswordAction.Internal.CredentialResultReceived,
    ) {
        val resultMessage = when (val result = action.result) {
            is CredentialTestResult.Success -> {
                buildString {
                    append("${timestamp()} ✅ SUCCESS: Password created successfully\n")
                    if (result.data != null) {
                        append("\n📋 Response Data:\n${result.data}\n")
                    }
                }
            }

            is CredentialTestResult.Error -> {
                buildString {
                    val errorMessage = result.exception?.message ?: "Unknown error"
                    append("${timestamp()} ❌ ERROR: Failed to create password: $errorMessage\n")
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
 * State for Create Password screen.
 */
@Parcelize
data class CreatePasswordState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val resultText: String = "Ready to create password credential.\n\n" +
        "Enter username and password, then click Execute.",
) : Parcelable

/**
 * Events for Create Password screen.
 */
sealed class CreatePasswordEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CreatePasswordEvent()
}

/**
 * Actions for Create Password screen.
 */
sealed class CreatePasswordAction {
    /**
     * Username input value changed.
     */
    data class UsernameChanged(val username: String) : CreatePasswordAction()

    /**
     * Password input value changed.
     */
    data class PasswordChanged(val password: String) : CreatePasswordAction()

    /**
     * User clicked execute button to create password credential.
     */
    data object ExecuteClick : CreatePasswordAction()

    /**
     * User clicked clear button to reset results.
     */
    data object ClearResultClick : CreatePasswordAction()

    /**
     * User clicked back button.
     */
    data object BackClick : CreatePasswordAction()

    /**
     * Internal actions for Create Password screen.
     */
    sealed class Internal : CreatePasswordAction() {
        /**
         * Credential operation result received.
         */
        data class CredentialResultReceived(
            val result: CredentialTestResult,
        ) : Internal()
    }
}
