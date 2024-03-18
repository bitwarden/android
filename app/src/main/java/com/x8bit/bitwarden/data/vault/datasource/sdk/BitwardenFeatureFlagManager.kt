package com.x8bit.bitwarden.data.vault.datasource.sdk

/**
 * Manages the available feature flags for the Bitwarden application.
 */
interface BitwardenFeatureFlagManager {
    /**
     * Returns a map of feature flags.
     */
    val featureFlags: Map<String, Boolean>
}
