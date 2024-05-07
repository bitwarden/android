package com.bitwarden.authenticator.ui.platform.feature.settings.importing.model

/**
 * Represents the file formats a user can select to import their vault.
 */
enum class ImportFormat(
    val mimeType: String,
) {
    JSON("application/json"),
}
