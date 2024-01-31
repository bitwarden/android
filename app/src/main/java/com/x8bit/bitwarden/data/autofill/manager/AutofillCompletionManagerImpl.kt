package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.bitwarden.core.CipherView
import com.bitwarden.core.DateTime
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionResultIntent
import com.x8bit.bitwarden.data.autofill.util.getAutofillAssistStructureOrNull
import com.x8bit.bitwarden.data.autofill.util.toAutofillAppInfo
import com.x8bit.bitwarden.data.autofill.util.toAutofillCipherProvider
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Primary implementation of [AutofillCompletionManager].
 */
class AutofillCompletionManagerImpl(
    private val authRepository: AuthRepository,
    private val autofillParser: AutofillParser,
    private val clipboardManager: BitwardenClipboardManager,
    private val dispatcherManager: DispatcherManager,
    private val filledDataBuilderProvider: (CipherView) -> FilledDataBuilder =
        { createSingleItemFilledDataBuilder(cipherView = it) },
    private val vaultRepository: VaultRepository,
) : AutofillCompletionManager {
    private val mainScope = CoroutineScope(dispatcherManager.main)

    override fun completeAutofill(
        activity: Activity,
        cipherView: CipherView,
    ) {
        val autofillAppInfo = activity.toAutofillAppInfo()
        val assistStructure = activity
            .intent
            ?.getAutofillAssistStructureOrNull()
            ?: run {
                activity.cancelAndFinish()
                return
            }

        val autofillRequest = autofillParser
            .parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        if (autofillRequest !is AutofillRequest.Fillable) {
            activity.cancelAndFinish()
            return
        }

        val fillDataBuilder = filledDataBuilderProvider(cipherView)
        // We'll launch a coroutine here but this code will technically run synchronously given
        // how we've constructed a single-item AutofillCipherProvider.
        mainScope.launch {
            val dataset = fillDataBuilder
                .build(autofillRequest)
                .filledPartitions
                .firstOrNull()
                ?.buildDataset(
                    autofillAppInfo = autofillAppInfo,
                    authIntentSender = null,
                )
                ?: run {
                    activity.cancelAndFinish()
                    return@launch
                }
            tryCopyTotpToClipboard(
                activity = activity,
                cipherView = cipherView,
            )
            val resultIntent = createAutofillSelectionResultIntent(dataset)
            activity.setResultAndFinish(resultIntent = resultIntent)
        }
    }

    /**
     * Attempt to copy the totp code to clipboard. If it succeeds show a toast.
     *
     * @param activity An activity for launching a toast.
     * @param cipherView The [CipherView] for which to generate a TOTP code.
     */
    private suspend fun tryCopyTotpToClipboard(
        activity: Activity,
        cipherView: CipherView,
    ) {
        val isPremium = authRepository.userStateFlow.value?.activeAccount?.isPremium == true
        val totpCode = cipherView.login?.totp

        // TODO check global TOTP enabled status BIT-1093
        if (isPremium && totpCode != null) {
            val totpResult = vaultRepository.generateTotp(
                time = DateTime.now(),
                totpCode = totpCode,
            )

            if (totpResult is GenerateTotpResult.Success) {
                clipboardManager.setText(totpResult.code)
                Toast
                    .makeText(
                        activity.applicationContext,
                        R.string.verification_code_totp,
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
    }
}

private fun createSingleItemFilledDataBuilder(
    cipherView: CipherView,
): FilledDataBuilder =
    FilledDataBuilderImpl(
        autofillCipherProvider = cipherView.toAutofillCipherProvider(),
    )

private fun Activity.cancelAndFinish() {
    this.setResult(Activity.RESULT_CANCELED)
    this.finish()
}

private fun Activity.setResultAndFinish(resultIntent: Intent) {
    this.setResult(Activity.RESULT_OK, resultIntent)
    this.finish()
}
