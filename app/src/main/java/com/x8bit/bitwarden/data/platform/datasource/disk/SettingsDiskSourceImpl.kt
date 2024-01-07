package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

private const val VAULT_TIMEOUT_ACTION_KEY = "$BASE_KEY:vaultTimeoutAction"
private const val VAULT_TIME_IN_MINUTES_KEY = "$BASE_KEY:vaultTimeout"

/**
 * Primary implementation of [SettingsDiskSource].
 */
class SettingsDiskSourceImpl(
    val sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    SettingsDiskSource {
    private val mutableVaultTimeoutActionFlowMap =
        mutableMapOf<String, MutableSharedFlow<VaultTimeoutAction?>>()

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

    override fun getVaultTimeoutAction(userId: String): VaultTimeoutAction? =
        getInt(key = "${VAULT_TIMEOUT_ACTION_KEY}_$userId")?.let { storedValue ->
            VaultTimeoutAction.entries.firstOrNull { storedValue == it.value }
        }

    override fun getVaultTimeoutActionFlow(userId: String): Flow<VaultTimeoutAction?> =
        getMutableVaultTimeoutActionFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutAction(userId = userId)) }

    override fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    ) {
        putInt(
            key = "${VAULT_TIMEOUT_ACTION_KEY}_$userId",
            value = vaultTimeoutAction?.value,
        )
        getMutableVaultTimeoutActionFlow(userId = userId).tryEmit(vaultTimeoutAction)
    }

    private fun getMutableVaultTimeoutActionFlow(
        userId: String,
    ): MutableSharedFlow<VaultTimeoutAction?> =
        mutableVaultTimeoutActionFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableVaultTimeoutInMinutesFlow(
        userId: String,
    ): MutableSharedFlow<Int?> =
        mutableVaultTimeoutInMinutesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }
}
