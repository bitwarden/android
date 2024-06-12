package com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity

import com.bitwarden.vault.PasswordHistory
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant

class PasswordHistoryEntityTest {

    @Test
    fun `toPasswordHistoryEntity should return the correct value`() {
        val passwordHistory = PasswordHistory(
            password = "testPassword",
            lastUsedDate = Instant.parse("2021-01-01T00:00:00Z"),
        )
        val expectedEntity = PasswordHistoryEntity(
            id = 0,
            userId = "testId",
            encryptedPassword = "testPassword",
            generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
        )
        val entity = passwordHistory.toPasswordHistoryEntity("testId")

        assertEquals(expectedEntity, entity)
    }

    @Test
    fun `toPasswordHistory should return the correct value`() {
        val entity = PasswordHistoryEntity(
            id = 1,
            userId = "testId",
            encryptedPassword = "testPassword",
            generatedDateTimeMs = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli(),
        )
        val passwordHistory = entity.toPasswordHistory()
        val expectedPasswordHistory = PasswordHistory(
            password = "testPassword",
            lastUsedDate = Instant.parse("2021-01-01T00:00:00Z"),
        )

        assertEquals(expectedPasswordHistory, passwordHistory)
    }
}
