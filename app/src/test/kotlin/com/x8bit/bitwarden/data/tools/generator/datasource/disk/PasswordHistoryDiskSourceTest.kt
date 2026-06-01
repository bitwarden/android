package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.datasource.disk.dao.FakePasswordHistoryDao
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordHistoryDiskSourceTest {

    private val fakePasswordHistoryDao = FakePasswordHistoryDao()
    private val diskSource = PasswordHistoryDiskSourceImpl(fakePasswordHistoryDao)
    private val testUserId = "testUserId"

    @Test
    fun `insertPassword calls dao insertPasswordHistory`() = runTest {
        val passwordHistoryEntity = PasswordHistoryEntity(
            id = 0,
            userId = testUserId,
            encryptedPassword = "encrypted",
            generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
        )

        diskSource.insertPasswordHistory(passwordHistoryEntity)

        assertTrue(fakePasswordHistoryDao.storedPasswordHistories.contains(passwordHistoryEntity))
    }

    @Test
    fun `getPasswordHistoriesForUser returns flow from dao`() = runTest {
        val passwordHistoryEntity = PasswordHistoryEntity(
            id = 0,
            userId = testUserId,
            encryptedPassword = "encrypted",
            generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
        )
        fakePasswordHistoryDao.insertPasswordHistory(passwordHistoryEntity)

        val result = diskSource
            .getPasswordHistoriesForUser(testUserId)
            .first()

        assertEquals(listOf(passwordHistoryEntity), result)
    }

    @Test
    fun `clearPasswordHistoriesForUser calls dao clearPasswordHistoriesForUser`() = runTest {
        fakePasswordHistoryDao.storedPasswordHistories.add(
            PasswordHistoryEntity(
                id = 1,
                userId = testUserId,
                encryptedPassword = "encrypted",
                generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
            ),
        )

        diskSource.clearPasswordHistories(testUserId)

        assertTrue(fakePasswordHistoryDao.storedPasswordHistories.none { it.userId == testUserId })
    }
}
