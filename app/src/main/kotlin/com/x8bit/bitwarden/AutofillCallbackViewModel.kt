package com.x8bit.bitwarden

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.getAutofillCallbackIntentOrNull
import com.x8bit.bitwarden.data.platform.util.launchWithTimeout
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * The amount of time we should wait for ciphers to be loaded before timing out.
 */
private const val CIPHER_WAIT_TIMEOUT_MILLIS: Long = 500

/**
 * A view model that handles logic for the [AutofillCallbackActivity].
 */
@HiltViewModel
class AutofillCallbackViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<Unit, AutofillCallbackEvent, AutofillCallbackAction>(Unit) {
    private val activeUserId: String? get() = authRepository.activeUserId

    override fun handleAction(action: AutofillCallbackAction): Unit = when (action) {
        is AutofillCallbackAction.IntentReceived -> handleIntentReceived(action)
    }

    /**
     * Process the received intent and alert the activity of what to do next.
     */
    private fun handleIntentReceived(action: AutofillCallbackAction.IntentReceived) {
        viewModelScope
            .launchWithTimeout(
                timeoutBlock = {
                    Timber.w("Autofill -- Timeout")
                    finishActivity()
                },
                timeoutDuration = CIPHER_WAIT_TIMEOUT_MILLIS,
            ) {
                // Extract TOTP copy data from the intent.
                val cipherId = action
                    .intent
                    .getAutofillCallbackIntentOrNull()
                    ?.cipherId

                if (cipherId == null) {
                    Timber.w("Autofill -- Cipher was not provided")
                    finishActivity()
                    return@launchWithTimeout
                }
                if (isVaultLocked()) {
                    Timber.w("Autofill -- Vault is locked")
                    finishActivity()
                    return@launchWithTimeout
                }

                // Try and find the matching cipher.
                when (val result = vaultRepository.getCipher(cipherId = cipherId)) {
                    GetCipherResult.CipherNotFound -> {
                        Timber.w("Autofill -- Cipher not found")
                        finishActivity()
                    }

                    is GetCipherResult.Failure -> {
                        Timber.w(result.error, "Autofill -- Get cipher failure")
                        finishActivity()
                    }

                    is GetCipherResult.Success -> {
                        Timber.d("Autofill -- Cipher found")
                        sendEvent(AutofillCallbackEvent.CompleteAutofill(result.cipherView))
                    }
                }
            }
    }

    /**
     * Send an event to the activity that signals it to finish.
     */
    private fun finishActivity() {
        sendEvent(AutofillCallbackEvent.FinishActivity)
    }

    private suspend fun isVaultLocked(): Boolean {
        val userId = activeUserId ?: return true

        // Wait for any unlocking actions to finish. This can be relevant on startup for Never lock
        // accounts.
        vaultRepository.vaultUnlockDataStateFlow.first {
            it.statusFor(userId) != VaultUnlockData.Status.UNLOCKING
        }

        return !vaultRepository.isVaultUnlocked(userId = userId)
    }
}

/**
 * Represents actions that can be sent to the [AutofillCallbackViewModel].
 */
sealed class AutofillCallbackAction {
    /**
     * An [intent] has been received and is ready to be processed.
     */
    data class IntentReceived(
        val intent: Intent,
    ) : AutofillCallbackAction()
}

/**
 * Represents events emitted by the [AutofillCallbackViewModel].
 */
sealed class AutofillCallbackEvent {
    /**
     * Complete autofill with the provided [cipherView].
     */
    data class CompleteAutofill(
        val cipherView: CipherView,
    ) : AutofillCallbackEvent()

    /**
     * Finish the activity.
     */
    data object FinishActivity : AutofillCallbackEvent()
}
