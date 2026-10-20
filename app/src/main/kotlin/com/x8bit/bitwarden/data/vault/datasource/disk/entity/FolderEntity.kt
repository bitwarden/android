package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.Instant

/**
 * Entity representing a folder in the database.
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "revision_date")
    val revisionDate: Instant,
)
