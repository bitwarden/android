package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a cipher in the database.
 */
@Entity(
    tableName = "ciphers",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["user_id", "organization_id"]),
    ],
)
data class CipherEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    // Default to true for initial migration.
    // Subsequent syncs will populate with correct values for optimizations.
    @ColumnInfo(name = "has_totp", defaultValue = "1")
    val hasTotp: Boolean,

    @ColumnInfo(name = "cipher_type")
    val cipherType: String,

    @ColumnInfo(name = "cipher_json")
    val cipherJson: String,

    // Extracted organizationId for query optimization to avoid loading full cipher JSON.
    // Enables lightweight queries for vault migration checks and organization filtering.
    @ColumnInfo(name = "organization_id")
    val organizationId: String?,
)
