package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a collection in the database.
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: String,

    @ColumnInfo(name = "organization_id")
    val organizationId: String,

    @ColumnInfo(name = "should_hide_passwords")
    val shouldHidePasswords: Boolean,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "external_id")
    val externalId: String?,

    @ColumnInfo(name = "read_only")
    val isReadOnly: Boolean,

    @ColumnInfo(name = "manage")
    val canManage: Boolean?,
)
