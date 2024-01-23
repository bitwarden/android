package com.x8bit.bitwarden.data.auth.datasource.disk.legacy

import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorage
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigratorImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacySecureStorageMigratorTest {

    private val fakeLegacySecureStorage = FakeLegacySecureStorage()
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val legacySecureStorageMigrator = LegacySecureStorageMigratorImpl(
        legacySecureStorage = fakeLegacySecureStorage,
        encryptedSharedPreferences = fakeSharedPreferences,
    )

    @Test
    fun `migrateIfNecessary when there are no keys left should do nothing`() {
        assertTrue(fakeSharedPreferences.all.keys.isEmpty())

        legacySecureStorageMigrator.migrateIfNecessary()

        assertTrue(fakeSharedPreferences.all.keys.isEmpty())
    }

    @Test
    fun `migrateIfNecessary when the keys do not start with bwSecureStorage should do nothing`() {
        assertTrue(fakeSharedPreferences.all.keys.isEmpty())

        fakeLegacySecureStorage.put(
            key = "hashedKey",
            value = "value",
        )
        legacySecureStorageMigrator.migrateIfNecessary()

        assertTrue(fakeSharedPreferences.all.keys.isEmpty())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `migrateIfNecessary when the keys start with bwSecureStorage should migrate the data and remove it from the legacy storage`() {
        assertTrue(fakeSharedPreferences.all.keys.isEmpty())

        val userId = "userId"
        val key = "bwSecureStorage:userKeyAutoUnlock_$userId"
        val value = "mockUserAutoUnlockKey"
        fakeLegacySecureStorage.put(
            key = key,
            value = value,
        )
        legacySecureStorageMigrator.migrateIfNecessary()

        assertFalse(fakeSharedPreferences.all.keys.isEmpty())
        assertEquals(
            value,
            fakeSharedPreferences.getString(
                key = key,
                defaultValue = null,
            ),
        )
        assertNull(
            fakeLegacySecureStorage.get(
                key = key,
            ),
        )
    }
}

private class FakeLegacySecureStorage : LegacySecureStorage {
    private val dataMap = mutableMapOf<String, String>()

    override fun get(key: String): String? =
        dataMap[key]

    override fun getRawKeys(): Set<String> =
        dataMap.keys

    override fun remove(key: String) {
        dataMap.remove(key)
    }

    override fun removeAll() {
        dataMap.clear()
    }

    fun put(key: String, value: String) {
        dataMap[key] = value
    }
}
