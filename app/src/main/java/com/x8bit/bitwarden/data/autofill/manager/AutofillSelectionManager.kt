package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.vault.CipherView
import kotlinx.coroutines.flow.Flow

/**
 * Tracks the selection of a [CipherView] during the autofill flow within the app.
 */
interface AutofillSelectionManager {

    /**
     * Emits a [CipherView] as a result of calls to [emitAutofillSelection].
     */
    val autofillSelectionFlow: Flow<CipherView>

    /**
     * Triggers an emission via [autofillSelectionFlow].
     */
    fun emitAutofillSelection(cipherView: CipherView)
}
