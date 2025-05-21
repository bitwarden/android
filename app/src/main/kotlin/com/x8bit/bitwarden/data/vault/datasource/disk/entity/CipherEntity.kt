package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a cipher in the database.
 */
@Entity(tableName = "ciphers")
data class CipherEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: String,

    @ColumnInfo(name = "cipher_type")
    val cipherType: String,

    @ColumnInfo(name = "cipher_json")
    val cipherJson: String,
)
