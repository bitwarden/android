package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Primary implementation of [DatabaseSchemeManager].
 */
class DatabaseSchemeManagerImpl(
    val authDiskSource: AuthDiskSource,
    val settingsDiskSource: SettingsDiskSource,
) : DatabaseSchemeManager {
    private val mutableSharedFlow: MutableSharedFlow<Unit> = bufferedMutableSharedFlow()

    override fun clearSyncState() {
        authDiskSource.userState?.accounts?.forEach { (userId, _) ->
            settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
        }
        mutableSharedFlow.tryEmit(Unit)
    }

    override val databaseSchemeChangeFlow: Flow<Unit> = mutableSharedFlow.asSharedFlow()
}
