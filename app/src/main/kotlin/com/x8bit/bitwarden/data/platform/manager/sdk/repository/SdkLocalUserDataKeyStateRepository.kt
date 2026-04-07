package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.core.LocalUserDataKeyState
import com.bitwarden.sdk.LocalUserDataKeyStateRepository
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource

/**
 * An implementation of a Bitwarden SDK [LocalUserDataKeyStateRepository].
 */
class SdkLocalUserDataKeyStateRepository(
    private val authDiskSource: AuthDiskSource,
) : LocalUserDataKeyStateRepository {
    override suspend fun get(id: String): LocalUserDataKeyState? {
        return authDiskSource
            .getLocalUserDataKey(userId = id)
            ?.let { LocalUserDataKeyState(wrappedKey = it) }
    }

    override suspend fun has(
        id: String,
    ): Boolean = authDiskSource.getLocalUserDataKey(userId = id) != null

    override suspend fun list(): List<LocalUserDataKeyState> =
        authDiskSource
            .userState
            ?.accounts
            ?.mapNotNull { get(id = it.key) }
            .orEmpty()

    override suspend fun remove(id: String) {
        authDiskSource.storeLocalUserDataKey(userId = id, wrappedKey = null)
    }

    override suspend fun removeAll() {
        removeBulk(keys = authDiskSource.userState?.accounts.orEmpty().keys.toList())
    }

    override suspend fun removeBulk(keys: List<String>) {
        keys.forEach { remove(id = it) }
    }

    override suspend fun set(id: String, value: LocalUserDataKeyState) {
        authDiskSource.storeLocalUserDataKey(userId = id, value.wrappedKey)
    }

    override suspend fun setBulk(values: Map<String, LocalUserDataKeyState>) {
        values.forEach { (id, value) -> set(id = id, value = value) }
    }
}
