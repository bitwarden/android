package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a send in the database.
 */
@Entity(tableName = "sends")
data class SendEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: String,

    @ColumnInfo(name = "send_type")
    val sendType: String,

    @ColumnInfo(name = "send_json")
    val sendJson: String,
)
