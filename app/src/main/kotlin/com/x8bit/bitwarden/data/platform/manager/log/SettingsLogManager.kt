package com.x8bit.bitwarden.data.platform.manager.log

/**
 * Abstracts the collecting of information for sharing.
 */
interface SettingsLogManager {
    /**
     * A formatted string to be shared for debugging purposes.
     */
    val data: String
}
