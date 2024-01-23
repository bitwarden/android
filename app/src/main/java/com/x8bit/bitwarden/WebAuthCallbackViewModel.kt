package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * A view model that handles logic for the [WebAuthCallbackActivity].
 */
@HiltViewModel
class WebAuthCallbackViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<Unit, Unit, WebAuthCallbackAction>(Unit) {
    override fun handleAction(action: WebAuthCallbackAction) {
        when (action) {
            is WebAuthCallbackAction.IntentReceive -> handleIntentReceived(action)
        }
    }

    private fun handleIntentReceived(action: WebAuthCallbackAction.IntentReceive) {
        val captchaCallbackTokenResult = action.intent.getCaptchaCallbackTokenResult()
        val ssoCallbackResult = action.intent.getSsoCallbackResult()
        when {
            captchaCallbackTokenResult != null -> {
                authRepository.setCaptchaCallbackTokenResult(
                    tokenResult = captchaCallbackTokenResult,
                )
            }

            ssoCallbackResult != null -> {
                authRepository.setSsoCallbackResult(
                    result = ssoCallbackResult,
                )
            }

            else -> Unit
        }
    }
}

/**
 * Actions for the [WebAuthCallbackViewModel].
 */
sealed class WebAuthCallbackAction {
    /**
     * Receive Intent by the application.
     */
    data class IntentReceive(val intent: Intent) : WebAuthCallbackAction()
}
