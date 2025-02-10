package com.x8bit.bitwarden.ui.platform.manager.keychain

import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult

/**
 * Responsible for managing keys stored in the system KeyChain.
 */
interface KeyChainManager {

    /**
     * Display the system private key alias selection dialog.
     *
     * @param currentServerUrl The currently selected server URL.
     */
    suspend fun choosePrivateKeyAlias(
        currentServerUrl: String?,
    ): PrivateKeyAliasSelectionResult
}
