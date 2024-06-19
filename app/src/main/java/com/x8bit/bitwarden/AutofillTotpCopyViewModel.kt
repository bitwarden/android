package com.x8bit.bitwarden

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.getTotpCopyIntentOrNull
import com.x8bit.bitwarden.data.platform.util.launchWithTimeout
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

/**
 * The amount of time we should wait for ciphers to be loaded before timing out.
 */
private const val CIPHER_WAIT_TIMEOUT_MILLIS: Long = 500

/**
 * A view model that handles logic for the [AutofillTotpCopyActivity].
 */
@HiltViewModel
class AutofillTotpCopyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<Unit, AutofillTotpCopyEvent, AutofillTotpCopyAction>(Unit) {
    private val activeUserId: String? get() = authRepository.activeUserId

    override fun handleAction(action: AutofillTotpCopyAction): Unit = when (action) {
        is AutofillTotpCopyAction.IntentReceived -> handleIntentReceived(action)
    }

    /**
     * Process the received intent and alert the activity of what to do next.
     */
    private fun handleIntentReceived(action: AutofillTotpCopyAction.IntentReceived) {
        viewModelScope
            .launchWithTimeout(
                timeoutBlock = { finishActivity() },
                timeoutDuration = CIPHER_WAIT_TIMEOUT_MILLIS,
            ) {
                // Extract TOTP copy data from the intent.
                val cipherId = action
                    .intent
                    .getTotpCopyIntentOrNull()
                    ?.cipherId

                if (cipherId == null || isVaultLocked()) {
                    finishActivity()
                    return@launchWithTimeout
                }

                // Try and find the matching cipher.
                vaultRepository
                    .ciphersStateFlow
                    .mapNotNull { it.data }
                    .first()
                    .find { it.id == cipherId }
                    ?.let { cipherView ->
                        sendEvent(
                            AutofillTotpCopyEvent.CompleteAutofill(
                                cipherView = cipherView,
                            ),
                        )
                    }
                    ?: finishActivity()
            }
    }

    /**
     * Send an event to the activity that signals it to finish.
     */
    private fun finishActivity() {
        sendEvent(AutofillTotpCopyEvent.FinishActivity)
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
 * Represents actions that can be sent to the [AutofillTotpCopyViewModel].
 */
sealed class AutofillTotpCopyAction {
    /**
     * An [intent] has been received and is ready to be processed.
     */
    data class IntentReceived(
        val intent: Intent,
    ) : AutofillTotpCopyAction()
}

/**
 * Represents events emitted by the [AutofillTotpCopyViewModel].
 */
sealed class AutofillTotpCopyEvent {
    /**
     * Complete autofill with the provided [cipherView].
     */
    data class CompleteAutofill(
        val cipherView: CipherView,
    ) : AutofillTotpCopyEvent()

    /**
     * Finish the activity.
     */
    data object FinishActivity : AutofillTotpCopyEvent()
}
