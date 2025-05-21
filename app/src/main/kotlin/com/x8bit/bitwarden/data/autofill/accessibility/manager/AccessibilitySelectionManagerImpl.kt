package com.x8bit.bitwarden.data.autofill.accessibility.manager

import com.bitwarden.vault.CipherView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The default implementation of the [AccessibilitySelectionManager].
 */
class AccessibilitySelectionManagerImpl : AccessibilitySelectionManager {
    private val accessibilitySelectionChannel: Channel<CipherView> = Channel(
        capacity = Int.MAX_VALUE,
    )

    override val accessibilitySelectionFlow: Flow<CipherView> =
        accessibilitySelectionChannel.receiveAsFlow()

    override fun emitAccessibilitySelection(cipherView: CipherView) {
        accessibilitySelectionChannel.trySend(cipherView)
    }
}
