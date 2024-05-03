package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

/**
 * Provides the ability to migrate from a legacy AppCenter system to this app.
 */
interface LegacyAppCenterMigrator {
    /**
     * Migrates any data from the legacy AppCenter system to the new app.
     * After migration, data will be removed from the legacy system.
     */
    fun migrateIfNecessary()
}
