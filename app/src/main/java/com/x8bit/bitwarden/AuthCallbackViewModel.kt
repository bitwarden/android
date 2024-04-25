package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getDuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.getWebAuthResultOrNull
import com.x8bit.bitwarden.data.auth.util.getYubiKeyResultOrNull
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * A view model that handles logic for the [AuthCallbackActivity].
 */
@HiltViewModel
class AuthCallbackViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<Unit, Unit, AuthCallbackAction>(Unit) {
    override fun handleAction(action: AuthCallbackAction) {
        when (action) {
            is AuthCallbackAction.IntentReceive -> handleIntentReceived(action)
        }
    }

    private fun handleIntentReceived(action: AuthCallbackAction.IntentReceive) {
        val yubiKeyResult = action.intent.getYubiKeyResultOrNull()
        val webAuthResult = action.intent.getWebAuthResultOrNull()
        val captchaCallbackTokenResult = action.intent.getCaptchaCallbackTokenResult()
        val duoCallbackTokenResult = action.intent.getDuoCallbackTokenResult()
        val ssoCallbackResult = action.intent.getSsoCallbackResult()
        when {
            yubiKeyResult != null -> {
                authRepository.setYubiKeyResult(yubiKeyResult = yubiKeyResult)
            }

            captchaCallbackTokenResult != null -> {
                authRepository.setCaptchaCallbackTokenResult(
                    tokenResult = captchaCallbackTokenResult,
                )
            }

            duoCallbackTokenResult != null -> {
                authRepository.setDuoCallbackTokenResult(
                    tokenResult = duoCallbackTokenResult,
                )
            }

            ssoCallbackResult != null -> {
                authRepository.setSsoCallbackResult(
                    result = ssoCallbackResult,
                )
            }

            webAuthResult != null -> {
                authRepository.setWebAuthResult(webAuthResult = webAuthResult)
            }

            else -> Unit
        }
    }
}

/**
 * Actions for the [AuthCallbackViewModel].
 */
sealed class AuthCallbackAction {
    /**
     * Receive Intent by the application.
     */
    data class IntentReceive(val intent: Intent) : AuthCallbackAction()
}
