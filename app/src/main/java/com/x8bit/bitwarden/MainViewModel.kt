package com.x8bit.bitwarden

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    /**
     * Send a [MainAction].
     */
    fun sendAction(action: MainAction) {
        when (action) {
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(intent = action.intent)
        }
    }

    private fun handleNewIntentReceived(intent: Intent) {
        val captchaCallbackTokenResult = intent.getCaptchaCallbackTokenResult()
        when {
            captchaCallbackTokenResult != null -> {
                authRepository.setCaptchaCallbackTokenResult(
                    tokenResult = captchaCallbackTokenResult,
                )
            }

            else -> Unit
        }
    }
}

/**
 * Models actions for the [MainActivity].
 */
sealed class MainAction {
    /**
     * Receive Intent by the application.
     */
    data class ReceiveNewIntent(val intent: Intent) : MainAction()
}
