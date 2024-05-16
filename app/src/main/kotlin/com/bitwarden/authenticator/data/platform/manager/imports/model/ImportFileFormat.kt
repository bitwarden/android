package com.bitwarden.authenticator.data.platform.manager.imports.model

/**
 * Represents the file formats a user can select to import their vault.
 */
enum class ImportFileFormat(
    val mimeType: String,
) {
    BITWARDEN_JSON("application/json"),
    TWO_FAS_JSON("*/*"),
}
