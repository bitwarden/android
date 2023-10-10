package com.x8bit.bitwarden.data.auth.datasource.disk

/**
 * Primary access point for disk information.
 */
interface AuthDiskSource {
    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?
}
