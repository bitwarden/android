package com.x8bit.bitwarden

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * An activity for copying a TOTP code to the clipboard. This is done when an autofill item is
 * selected and it requires TOTP authentication. Due to the constraints of the autofill framework,
 * we also have to re-fulfill the autofill for the views that are being filled.
 */
@OmitFromCoverage
@AndroidEntryPoint
class AutofillTotpCopyActivity : AppCompatActivity() {

    @Inject
    lateinit var autofillCompletionManager: AutofillCompletionManager

    private val autofillTotpCopyViewModel: AutofillTotpCopyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModelEvents()

        autofillTotpCopyViewModel.trySendAction(
            AutofillTotpCopyAction.IntentReceived(
                intent = intent,
            ),
        )
    }

    private fun observeViewModelEvents() {
        autofillTotpCopyViewModel
            .eventFlow
            .onEach { event ->
                when (event) {
                    is AutofillTotpCopyEvent.CompleteAutofill -> {
                        handleCompleteAutofill(event)
                    }

                    is AutofillTotpCopyEvent.FinishActivity -> {
                        finishActivity()
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    /**
     * Complete autofill with the provided data.
     */
    private fun handleCompleteAutofill(event: AutofillTotpCopyEvent.CompleteAutofill) {
        autofillCompletionManager.completeAutofill(
            activity = this,
            cipherView = event.cipherView,
        )
    }

    /**
     * Finish the activity.
     */
    private fun finishActivity() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
