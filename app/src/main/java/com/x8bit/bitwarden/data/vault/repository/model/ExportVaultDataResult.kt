package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of the vault data being exported.
 */
sealed class ExportVaultDataResult {

    /**
     * The vault data has been successfully converted into the selected format
     * (JSON, CSV, encrypted JSON).
     */
    data class Success(val vaultData: String) : ExportVaultDataResult()

    /**
     * There was an error converting the vault data.
     */
    data object Error : ExportVaultDataResult()
}
