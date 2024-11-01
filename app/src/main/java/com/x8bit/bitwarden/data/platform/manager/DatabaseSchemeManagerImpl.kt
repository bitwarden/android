package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import java.time.Instant

/**
 * Primary implementation of [DatabaseSchemeManager].
 */
class DatabaseSchemeManagerImpl(
    val settingsDiskSource: SettingsDiskSource,
) : DatabaseSchemeManager {
    override var lastDatabaseSchemeChangeInstant: Instant?
        get() = settingsDiskSource.lastDatabaseSchemeChangeInstant
        set(value) {
            settingsDiskSource.lastDatabaseSchemeChangeInstant = value
        }
}
