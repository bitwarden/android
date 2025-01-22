package com.x8bit.bitwarden.ui.platform.manager.keychain.model

/**
 * Represents the result of an operation to select a private key alias from the system KeyChain.
 */
sealed class PrivateKeyAliasSelectionResult {
    /**
     * Indicates that the operation was successful and an alias was selected (or null if none was
     * selected).
     */
    data class Success(val alias: String?) : PrivateKeyAliasSelectionResult()

    /**
     * Indicates that an error occurred during the operation.
     */
    object Error : PrivateKeyAliasSelectionResult()
}
