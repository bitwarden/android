package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

/**
 * Primary implementation of [DatabaseSchemeManager].
 */
class DatabaseSchemeManagerImpl(
    val settingsDiskSource: SettingsDiskSource,
    val dispatcherManager: DispatcherManager,
) : DatabaseSchemeManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var lastDatabaseSchemeChangeInstant: Instant?
        get() = settingsDiskSource.lastDatabaseSchemeChangeInstant
        set(value) {
            settingsDiskSource.lastDatabaseSchemeChangeInstant = value
        }

    override val lastDatabaseSchemeChangeInstantFlow =
        settingsDiskSource
            .lastDatabaseSchemeChangeInstantFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.lastDatabaseSchemeChangeInstant,
            )
}
