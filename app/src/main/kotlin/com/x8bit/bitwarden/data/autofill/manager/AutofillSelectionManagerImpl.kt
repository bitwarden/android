package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.vault.CipherView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Primary implementation of [AutofillSelectionManager].
 */
class AutofillSelectionManagerImpl : AutofillSelectionManager {
    private val autofillSelectionChannel = Channel<CipherView>(capacity = Int.MAX_VALUE)

    override val autofillSelectionFlow: Flow<CipherView> =
        autofillSelectionChannel.receiveAsFlow()

    override fun emitAutofillSelection(cipherView: CipherView) {
        autofillSelectionChannel.trySend(cipherView)
    }
}
