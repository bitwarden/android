package com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bitwarden.vault.PasswordHistory
import java.time.Instant

/**
 * Entity representing a generated history item in the database.
 */
@Entity(tableName = "password_history")
data class PasswordHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "userId")
    val userId: String,

    @ColumnInfo(name = "encrypted_password")
    val encryptedPassword: String,

    @ColumnInfo(name = "generated_date_time_ms")
    val generatedDateTimeMs: Long,
)

/**
 * Converts a PasswordHistory object to a GeneratedHistoryItem.
 * This function is used to transform data from the SDK model to the database entity model.
 */
fun PasswordHistory.toPasswordHistoryEntity(userId: String): PasswordHistoryEntity {
    return PasswordHistoryEntity(
        userId = userId,
        encryptedPassword = this.password,
        generatedDateTimeMs = this.lastUsedDate.toEpochMilli(),
    )
}

/**
 * Converts a GeneratedHistoryItem object to a PasswordHistory.
 * This function is used to transform data from the database entity model to the SDK model.
 */
fun PasswordHistoryEntity.toPasswordHistory(): PasswordHistory {
    return PasswordHistory(
        password = this.encryptedPassword,
        lastUsedDate = Instant.ofEpochMilli(this.generatedDateTimeMs),
    )
}
