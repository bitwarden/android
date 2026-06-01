package com.x8bit.bitwarden.data.autofill.accessibility.manager

import com.bitwarden.vault.CipherView
import kotlinx.coroutines.flow.Flow

/**
 * A manager class used to handle the accessibility autofill selections.
 */
interface AccessibilitySelectionManager {
    /**
     * Emits a [CipherView] as a result of calls to [emitAccessibilitySelection].
     */
    val accessibilitySelectionFlow: Flow<CipherView>

    /**
     * Triggers an emission via [accessibilitySelectionFlow].
     */
    fun emitAccessibilitySelection(cipherView: CipherView)
}
