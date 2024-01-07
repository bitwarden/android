package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

private const val VAULT_TIME_IN_MINUTES_KEY = "$BASE_KEY:vaultTimeout"

/**
 * Primary implementation of [SettingsDiskSource].
 */
class SettingsDiskSourceImpl(
    val sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    SettingsDiskSource {
    private val mutableVaultTimeoutInMinutesFlowMap =
        mutableMapOf<String, MutableSharedFlow<Int?>>()

    override fun getVaultTimeoutInMinutes(userId: String): Int? =
        getInt(key = "${VAULT_TIME_IN_MINUTES_KEY}_$userId")

    override fun getVaultTimeoutInMinutesFlow(userId: String): Flow<Int?> =
        getMutableVaultTimeoutInMinutesFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutInMinutes(userId = userId)) }

    override fun storeVaultTimeoutInMinutes(
        userId: String,
        vaultTimeoutInMinutes: Int?,
    ) {
        putInt(
            key = "${VAULT_TIME_IN_MINUTES_KEY}_$userId",
            value = vaultTimeoutInMinutes,
        )
        getMutableVaultTimeoutInMinutesFlow(userId = userId).tryEmit(vaultTimeoutInMinutes)
    }

    private fun getMutableVaultTimeoutInMinutesFlow(
        userId: String,
    ): MutableSharedFlow<Int?> =
        mutableVaultTimeoutInMinutesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }
}
