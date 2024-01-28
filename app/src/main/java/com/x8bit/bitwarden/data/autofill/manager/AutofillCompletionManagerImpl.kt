package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity
import android.content.Intent
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilderImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionResultIntent
import com.x8bit.bitwarden.data.autofill.util.getAutofillAssistStructureOrNull
import com.x8bit.bitwarden.data.autofill.util.toAutofillAppInfo
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.util.subtitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Primary implementation of [AutofillCompletionManager].
 */
class AutofillCompletionManagerImpl(
    private val autofillParser: AutofillParser,
    private val dispatcherManager: DispatcherManager,
    private val filledDataBuilderProvider: (CipherView) -> FilledDataBuilder =
        { createSingleItemFilledDataBuilder(cipherView = it) },
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
                ?.buildDataset(autofillAppInfo = autofillAppInfo)
                ?: run {
                    activity.cancelAndFinish()
                    return@launch
                }
            val resultIntent = createAutofillSelectionResultIntent(dataset)
            activity.setResultAndFinish(resultIntent = resultIntent)
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

private fun CipherView.toAutofillCipherProvider(): AutofillCipherProvider =
    object : AutofillCipherProvider {
        override suspend fun isVaultLocked(): Boolean = true

        override suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card> {
            val card = this@toAutofillCipherProvider.card ?: return emptyList()
            return listOf(
                AutofillCipher.Card(
                    name = name,
                    subtitle = subtitle.orEmpty(),
                    cardholderName = card.cardholderName.orEmpty(),
                    code = card.code.orEmpty(),
                    expirationMonth = card.expMonth.orEmpty(),
                    expirationYear = card.expYear.orEmpty(),
                    number = card.number.orEmpty(),
                ),
            )
        }

        override suspend fun getLoginAutofillCiphers(
            uri: String,
        ): List<AutofillCipher.Login> {
            val login = this@toAutofillCipherProvider.login ?: return emptyList()
            return listOf(
                AutofillCipher.Login(
                    name = name,
                    password = login.password.orEmpty(),
                    subtitle = subtitle.orEmpty(),
                    username = login.username.orEmpty(),
                ),
            )
        }
    }
